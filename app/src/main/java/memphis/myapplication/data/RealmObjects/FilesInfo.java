package memphis.myapplication.data.RealmObjects;

public class FilesInfo {

    public String filename;
    public String filePath;

    public String producer;
    public boolean feed;

    public boolean location;
    public double latitude;
    public double longitude;

    public FilesInfo() {
    }

    public FilesInfo(String filename, String filePath, String producer, boolean feed, boolean location, double latitude, double longitude) {
        this.filename = filename;
        this.filePath = filePath;
        this.producer = producer;
        this.feed = feed;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
