package memphis.myapplication.data.RealmObjects;

/**
 * FilesInfo POJO class to represent Files meta information
 */
public class FilesInfo {

    public String filename;
    public String filePath;

    public String producer;
    public boolean feed;
    public boolean location;
    public boolean isFile;

    public FilesInfo() {
    }

    public FilesInfo(String filename, String filePath, String producer, boolean feed, boolean location, boolean isFile) {
        this.filename = filename;
        this.filePath = filePath;
        this.producer = producer;
        this.feed = feed;
        this.location = location;
        this.isFile = isFile;
    }

    public String getFriendName(){
        return producer.substring(producer.indexOf("/npChat") + 8);
    }
}
