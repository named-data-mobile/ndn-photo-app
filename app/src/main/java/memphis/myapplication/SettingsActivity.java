package memphis.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    final int PICK_PHOTO = 0;
    private ImageView m_imageView;
    FileManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_settings);
        setupToolbar();
        m_imageView = findViewById(R.id.profilePhoto);
        manager = new FileManager(getApplicationContext());
        File file = manager.getProfilePhoto();
        if(file.length() == 0) {
            Picasso.get().load(R.drawable.avatar).into(m_imageView);
        }
        else {
            Picasso.get().load(file).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(m_imageView);
        }

    }

    // change photo
    public void changePhoto(View view) {
        // open up photos directory
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_PHOTO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if(resultData != null) {
            Uri photoUri = resultData.getData();
            if (photoUri != null && requestCode == PICK_PHOTO) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                    FileManager manager = new FileManager(getApplicationContext());
                    manager.setProfilePhoto(bitmap);
                } catch (IOException e) {
                    Log.d("profilePhoto", "Problem making bitmap from chosen photo");
                }
                Picasso.get().load(photoUri).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(m_imageView);
                ToolbarHelper toolbarHelper = new ToolbarHelper(this);
                toolbarHelper.setupToolbarImage(String.valueOf(photoUri));
            }
        }
    }
    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, "Settings");
        Toolbar toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }
}
