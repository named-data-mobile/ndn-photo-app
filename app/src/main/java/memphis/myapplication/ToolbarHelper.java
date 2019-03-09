package memphis.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.view.View.OnClickListener;

import com.squareup.picasso.Picasso;

import java.io.File;

import static android.support.v4.content.ContextCompat.startActivity;

public class ToolbarHelper {

    public Activity activity;
    private String title;

    public ToolbarHelper(Activity _activity, String title) {
        this.activity = _activity;
        this.title = title;
    }

    public Toolbar setupToolbar() {
        FileManager m_manager = new FileManager(this.activity.getApplicationContext());
        Toolbar toolbar = this.activity.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ImageView imageView = this.activity.findViewById(R.id.toolbar_main_photo);
        TextView textView = this.activity.findViewById(R.id.main_title);
        textView.setText(this.title);
        File file = m_manager.getProfilePhoto();
        if(file == null || file.length() == 0) {
            Picasso.get().load(R.drawable.avatar).fit().centerCrop().into(imageView);
        }
        else {
            Picasso.get().load(file).fit().centerCrop().into(imageView);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                startActivity(activity,intent,null);
            }
        });
        return toolbar;
    }
}
