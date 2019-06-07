package memphis.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import timber.log.Timber;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

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
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encrypt.algo.EncryptAlgorithmType;
import net.named_data.jndn.encrypt.algo.EncryptParams;
import net.named_data.jndn.encrypt.algo.RsaAlgorithm;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.VerificationHelpers;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.pib.PibKey;
import net.named_data.jndn.security.tpm.Tpm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import net.named_data.jni.psync.PSync;


import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static java.lang.Thread.sleep;

import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.PublishedContent;
import memphis.myapplication.RealmObjects.SelfCertificate;
import memphis.myapplication.RealmObjects.User;
import memphis.myapplication.psync.ConsumerManager;
import memphis.myapplication.psync.ProducerManager;

public class MainActivity extends AppCompatActivity {

    public AndroidSqlite3Pib m_pib;
    public TpmBackEndFile m_tpm;

    // not sure if this globals instance is necessary here but this should ensure we have at least
    // one instance so the security vars exist
    Globals globals = (Globals) getApplication();
    public KeyChain keyChain;
    public Face face;
    public MemoryCache memoryCache;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final int CAMERA_REQUEST_CODE = 0;
    private final int SELECT_RECIPIENTS_CODE = 1;
    private final int ADD_FRIEND_CODE = 2;
    private final int SETTINGS_CODE = 3;
    private final int RESULT_ALREADY_TRUST = 5;
    private File m_curr_photo_file;
    private int networkDiscoveryTries = 0;
    private int maxnetworkDiscoveryTries = 10;

    private boolean netThreadShouldStop = true;

    private PSync psync;
    private ProducerManager producerManager;
    private ConsumerManager consumerManager;

    SharedPrefsManager sharedPrefsManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        psync = PSync.getInstance(getFilesDir().getAbsolutePath());
        Globals.setPSync(psync);
        Realm.init(this);


        sharedPrefsManager = SharedPrefsManager.getInstance(this);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        boolean faceExists = (Globals.face == null);
        Timber.d( "Globals face is null?: " + faceExists +
                "; Globals security is setup: " + Globals.has_setup_security);
        // need to check if we have an existing face or if security is not setup; either way, we
        // need to make changes; see setup_security()
        if (faceExists || !Globals.has_setup_security) {
            setup_security();
        }

        face = Globals.face;
        memoryCache = Globals.memoryCache;
        keyChain = Globals.keyChain;

