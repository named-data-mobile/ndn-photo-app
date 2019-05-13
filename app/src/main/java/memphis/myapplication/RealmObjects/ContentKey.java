package memphis.myapplication.RealmObjects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class ContentKey extends RealmObject {

    @PrimaryKey
    @Required
    private String filename;

    @Required
    private byte[] key;

    public void addKey(SecretKey sk) {
        key = sk.getEncoded();
    }

    public String getFilename() { return filename; }

    public SecretKey getKey() {
        return  new SecretKeySpec(key, 0, key.length, "AES");
    }

}
