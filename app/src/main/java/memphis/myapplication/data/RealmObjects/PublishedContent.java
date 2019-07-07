package memphis.myapplication.data.RealmObjects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class PublishedContent {

    private String filename;

    private byte[] key;

    public void addKey(SecretKey sk) {
        key = sk.getEncoded();
    }

    public String getFilename() {
        return filename;
    }

    public SecretKey getKey() {
        if (key == null)
            return null;
        return new SecretKeySpec(key, 0, key.length, "AES");
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}
