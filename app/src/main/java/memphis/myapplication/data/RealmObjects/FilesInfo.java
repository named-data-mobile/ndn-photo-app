package memphis.myapplication.data.RealmObjects;

public class FilesInfo {

    public String filename;
    public String filePath;

    public String producer;
    public boolean feed;
    public boolean location;

    public FilesInfo() {
    }

    public FilesInfo(String filename, String filePath, String producer, boolean feed, boolean location) {
        this.filename = filename;
        this.filePath = filePath;
        this.producer = producer;
        this.feed = feed;
        this.location = location;
    }

    public String getFriendName(){
        return producer.substring(producer.indexOf("/npChat") + 8);
    }
}
