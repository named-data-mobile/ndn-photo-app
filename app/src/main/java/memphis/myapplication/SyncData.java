package memphis.myapplication;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SyncData implements Serializable {
    private Map<String, byte[]> m_symKeys = new HashMap<String, byte[]>();
    private String filename;
    private boolean feed = false;

    public SyncData(String f) {
        this.filename = f;
    }

    public void addFriendKey (String friend, byte[] key) {
        m_symKeys.put(friend, key);
    }

    public byte[] getFriendKey (String friend) {
        return m_symKeys.get(friend);
    }

    public String getFilename () {
        return filename;
    }

    public void setFeed() {
        feed = true;
    }

    public boolean isFeed() {
        return feed;
    }

    public boolean forMe(String f) {
        return m_symKeys.containsKey(f);
    }
}
