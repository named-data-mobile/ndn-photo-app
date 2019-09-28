package memphis.myapplication.data.RealmObjects;

import javax.crypto.SecretKey;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * PublishedContentRealm is the RealmObject Class to store meta information on the files shared
 */
public class PublishedContentRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String filename;

//    @Required
//    private Date date;

    public void addKey(SecretKey sk) {
        key = sk.getEncoded();
    }
    private byte[] key;

    public String getFilename() { return filename; }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }
}
