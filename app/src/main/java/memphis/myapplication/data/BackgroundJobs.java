package memphis.myapplication.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.pib.PibKey;
import net.named_data.jndn.security.tpm.Tpm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.PSync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import memphis.myapplication.Globals;
import memphis.myapplication.R;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.psync.ConsumerManager;
import memphis.myapplication.psync.ProducerManager;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.utilities.MemoryCache;
import memphis.myapplication.utilities.SharedPrefsManager;
import timber.log.Timber;

import static java.lang.Thread.sleep;


public class BackgroundJobs {

    private static BackgroundJobs instance;
    private final Context applicationContext;
    public AndroidSqlite3Pib m_pib;
    public TpmBackEndFile m_tpm;

    // not sure if applicationContext globals instance is necessary here but applicationContext should ensure we have at least
    // one instance so the security vars exist
    public KeyChain keyChain;
    public Face face;
    public MemoryCache memoryCache;
    private int networkDiscoveryTries = 0;
    private int maxnetworkDiscoveryTries = 10;

    private boolean netThreadShouldStop = true;

    private PSync psync;
    private ProducerManager producerManager;
    private ConsumerManager consumerManager;

    SharedPrefsManager sharedPrefsManager;
    private MutableLiveData<String> toastData;
    private MutableLiveData<Interest> friendRequest;

    public BackgroundJobs(Context applicationContext) {
        this.applicationContext = applicationContext;
        toastData = new MutableLiveData<>();
        friendRequest = new MutableLiveData<>();
        initialise(applicationContext);
    }

    private void initialise(Context applicationContext) {
        psync = PSync.getInstance(applicationContext.getFilesDir().getAbsolutePath());
        Globals.setPSync(psync);

        sharedPrefsManager = SharedPrefsManager.getInstance(applicationContext);

        boolean faceExists = (Globals.face == null);
        Timber.d("Globals face is null?: " + faceExists +
                "; Globals security is setup: " + Globals.has_setup_security);
        // need to check if we have an existing face or if security is not setup; either way, we
        // need to make changes; see setup_security()
        if (faceExists || !Globals.has_setup_security) {
            setup_security(applicationContext);
        }

        face = Globals.face;
        memoryCache = Globals.memoryCache;
        keyChain = Globals.keyChain;

        if (!appThreadIsRunning()) {
            Timber.d("Starting network thread");
            startNetworkThread();
        }

    }

    public static synchronized BackgroundJobs getInstance(Context context) {
        if (instance == null) {
            instance = new BackgroundJobs(context);

        }
        return instance;
    }

    /**
     * This function sets up identity storage, keys, and the face our app will use.
     *
     * @param applicationContext
     */
    public void setup_security(Context applicationContext) {
        Timber.d("Setting up security");
        FileManager manager = new FileManager(applicationContext);
        // /npChat/<username>
        Name appPrefix = new Name(sharedPrefsManager.getDomain() + "/" + applicationContext.getString(R.string.app_name) + "/" + sharedPrefsManager.getUsername());

        // Creating producer
        Timber.d("Creating producer %s", appPrefix.toUri());
        String producerPrefix = appPrefix.toUri();
        producerManager = new ProducerManager(producerPrefix);
        Globals.setProducerManager(producerManager);


        // Creating consumers
        Timber.d("Creating consumer");
        consumerManager = new ConsumerManager(applicationContext, toastData);
        Globals.setConsumerManager(consumerManager);

        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        ArrayList<User> friends = realmRepository.getAllFriends();
        realmRepository.close();

        for (User friend : friends) {
            String friendNamespace = friend.getNamespace();
            consumerManager.createConsumer(friendNamespace);
            Timber.d("Added consumer for friend for %s", friendNamespace);
        }


        String rootPath = applicationContext.getFilesDir().toString();
        String pibPath = "pib-sqlite3:" + rootPath;

        face = new Face();
        try {
            m_pib = new AndroidSqlite3Pib(rootPath, "/pib.db");
            Globals.setPib(m_pib);
        } catch (PibImpl.Error e) {
            e.printStackTrace();
        }

        m_tpm = new TpmBackEndFile(TpmBackEndFile.getDefaultDirecoryPath(applicationContext.getFilesDir()));
        Globals.setTpmBackEndFile(m_tpm);
        try {
            m_pib.setTpmLocator("tpm-file:" + TpmBackEndFile.getDefaultDirecoryPath(applicationContext.getFilesDir()));
        } catch (PibImpl.Error e) {
            e.printStackTrace();
        }

        try {
            keyChain = new KeyChain(m_pib, m_tpm);
        } catch (PibImpl.Error e) {
            e.printStackTrace();
        }

        Name identity = new Name(appPrefix);
        Name defaultCertificateName;
        PibIdentity pibId = null;
        PibKey key;

        try {
            // see if the identity exists; if it doesn't, this will throw an error
            pibId = keyChain.getPib().getIdentity(identity);
            key = pibId.getDefaultKey();
            keyChain.setDefaultIdentity(pibId);
            keyChain.setDefaultKey(pibId, key);
            keyChain.getPib().setDefaultIdentity_(identity);
            Globals.setPubKeyName(key.getName());
            Globals.setPublicKey(key.getPublicKey());
            Globals.setDefaultPibId(pibId);

        } catch (PibImpl.Error | Pib.Error e) {
            try {
                pibId = keyChain.createIdentityV2(identity);
                key = pibId.getDefaultKey();
                keyChain.setDefaultIdentity(pibId);
                keyChain.setDefaultKey(pibId, key);
                keyChain.getPib().setDefaultIdentity_(identity);
                Globals.setPubKeyName(key.getName());
                Globals.setPublicKey(key.getPublicKey());
                Globals.setDefaultPibId(pibId);
            } catch (PibImpl.Error | Pib.Error | TpmBackEnd.Error | Tpm.Error | KeyChain.Error ex) {
                ex.printStackTrace();
            }
        }

        Globals.setDefaultIdName(appPrefix);

        try {
            defaultCertificateName = keyChain.getDefaultCertificateName();
        } catch (SecurityException e) {
            e.printStackTrace();
            defaultCertificateName = new Name("/bogus/certificate/name");
        }
        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setMemoryCache(new MemoryCache(face, applicationContext));
        Globals.setHasSecurity(true);

        try {
            Timber.d(keyChain.getPib().getDefaultIdentity().getDefaultKey().getDefaultCertificate().toString());
        } catch (Pib.Error error) {
            error.printStackTrace();
        } catch (PibImpl.Error error) {
            error.printStackTrace();
        }

        Timber.d("Security was setup successfully");
        register_with_NFD(appPrefix);

        // Share friends list
        producerManager.updateFriendsList();

//        Realm tempRealm = Realm.getDefaultInstance();
//        tempRealm.beginTransaction();
//        RealmResults<UserRealm> users = tempRealm.where(UserRealm.class).equalTo("username", "mw").or().equalTo("username","mb").findAll();
//        users.deleteAllFromRealm();
//        tempRealm.commitTransaction();
//        tempRealm.close();
    }

