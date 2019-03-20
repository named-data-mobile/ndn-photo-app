package memphis.myapplication;

import android.app.Application;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.certificate.PublicKey;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.util.Blob;

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
    public static boolean has_setup_security;

    // add some checks for the file related keys and identity stuff; we do not want to overwrite them
    // if they are present. Face can be new, but face will need to set things with keychain again.
    public Globals() {
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

    public static void setPubKeyName(Name pk) {pubKeyName = pk; }

    public static void setHasSecurity(boolean yesNo) {
        has_setup_security = yesNo;
    }

    public static Name getDefaultIdName() { return defaultIdName; }

}