        if (!appThreadIsRunning()) {
            Timber.d( "Starting network thread");
            startNetworkThread();
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        // check if user has given us permissions for storage manipulation (one time dialog box)
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        sharedPrefsManager = SharedPrefsManager.getInstance(this);
        psync = PSync.getInstance(getFilesDir().getAbsolutePath());

    }

    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, getString(R.string.app_name));
        Toolbar toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        Timber.d("menuInflated");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Timber.d("item: "+ item.toString());
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_CODE);
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function sets up identity storage, keys, and the face our app will use.
     */
    public void setup_security() {
        Timber.d("Setting up security");
        FileManager manager = new FileManager(getApplicationContext());
        // /npChat/<username>
        Name appPrefix = new Name(sharedPrefsManager.getDomain() + "/" + getString(R.string.app_name) + "/" + sharedPrefsManager.getUsername());

        // Creating producer
        Timber.d("Creating producer %s",  appPrefix.toUri());
        String producerPrefix = appPrefix.toUri();
        producerManager = new ProducerManager(producerPrefix);
        Globals.setProducerManager(producerManager);


        // Creating consumers
        Timber.d( "Creating consumer");
        consumerManager = new ConsumerManager(this, getApplicationContext());
        Globals.setConsumerManager(consumerManager);

        Realm realm = Realm.getDefaultInstance();

        RealmResults<User> friends = realm.where(User.class).equalTo("friend", true).findAll();
        for (User friend : friends) {
            String friendNamespace = friend.getNamespace();
            consumerManager.createConsumer(friendNamespace);
            Timber.d("Added consumer for friend for %s", friendNamespace);
        }


        Context context = getApplicationContext();
        String rootPath = getApplicationContext().getFilesDir().toString();
        String pibPath = "pib-sqlite3:" + rootPath;

        face = new Face();
        try {
            m_pib = new AndroidSqlite3Pib(rootPath, "/pib.db");
            Globals.setPib(m_pib);
        }
        catch(PibImpl.Error e) {
            e.printStackTrace();
        }

        m_tpm = new TpmBackEndFile(TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
        Globals.setTpmBackEndFile(m_tpm);
        try {
            m_pib.setTpmLocator("tpm-file:" + TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
        }
        catch(PibImpl.Error e) {
            e.printStackTrace();
        }

        try {
            keyChain = new KeyChain(pibPath, m_pib.getTpmLocator());
        }
        catch(SecurityException | IOException | PibImpl.Error | KeyChain.Error e) {
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

        }
        catch(PibImpl.Error | Pib.Error e) {
            try {
                pibId = keyChain.createIdentityV2(identity);
                key = pibId.getDefaultKey();
                keyChain.setDefaultIdentity(pibId);
                keyChain.setDefaultKey(pibId, key);
                keyChain.getPib().setDefaultIdentity_(identity);
                Globals.setPubKeyName(key.getName());
                Globals.setPublicKey(key.getPublicKey());
                Globals.setDefaultPibId(pibId);
            }
            catch(PibImpl.Error | Pib.Error | TpmBackEnd.Error | Tpm.Error | KeyChain.Error ex) {
                ex.printStackTrace();
            }
        }

        Globals.setDefaultIdName(appPrefix);

        try {
            defaultCertificateName = keyChain.getDefaultCertificateName();
        }
        catch(SecurityException e) {
            e.printStackTrace();
            defaultCertificateName = new Name("/bogus/certificate/name");
        }
        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setMemoryCache(new MemoryCache(face, getApplicationContext()));
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
//        RealmResults<User> users = tempRealm.where(User.class).equalTo("username", "mw").or().equalTo("username","mb").findAll();
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
                setup_security();
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
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }

    /**
     * Registers the provided name with NFD. This is intended to occur whenever the app starts up.
     * @param name The provided name should be /npChat/<username>
     * @throws IOException
     * @throws PibImpl.Error
     */
    public void register_with_NFD(Name name) {

        if (!Globals.has_setup_security) {
            setup_security();
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
                            runOnUiThread(makeToast(msg));
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
                        }
                    }
            );

            Globals.memoryCache.getmCache().registerPrefix(fileName,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d("Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Timber.d("Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
                        }
                    }, Globals.memoryCache.onNoDataInterest
            );

            Globals.face.registerPrefix(certName, FileManager.onCertInterest,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Timber.d( "Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
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
                            Timber.d( "Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
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

                            final FriendRequest friendRequest = new FriendRequest(interest, MainActivity.this);
                            friendRequest.receive();
                            friendRequest.addObserver(new Observer() {
                                @Override
                                public void update(Observable o, Object arg) {
                                    int updateCode = (int) arg;
                                    if (updateCode == 1) {
                                        runOnUiThread(new Thread(new Runnable() {
                                            public void run() {
                                                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                                alert.setTitle("Friend request");
                                                alert.setMessage("Accept friend request from " + friendRequest.getPendingFriend());
                                                alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // Accept incoming friend request
                                        try {
                                            friendRequest.accept();
                                        } catch (EncodingException e) {
                                            e.printStackTrace();
                                        }

                                                    }
                                                });

                                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // reject incoming friend request
                                                        friendRequest.reject();
                                                    }
                                                });

                                                alert.show();
                                            }
                                        }));
                                    } else if (updateCode == 2) {
                                        Timber.d("Could not be verified");
                                        runOnUiThread(makeToast("Received unverifiable friend request."));

                                    } else if (updateCode == 3) {
                                        Timber.d("Already friends");
                                        Data data = new Data();
                                        data.setContent(new Blob("Friends"));
                                        try {
                                            face.putData(data);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    } else if (updateCode == 4) {
                                        Timber.d( "Already trust");
                                        runOnUiThread(new Thread(new Runnable() {
                                            public void run() {
                                                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                                alert.setTitle("Friend request");
                                                alert.setMessage("Accept friend request from " + friendRequest.getPendingFriend());
                                                alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // Accept incoming friend request
                                                        friendRequest.acceptTrusted();

                                                    }
                                                });

                                                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int whichButton) {
                                                        // reject incoming friend request
                                                        friendRequest.reject();
                                                    }
                                                });

                                                alert.show();
                                            }
                                        }));
                                    }
                                }
                            });
                            }},
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
                Realm realm = Realm .getDefaultInstance();

                String username = null;
                String userDomain = null;
                for (int i = 0; i <= interestData.size(); i++) {
                    if (interestData.getSubName(i, 1).toUri().equals("/npChat")) {
                        username = interestData.getSubName(i+1, 1).toUri().substring(1);
                        userDomain = interestData.getPrefix(i).toUri();
                    }
                }
                Timber.d(username + " and " + userDomain);


                User user = realm.where(User.class).equalTo("username", username).findFirst();
                if (user == null) {
                    realm.beginTransaction();
                    user = realm.createObject(User.class, username);
                    user.setDomain(userDomain);
                    realm.commitTransaction();
                }
                realm.close();
                registerFriends();
            }
        };

        final OnTimeout onTimeout = new OnTimeout() {
            @Override
            public void onTimeout(Interest interest) {
                Timber.d( "expressing interest after timeout");
                networkDiscoveryTries++;
                if (networkDiscoveryTries <= maxnetworkDiscoveryTries) {
                    expressNetworkDiscoveryInterest();
                } else if (networkDiscoveryTries == maxnetworkDiscoveryTries + 1){
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
                    Timber.d( "Connected via WifiDirect");
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
        Realm realm = Realm.getDefaultInstance();

        RealmResults<User> friends = realm.where(User.class).equalTo("friend", true).findAll();
        for (User u : friends) {
            try {
                Nfdc.register(face, myFace, new Name(u.getNamespace()), 0);
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }
        realm.close();
    }

    public void registerUser(String friendName) {
        Realm realm = Realm.getDefaultInstance();
        User friend = realm.where(User.class).equalTo("username", friendName).findFirst();
        try {
            Nfdc.register(face, Globals.multicastFaceID, new Name(friend.getNamespace()), 0);
        } catch (ManagementException e) {
            e.printStackTrace();
        }
    }

    public void registerWithNSD() {
        Timber.d("Discovering nodes");
        NSDHelper nsdHelper;
        nsdHelper = new NSDHelper(sharedPrefsManager.getNamespace(), getApplicationContext(), face);
        Globals.setNSDHelper(nsdHelper);
        nsdHelper.discoverServices();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Timber.d("requestCode: %s", requestCode);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Timber.d("Got result data");
            // check if we even took a picture
            if (m_curr_photo_file != null && m_curr_photo_file.length() > 0) {
                Timber.d("We have an actual file");

                FileOutputStream out = null;
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(m_curr_photo_file.getAbsolutePath());
                    out = new FileOutputStream(m_curr_photo_file);
                    Timber.d("bitmap is null?: %s", (bitmap == null));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.share_photo).setCancelable(false);

                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send them to a new page to select friends to send photo to
                        Intent intent = new Intent(MainActivity.this, SelectRecipientsActivity.class);
                        Realm realm = Realm.getDefaultInstance();
                        RealmResults<User> friends = realm.where(User.class).equalTo("friend", true).findAll();
                        ArrayList<String> friendsList = new ArrayList<>();
                        for (User f: friends) {
                            Timber.d("Adding friend to friendslist %s", f.getUsername());
                            friendsList.add(f.getUsername());
                        }
                        intent.putStringArrayListExtra("friendsList", friendsList);
                        // make this startActivityForResult and catch the list of recipients;
                        intent.putExtra("photo", m_curr_photo_file.toString());
                        m_curr_photo_file = null;
                        startActivityForResult(intent, SELECT_RECIPIENTS_CODE);
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        runOnUiThread(makeToast("Photo was not shared but can be later."));
                        m_curr_photo_file = null;
                    }
                });

                builder.show();
            }
        }
        else if(requestCode == SELECT_RECIPIENTS_CODE) {
            if(resultCode == RESULT_OK) {
                encryptAndPublish(resultData);
            }
            else {
                runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
            }
        }
        else if (requestCode == ADD_FRIEND_CODE) {
            if(resultCode == RESULT_OK) {
                final String friend = resultData.getStringExtra("username");
                // After adding friend, wait 5 seconds and then send interest for your own certificate signed by your friend
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            generateCertificateInterest(friend);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 5000);

            } else if (resultCode == RESULT_ALREADY_TRUST) {
                final String friendName = resultData.getStringExtra("username");
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                User friend = realm.where(User.class).equalTo("username", friendName).findFirst();
                friend.setFriend(true);
                realm.commitTransaction();
                consumerManager.createConsumer(friend.getNamespace());
                realm.close();


            }
        }
        else if (requestCode == SETTINGS_CODE) {
            Timber.d("SETTINGS_CODE hit");
            FileManager manager = new FileManager(getApplicationContext());
            ImageView imageView = findViewById(R.id.toolbar_main_photo);
            File file = manager.getProfilePhoto();
            if(file == null || file.length() == 0) {
                Picasso.get().load(R.drawable.avatar).fit().centerCrop().into(imageView);
            }
            else {
                // needed the MemoryPolicy.NO_CACHE because it was using the cached (last) profile photo
                // and was not showing the new one when we changed it in SettingsActivity
                Picasso.get().load(file).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(imageView);
            }
        }
        else {
            Timber.d("Unexpected activity requestcode caught");
        }
    }


    // start activity for add friends
    public void startMakingFriends(View view) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, ADD_FRIEND_CODE);
    }

    public void seeRcvdPhotos(View view) {
        Intent intent = new Intent(this, NewContentActivity.class);
        startActivity(intent);
    }

    /**
     * encodes sync data, encrypts photo, and publishes filename and symmetric keys
     * @param resultData: intent with filename and recipients list
     */
    public void encryptAndPublish(Intent resultData) {
        try {
            final String path = resultData.getStringExtra("photo");
            final File photo = new File(path);
            Timber.d("File size: " + photo.length());
            final Uri uri = UriFileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() +
                            ".UriFileProvider", photo);
            final Encrypter encrypter = new Encrypter(getApplicationContext());


            ArrayList<String> recipients;
            try {
                recipients = resultData.getStringArrayListExtra("recipients");
                String name = sharedPrefsManager.getNamespace() + "/data";
                final String filename = sharedPrefsManager.getNamespace() + "/file" + path;

                // Generate symmetric key
                final SecretKey secretKey = encrypter.generateKey();
                final byte[] iv = encrypter.generateIV();

                // Encode sync data
                SyncData syncData = new SyncData();
                syncData.setFilename(filename);

                final boolean feed = (recipients == null);
                if (feed) {
                    Timber.d("For feed");
                    syncData.setFeed(true);
                }
                else {
                    syncData.setFeed(false);
                    Timber.d( "For friends");
                    Realm realm = Realm.getDefaultInstance();
                    for (String friend : recipients) {
                        Blob friendKey = realm.where(User.class).equalTo("username", friend).findFirst().getCert().getPublicKey();
                        byte[] encryptedKey = RsaAlgorithm.encrypt
                                (friendKey, new Blob(secretKey.getEncoded()), new EncryptParams(EncryptAlgorithmType.RsaOaep)).getImmutableArray();
                        syncData.addFriendKey(friend, encryptedKey);
                    }
                }
                // Stringify sync data
                producerManager.setDataSeqMap(syncData.stringify());
                Timber.d("Publishing file: %s", filename);

                byte[] bytes;
                try {
                    InputStream is = MainActivity.this.getContentResolver().openInputStream(uri);
                    bytes = IOUtils.toByteArray(is);
                    Timber.d("select file activity: %s", "file byte array size: " + bytes.length);
                } catch (IOException e) {
                    Timber.d("onItemClick: failed to byte");
                    e.printStackTrace();
                    bytes = new byte[0];
                }
                Timber.d("file selection result: %s", "file path: " + path);
                try {
                    String prefixApp = "/" + sharedPrefsManager.getNamespace();

                    final String prefix = prefixApp + "/file" + path;
                    Timber.d(prefix);
                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    PublishedContent contentKey = realm.createObject(PublishedContent.class, path);
                    if (!feed) {
                        Timber.d("Publishing to friend(s)");
                        contentKey.addKey(secretKey);
                        realm.commitTransaction();
                        realm.close();

                        Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
                        Common.publishData(encryptedBlob, new Name(prefix));
                    }
                    else {
                        Timber.d("Publishing to feed");
                        realm.commitTransaction();
                        realm.close();
                        Blob unencryptedBlob = new Blob(bytes);
                        Common.publishData(unencryptedBlob, new Name(prefix));

                    }
                    final FileManager manager = new FileManager(getApplicationContext());
                    Bitmap bitmap = QRExchange.makeQRCode(prefix);
                    manager.saveFileQR(bitmap, prefix);
                    runOnUiThread(makeToast("Sending photo"));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
                producerManager.publishFile(name);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
        }
    }

    /**
     * Generates and expresses interest for our certificate signed by friend
     * @param friend: name of friend who has our certificate
     */
    public void generateCertificateInterest(String friend) throws SecurityException, IOException {
        Realm realm = Realm.getDefaultInstance();
        User user = realm.where(User.class).equalTo("username", friend).findFirst();
        Name name =  new Name(user.getNamespace());
        name.append(getString(R.string.certificate_prefix));
        Name certName = Globals.keyChain.getDefaultCertificateName();
        Name newCertName = new Name();
        int end = 0;
        for (int i = 0; i<= certName.size(); i++) {
            if (certName.getSubName(i, 1).toUri().equals("/self")) {
                newCertName.append(certName.getPrefix(i));
                end = i;
                break;
            }
        }
        newCertName.append(friend);
        newCertName.append(certName.getSubName(end+1));
        name.append(newCertName);
        Interest interest = new Interest(name);
        Timber.d("Expressing interest for our cert %s", name.toUri());
        registerUser(friend);
        face.expressInterest(interest, onCertData, onCertTimeOut);
    }

    /**
     * Callback for certificate from friend
     */
    OnData onCertData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Timber.d("Getting our certificate back from friend");
            Realm realm = Realm.getDefaultInstance();

            String friendName = interest.getName().getSubName(-2, 1).toUri().substring(1);
            User friend = realm.where(User.class).equalTo("username", friendName).findFirst();
            Blob interestData = data.getContent();
            byte[] certBytes = interestData.getImmutableArray();

            CertificateV2 certificateV2 = new CertificateV2();
            try {
                certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
            } catch (EncodingException e) {
                e.printStackTrace();
            }
            realm.beginTransaction();
            SelfCertificate realmCertificate = realm.where(SelfCertificate.class).equalTo("username", friendName).findFirst();
            if (realmCertificate == null) {
                realmCertificate = realm.createObject(SelfCertificate.class, friendName);
            }
            realmCertificate.setCert(certificateV2);

            VerificationHelpers verificationHelpers = new VerificationHelpers();
            try {
                boolean verified = verificationHelpers.verifyDataSignature(certificateV2, realm.where(User.class).equalTo("username", friendName).findFirst().getCert());
            } catch (EncodingException e) {
                e.printStackTrace();
            }

            Timber.d("Saved our certificate back signed by friend and adding them as a consumer");

            friend.setFriend(true);
            friend.setTrust(true);
            consumerManager.createConsumer(friend.getNamespace());
            realm.commitTransaction();

            // Share friend's list
            producerManager.updateFriendsList();

            if (!Globals.useMulticast) {
                    Globals.nsdHelper.registerUser(friendName);
            } else {
                User user = realm.where(User.class).equalTo("username", friendName).findFirst();
                try {
                    Nfdc.register(face, Globals.multicastFaceID, new Name(user.getNamespace()), 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }

            }
            realm.close();
        }
    };

    /**
     * Callback for timeout of interest for certificate from friend
     */
    OnTimeout onCertTimeOut = new OnTimeout() {

        @Override
        public void onTimeout(Interest interest) {
            Timber.d( "Timeout for interest " + interest.toUri());
            String friend = interest.getName().getSubName(-2, 1).toString().substring(1);
            Timber.d("Resending interest to " + friend);
            try {
                generateCertificateInterest(friend);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Triggered by button press. This acts as a helper function to first ask for permission to
     * access the camera if we do not have it. If we are granted permission or have permission, we
     * will call startCamera()
     */
    public void startUpCamera(View view) {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        else {
            startCamera();
        }
    }

    /**
     * Opens the camera so we can capture an image or video. See onActivityResult for how media
     * is handled.
     */
    public void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // name the photo by using current time
        String tsPhoto = (new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())) + ".jpg";
        /* The steps below are necessary for photo captures. We set up a temporary file for our
           photo and pass the information to the Camera Activity. This is where it will store the
           photo if we choose to save it. */
        FileManager manager = new FileManager(getApplicationContext());
        File pic = new File(manager.getPhotosDir(), tsPhoto);
        m_curr_photo_file = pic;
        final Uri uri = UriFileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() +
                        ".UriFileProvider", pic);
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pic));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * This checks if the user gave us permission for the camera or not when the dialog box popped up.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
            else {
                runOnUiThread(makeToast("Can't access camera without your permission."));
            }
        }
    }

    public void startFiles(View view) {
        Intent intent = new Intent(this, FilesActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        Timber.d("Destroying memory cache");
        //memoryCache.destroy();
        super.onDestroy();
    }
}
