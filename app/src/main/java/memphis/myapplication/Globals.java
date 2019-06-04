package memphis.myapplication;

import android.app.Application;
import timber.log.Timber;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.PSync;

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
