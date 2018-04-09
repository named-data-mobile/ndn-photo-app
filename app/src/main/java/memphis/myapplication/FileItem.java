/*
source: http://custom-android-dn.blogspot.com/2013/01/create-simple-file-explore-in-android.html
 */

package memphis.myapplication;

import android.support.annotation.NonNull;

public class FileItem implements Comparable<FileItem> {
    private String name;
    private String data;
    private String date;
    private String path;
    private String image;

    public FileItem(String name, String data, String date, String path, String img)
    {
        this.name = name;
        this.data = data;
        this.date = date;
        this.path = path;
        this.image = img;
    }

    public String getName()
    {
        return name;
    }
    public String getData()
    {
        return data;
    }
    public String getDate()
    {
        return date;
    }
    public String getPath()
    {
        return path;
    }
    public String getImage() {
        return image;
    }

    public int compareTo(@NonNull FileItem fItem) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(fItem.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}
