package memphis.myapplication;

import android.app.Application;

import net.named_data.jndn.Face;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;

public class Globals extends Application {
    private Face face;
    private FaceProxy faceProxy;
    private AndroidSqlite3IdentityStorage identityStorage;
    private FilePrivateKeyStorage privateKeyStorage;
    private IdentityManager identityManager;
    private KeyChain keyChain;

    public Globals(Face f, FaceProxy fp, AndroidSqlite3IdentityStorage is, FilePrivateKeyStorage fpks,
                   IdentityManager im, KeyChain kc) {
        setFace(f);
        setFaceProxy(fp);
        setIdentityStorage(is);
        setFilePrivateKeyStorage(fpks);
        setIdentityManager(im);
        setKeyChain(kc);
    }

    public void setFace(Face face) {
        this.face = face;
    }

    public void setFaceProxy(FaceProxy fp) {
        this.faceProxy = fp;
    }

    public void setIdentityStorage(AndroidSqlite3IdentityStorage is) {
        this.identityStorage = is;
    }

    public void setFilePrivateKeyStorage(FilePrivateKeyStorage fpks) {
        this.privateKeyStorage = fpks;
    }

    public void setIdentityManager(IdentityManager im) {
        this.identityManager = im;
    }

    public void setKeyChain(KeyChain kc) {
        this.keyChain = kc;
    }
    ////
    public Face getFace() {
        return this.face;
    }

    public FaceProxy getFaceProxy() {
        return this.faceProxy;
    }

    public AndroidSqlite3IdentityStorage getIdentityStorage() {
        return this.identityStorage;
    }

    public FilePrivateKeyStorage getFilePrivateKeyStorage() {
        return this.privateKeyStorage;
    }

    public IdentityManager getIdentityManager() {
        return this.identityManager;
    }

    public KeyChain getKeyChain() {
        return this.keyChain;
    }
}
