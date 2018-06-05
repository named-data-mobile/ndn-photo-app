package memphis.myapplication;

import android.app.Application;
import android.util.Log;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.KeyClass;
import net.named_data.jndn.security.KeyIdType;
import net.named_data.jndn.security.KeyParams;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.RsaKeyParams;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.pib.PibImpl;

import java.io.IOException;
import java.security.Security;

public class Globals extends Application {
    public static Face face;
    public static FaceProxy faceProxy;
    public static AndroidSqlite3IdentityStorage identityStorage;
    public static FilePrivateKeyStorage privateKeyStorage;
    public static IdentityManager identityManager;
    public static KeyChain keyChain;
    public static boolean has_setup_security;

    // add some checks for the file related keys and identity stuff; we do not want to overwrite them
    // if they are present. Face and FaceProxy can be new, but face will need to set things with keychain again.
    public Globals() {
    }

    /*public Globals(Face f, FaceProxy fp, AndroidSqlite3IdentityStorage is, FilePrivateKeyStorage fpks,
                   IdentityManager im, KeyChain kc) {
        setFace(f);
        setFaceProxy(fp);
        setIdentityStorage(is);
        setFilePrivateKeyStorage(fpks);
        setIdentityManager(im);
        setKeyChain(kc);
    }*/

    public void setup_security() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileManager manager = new FileManager(getApplicationContext());
                // /ndn-snapchat/<username>
                // Name appAndUsername = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());
                Name appAndUsername = new Name("/ndn-snapchat/" + manager.getUsername() + "/KEY");

                // maybe we should have a separate method that creates a new face and faceProxy. Since the file
                // related storage will be "permanent", then we might need a method that solely sets up the
                // face and its signing info
                face = new Face();
                faceProxy = new FaceProxy();
                // check if identityStorage, privateKeyStorage, identityManager, and keyChain already exist in our phone.
                if (identityStorage == null) {
                    identityStorage = new AndroidSqlite3IdentityStorage(
                            AndroidSqlite3IdentityStorage.getDefaultFilePath(getApplicationContext().getFilesDir())
                    );
                }
                if (privateKeyStorage == null) {
                    privateKeyStorage = new FilePrivateKeyStorage(
                            FilePrivateKeyStorage.getDefaultDirecoryPath(getApplicationContext().getFilesDir())
                    );
                }
                try {
                    // check if key storage exists; we can call generateKeyPair because it has a
                    // check for existing keys built in it. It throws the SecurityException if they exist already.
                    Name keyName = new Name(appAndUsername + "/KEY");
                    privateKeyStorage.generateKeyPair(keyName, new RsaKeyParams(2048));
                }
                catch (SecurityException e) {
                    // keys already exist; no need to generate them again.
                    // I don't know if we should print these; it might be misleading or seem like a
                    // problem.
                    e.printStackTrace();
                }
                // this is fine if we haven't changed anything with storage
                identityManager = new IdentityManager(identityStorage, privateKeyStorage);
                keyChain = new KeyChain(identityManager);
                keyChain.setFace(face);

                Name defaultCertificateName;
                try {
                    defaultCertificateName = keyChain.createIdentityAndCertificate(appAndUsername);
                    keyChain.getIdentityManager().setDefaultIdentity(appAndUsername);
                    Log.d("setup_security", "Certificate was generated.");

                } catch (SecurityException e2) {
                    defaultCertificateName = new Name("/bogus/certificate/name");
                }
                face.setCommandSigningInfo(keyChain, defaultCertificateName);
                has_setup_security = true;
                Log.d("setup_security", "Security was setup successfully");
                // FileManager manager = new FileManager(getApplicationContext());
                // Name username = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());
        /*try {
            register_with_NFD(username);
        } catch (IOException | PibImpl.Error e) {
            e.printStackTrace();
        }*/ // for now, let's just deal with security related stuff; we'll figure out how to handle registration afterwards.
            }
        });
        thread.start();
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

    public static void setHasSecurity(Boolean yesNo) {
        has_setup_security = yesNo;
    }
}
