package memphis.myapplication;

import android.app.Application;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import io.realm.Realm;
import memphis.myapplication.RealmObjects.SelfCertificate;
import memphis.myapplication.RealmObjects.User;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.VerificationHelpers;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.PSync;

import java.io.IOException;
import java.nio.ByteBuffer;

import memphis.myapplication.psync.ConsumerManager;
import memphis.myapplication.psync.ProducerManager;

public class Globals extends Application {
    public static Face face;
    public static MemoryCache memoryCache;
    //v2 changes
    public static AndroidSqlite3Pib pib;
    public static TpmBackEndFile tpm;
    public static PibIdentity pibIdentity;
    public static Name defaultIdName;
    public static KeyChain keyChain;
    public static Blob pubKeyBlob;
    public static Name pubKeyName;
    public static CertificateV2 certificate;
    public static boolean has_setup_security;
    public static PSync psync;
    public static ProducerManager producerManager;
    public static ConsumerManager consumerManager;
    public static int multicastFaceID;
    public static NSDHelper nsdHelper;
    public static boolean useMulticast;

    // add some checks for the file related keys and identity stuff; we do not want to overwrite them
    // if they are present. Face can be new, but face will need to set things with keychain again.
    public Globals() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber();
    }

    private void setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected  String createStackElementTag(StackTraceElement element) {
                    return "Log: " + element.getLineNumber() + " : "+super.createStackElementTag(element) + " : "+ element.getMethodName();
                }
            });
        } else {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected  String createStackElementTag(StackTraceElement element) {
                    return super.createStackElementTag(element) + " : "+ element.getMethodName();
                }
            });
        }
    }

    /**
     * Generates and expresses interest for our certificate signed by friend
     * @param friend: name of friend who has our certificate
     */
    static void generateCertificateInterest(String friend) throws SecurityException, IOException {
        Realm realm = Realm.getDefaultInstance();
        User user = realm.where(User.class).equalTo("username", friend).findFirst();
        Name name =  new Name(user.getNamespace());
        name.append("cert");
        Name certName = keyChain.getDefaultCertificateName();
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

    public static void registerUser(String friendName) {
        Realm realm = Realm.getDefaultInstance();
        User friend = realm.where(User.class).equalTo("username", friendName).findFirst();
        try {
            Nfdc.register(face, Globals.multicastFaceID, new Name(friend.getNamespace()), 0);
        } catch (ManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback for certificate from friend
     */
    static OnData onCertData = new OnData() {

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
    static OnTimeout onCertTimeOut = new OnTimeout() {

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


    //v2 changes
    public static void setPib(AndroidSqlite3Pib p) {
        pib = p;
    }

    public static void setTpmBackEndFile(TpmBackEndFile t) {
        tpm = t;
    }

    public static void setDefaultPibId(PibIdentity pid) {
        pibIdentity = pid;
    }

    public static void setDefaultIdName(Name pn) {
        defaultIdName = pn;
    }

    // the chain of getters starting with the keychain is ridiculous; just do it once to set it so
    // we don't have to keep calling for it in that manner.
    public static void setPublicKey(Blob pk) {
        pubKeyBlob = pk;
    }

    // setters
    public static void setFace(Face f) {
        face = f;
    }

    public static void setMemoryCache(MemoryCache memoryCache) {
        Globals.memoryCache = memoryCache;
    }

    public static void setKeyChain(KeyChain kc) {
        keyChain = kc;
    }

    public static void setPubKeyName(Name pk) { pubKeyName = pk; }

    public static void setHasSecurity(boolean yesNo) {
        has_setup_security = yesNo;
    }

    public static void setPSync(PSync ps) { psync = ps; }

    public static Name getDefaultIdName() { return defaultIdName; }

    public static void setProducerManager(ProducerManager pm) {producerManager = pm;}

    public static void setConsumerManager(ConsumerManager cm) { consumerManager = cm; }

    public static void setMulticastFaceID(int f) { multicastFaceID = f; }

    public static void setNSDHelper(NSDHelper n) { nsdHelper = n; }

    public static void setUseMulticast(boolean b) { useMulticast = b; }


}
