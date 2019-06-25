package memphis.myapplication.data.RealmObjects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class PublishedContent extends RealmObject {

    @PrimaryKey
    @Required
    private String filename;

//    @Required
//    private Date date;

    private byte[] key;

    public void addKey(SecretKey sk) {
        key = sk.getEncoded();
    }

    public String getFilename() { return filename; }

    public SecretKey getKey() {
        if (key == null)
            return null;
        return  new SecretKeySpec(key, 0, key.length, "AES");
    }

}
