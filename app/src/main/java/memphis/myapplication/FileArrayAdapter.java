/*
source: http://custom-android-dn.blogspot.com/2013/01/create-simple-file-explore-in-android.html
 */

package memphis.myapplication;

import java.util.List;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class FileArrayAdapter extends ArrayAdapter<FileItem>{

    private Context c;
    private int id;
    private List<FileItem>items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<FileItem> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }

    public FileItem getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final FileItem fileItem = items.get(position);
        if (fileItem != null) {
            TextView t1 = v.findViewById(R.id.textViewFileName);
            TextView t2 = v.findViewById(R.id.textViewQuantity);
            TextView t3 = v.findViewById(R.id.textViewDate);

            // Take the ImageView from layout and set the city's image
            ImageView icon = v.findViewById(R.id.FileIcon);
            String fileType = fileItem.getImage();
            Drawable image;
            if(fileType.equals("folder")) {
                image = ResourcesCompat.getDrawable(c.getResources(), R.drawable.folder, null);
            }
            else if(fileType.equals("parent_folder")) {
                image = ResourcesCompat.getDrawable(c.getResources(), R.drawable.parent_folder, null);
            }
            // fileType.equals("text_file")
            else {
                image = ResourcesCompat.getDrawable(c.getResources(), R.drawable.text_file, null);
            }

            icon.setImageDrawable(image);
            if(t1!=null) {
                t1.setText(fileItem.getName());
            }
            if(t2!=null) {
                t2.setText(fileItem.getData());
            }
            if(t3!=null) {
                t3.setText(fileItem.getDate());
            }
        }
        return v;
    }
}