    // Eventually, we should move this to a Service, but for now, this thread consistently calls
    // face.processEvents() to check for any changes, such as publishing or fetching.
    private final Thread networkThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean faceExists = Globals.face == null;
            Timber.d("Globals face is null?: " + faceExists + "; Globals security is setup: " + Globals.has_setup_security);
            if (!Globals.has_setup_security) {
                setup_security(applicationContext);
            }
            try {
                sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!netThreadShouldStop) {
                try {
                    face.processEvents();
                    sleep(10);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // face.shutdown();
        }
    });

    private void startNetworkThread() {
        if (!networkThread.isAlive()) {
            netThreadShouldStop = false;
            networkThread.start();
        }
    }

    private void stopNetworkThread() {
        netThreadShouldStop = true;
    }

    protected boolean appThreadIsRunning() {
        return networkThread.isAlive();
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
//    public Runnable makeToast(final String s) {
//        return new Runnable() {
//            public void run() {
//                Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show();
//            }
//        };
//    }

    /**
     * Registers the provided name with NFD. This is intended to occur whenever the app starts up.
     *
     * @param name The provided name should be /npChat/<username>
     * @throws IOException
     * @throws PibImpl.Error
     */
    public void register_with_NFD(Name name) {

        if (!Globals.has_setup_security) {
            setup_security(applicationContext);
            while (!Globals.has_setup_security)
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        try {
            Name dataName = new Name(name);
            Name fileName = new Name(name);
            Name certName = new Name(name);
            Name friendsListName = new Name(name);
            Name friendRequestName = new Name(name);
            final Name networkDiscoveryName = new Name("network-discovery");
            dataName.append("data");
            fileName.append("file");
            certName.append("cert");
            friendsListName.append("friends");
            friendRequestName.append("friend-request");
            networkDiscoveryName.append("discover");

            Timber.d("Starting registration process.");

            Globals.face.registerPrefix(dataName, Globals.producerManager.onDataInterest,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d("Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            toastData.postValue(msg);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                            Timber.i(toastData + "");
                            toastData.postValue(msg);
                        }
                    }
            );

            Globals.memoryCache.getmCache().registerPrefix(fileName,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d("Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            toastData.postValue(msg);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                            Timber.i(toastData + "");
                            toastData.setValue(msg);
                        }
                    }, Globals.memoryCache.onNoDataInterest
            );

            Globals.face.registerPrefix(certName, FileManager.onCertInterest,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d("Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            toastData.postValue(msg);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                        }
                    }
            );

            Globals.face.registerPrefix(friendsListName, Globals.producerManager.onFriendsListInterest,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d("Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            toastData.postValue(msg);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                        }
                    }
            );

            Globals.face.registerPrefix(friendRequestName, new OnInterestCallback() {
                        @Override
                        public void onInterest(Name prefix, final Interest interest, final Face face, long interestFilterId, InterestFilter filter) {
                            Timber.d("Got interest " + interest.toUri());
                            friendRequest.setValue(interest);

                        }
                    },
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {

                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                        }
                    }
            );

            Globals.face.registerPrefix(networkDiscoveryName, new OnInterestCallback() {
                        @Override
                        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                            // Listen for incoming messages from other devices over multicast face
                            // and save any new users so we can register routes to them later
                            // Need to determine when to switch to NSD.
                            Timber.d("Discovered other nodes via multicast face");
                            Data data = new Data();
                            data.setName(interest.getName());
                            data.setContent(new Blob(new Blob(sharedPrefsManager.getNamespace())));
                            try {
                                face.putData(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Globals.setUseMulticast(true);
                        }
                    },
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {

                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                        }
                    });
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }

        // Register outgoing routes
        registerRoutesToMulticastFace();
        registerWithNSD();
        expressNetworkDiscoveryInterest();


    }

    public void expressNetworkDiscoveryInterest() {
        final OnData onData = new OnData() {
            @Override
            public void onData(Interest interest, Data data) {
                Timber.d("Got data");
                Name interestData = new Name(data.getContent().toString());
                Globals.setUseMulticast(true);

                String username = null;
                String userDomain = null;
                for (int i = 0; i <= interestData.size(); i++) {
                    if (interestData.getSubName(i, 1).toUri().equals("/npChat")) {
                        username = interestData.getSubName(i + 1, 1).toUri().substring(1);
                        userDomain = interestData.getPrefix(i).toUri();
                    }
                }
                Timber.d(username + " and " + userDomain);

                RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                realmRepository.saveNewFriend(username, userDomain, null);
                realmRepository.close();
                registerFriends();
            }
        };

        final OnTimeout onTimeout = new OnTimeout() {
            @Override
            public void onTimeout(Interest interest) {
                Timber.d("expressing interest after timeout");
                networkDiscoveryTries++;
                if (networkDiscoveryTries <= maxnetworkDiscoveryTries) {
                    expressNetworkDiscoveryInterest();
                } else if (networkDiscoveryTries == maxnetworkDiscoveryTries + 1) {
                    Timber.d("Now use NSD");
                    Globals.setUseMulticast(false);
                    Globals.nsdHelper.registerFriends();
                }


            }
        };
        try {
            Timber.d("expressing initial interest");
            Interest interest = new Interest(new Name("/network-discovery/discover/" + sharedPrefsManager.getNamespace()));
            face.expressInterest(interest, onData, onTimeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Checks NFD for a multicast face and registers network discovery prefix
     */
    public void registerRoutesToMulticastFace() {
        int myFace = 0;
        try {
            final List<FaceStatus> faceList = Nfdc.getFaceList(face);
            final List<RibEntry> routeInfo = Nfdc.getRouteList(face);
            boolean wifiDirect = false;
            for (RibEntry r : routeInfo) {
                String name = r.getName().toString();
                if (name.contains("wifidirect")) {
                    Timber.d("Connected via WifiDirect");
                    wifiDirect = true;
                    break;
                }
            }
            if (!wifiDirect) {
                for (FaceStatus f : faceList) {
                    if (f.getRemoteUri().contains("udp4://224")) {
                        Timber.d("Using multicast face: " + f.getRemoteUri());
                        myFace = f.getFaceId();
                        Globals.setMulticastFaceID(myFace);

                        if (myFace != 0) {
                            try {
                                // Register network discovery prefix
                                Nfdc.register(face, myFace, new Name("network-discovery"), 0);

                            } catch (ManagementException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerFriends() {

        int myFace = Globals.multicastFaceID;
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        ArrayList<User> friends = realmRepository.getAllFriends();
        realmRepository.close();

        for (User u : friends) {
            try {
                Nfdc.register(face, myFace, new Name(u.getNamespace()), 0);
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerUser(User friend) {
        try {
            Nfdc.register(face, Globals.multicastFaceID, new Name(friend.getNamespace()), 0);
        } catch (ManagementException e) {
            e.printStackTrace();
        }
    }

    public void registerWithNSD() {
        Timber.d("Discovering nodes");
        NSDHelper nsdHelper;
        nsdHelper = new NSDHelper(sharedPrefsManager.getNamespace(), applicationContext.getApplicationContext(), face);
        Globals.setNSDHelper(nsdHelper);
        nsdHelper.discoverServices();
    }


    public LiveData<String> toast() {
        return toastData;
    }

    public LiveData<Interest> getFriendRequest() {
        return friendRequest;
    }

}
