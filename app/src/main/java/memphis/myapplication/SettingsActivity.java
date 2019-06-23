package memphis.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends Fragment {

    final int PICK_PHOTO = 0;
    private ImageView m_imageView;
    FileManager manager;
    private View settingsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false);

        setupToolbar();
        m_imageView = settingsView.findViewById(R.id.profilePhoto);
        manager = new FileManager(getActivity().getApplicationContext());
        File file = manager.getProfilePhoto();
        if(file.length() == 0) {
            Picasso.get().load(R.drawable.avatar).into(m_imageView);
        }
        else {
            Picasso.get().load(file).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(m_imageView);
        }

        settingsView.findViewById(R.id.changePhotoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePhoto(v);
            }
        });
        return settingsView;
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
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoUri);
                    FileManager manager = new FileManager(getActivity().getApplicationContext());
                    manager.setProfilePhoto(bitmap);
                } catch (IOException e) {
                    Timber.d("profilePhoto: %s", "Problem making bitmap from chosen photo");
                }
                Picasso.get().load(photoUri).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(m_imageView);
                ToolbarHelper toolbarHelper = new ToolbarHelper(getActivity(), "Settings", settingsView);
                toolbarHelper.setupToolbarImage(String.valueOf(photoUri));
            }
        }
    }
    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(getActivity(), "Settings", settingsView);
        Toolbar toolbar = toolbarHelper.setupToolbar();
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }
}
