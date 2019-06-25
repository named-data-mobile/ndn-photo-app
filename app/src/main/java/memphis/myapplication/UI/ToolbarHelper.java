package memphis.myapplication.UI;

import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import memphis.myapplication.R;
import memphis.myapplication.utilities.FileManager;

public class ToolbarHelper {

    public Activity activity;
    private String title;
    private View view;

//    public ToolbarHelper(Activity _activity, String title) {
//        this.activity = _activity;
//        this.title = title;
//    }

    public ToolbarHelper(Activity activity, String title, View view) {
        this.activity = activity;
        this.title = title;
        this.view = view;
    }

    public ToolbarHelper(Activity _activity) {
        this.activity = _activity;
    }

    public Toolbar setupToolbar() {
        FileManager m_manager = new FileManager(this.activity.getApplicationContext());
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ImageView imageView = view.findViewById(R.id.toolbar_main_photo);
        TextView textView = view.findViewById(R.id.main_title);
        textView.setText(this.title);
        File file = m_manager.getProfilePhoto();
        if(file == null || file.length() == 0) {
            Picasso.get().load(R.drawable.avatar).fit().centerCrop().into(imageView);
        }
        else {
            Picasso.get().load(file).fit().centerCrop().into(imageView);
        }
        return toolbar;
    }

    public void setupToolbarImage(String photoUri) {
        ImageView imageView = view.findViewById(R.id.toolbar_main_photo);
        Picasso.get().load(photoUri).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(imageView);
    }
}
