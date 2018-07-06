package memphis.myapplication;

import android.app.Application;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.certificate.PublicKey;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;

public class Globals extends Application {
    public static Face face;
    public static FaceProxy faceProxy;
    public static AndroidSqlite3IdentityStorage identityStorage;
    public static FilePrivateKeyStorage privateKeyStorage;
    public static IdentityManager identityManager;
    public static KeyChain keyChain;
    public static Name pubKeyName;
    public static boolean has_setup_security;

    // add some checks for the file related keys and identity stuff; we do not want to overwrite them
    // if they are present. Face and FaceProxy can be new, but face will need to set things with keychain again.
    public Globals() {
    }

    // setters
    public static void setFace(Face f) {
        face = f;
    }

    public static void setFaceProxy(FaceProxy fp) {
        faceProxy = fp;
    }

    public static void setIdentityStorage(AndroidSqlite3IdentityStorage is) {
        identityStorage = is;
    }

    public static void setFilePrivateKeyStorage(FilePrivateKeyStorage fpks) {
        privateKeyStorage = fpks;
    }

    public static void setIdentityManager(IdentityManager im) {
        identityManager = im;
    }

    public static void setKeyChain(KeyChain kc) {
        keyChain = kc;
    }

    public static void setPubKeyName(Name pk) {pubKeyName = pk; }

    public static void setHasSecurity(boolean yesNo) {
        has_setup_security = yesNo;
    }

}
