package memphis.myapplication.utilities;


import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncData {
    private Map<String, String> m_symKeys = new HashMap<String, String>();
    private String filename;
    private boolean feed = false;
    JSONObject jo;


    public SyncData() {
        jo = new JSONObject();
    }

    public SyncData(String j) throws JSONException {

        jo = new JSONObject(j);
    }

    public void setFilename(String f) throws JSONException{
        jo.put("filename", f);
    }

    public void addFriendKey (String friend, byte[] key) throws JSONException{
        jo.put(friend, Base64.encodeToString(key, 0));
    }

    public byte[] getFriendKey (String friend) throws JSONException{
        return Base64.decode(jo.getString(friend), 0);
    }

    public String getFilename() throws JSONException {
        return jo.getString("filename");
    }

    public void setFeed(Boolean b) throws JSONException {
        jo.put("feed", b);
    }

    public boolean isFeed() throws JSONException {
        return jo.getBoolean("feed");
    }

    public boolean forMe(String me) {
        return jo.has(me);
    }

    public String stringify() {
        return jo.toString();
    }
}
