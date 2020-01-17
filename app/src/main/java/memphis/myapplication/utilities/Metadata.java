package memphis.myapplication.utilities;


import net.named_data.jndn.Name;

import org.json.JSONException;
import org.json.JSONObject;

public class Metadata {
    private String
            bloomFilter;
    private String filename;
    private boolean feed = false;
    JSONObject jo;


    public Metadata() {
        jo = new JSONObject();
    }

    public Metadata(String j) throws JSONException {

        jo = new JSONObject(j);
    }

    public void setFilename(String f) throws JSONException {
        jo.put("filename", f);
    }

    public void addLocation(Boolean b) throws JSONException {
        jo.put("location", b);
    }

    public boolean isLocation() throws JSONException {
        return jo.getBoolean("location");
    }

    public void setIsFile(boolean f) throws JSONException {
        jo.put("isFile", f);
    }

    public boolean isFile() throws JSONException {
        return jo.getBoolean("isFile");
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

    public void setBloomFilter(Name bf) throws JSONException {
        jo.put("bloomFilter", bf.toUri());
}

    public Name getBloomFilter() throws JSONException {
        return new Name(jo.getString("bloomFilter"));
    }

    public String stringify() {
        return jo.toString();
    }
}
