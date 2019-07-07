package memphis.myapplication.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import memphis.myapplication.R;
import memphis.myapplication.viewmodels.UserModel;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

public class SettingsFragment extends Fragment {

    final int PICK_PHOTO = 0;
    private ImageView m_imageView;
    private View settingsView;
    private UserModel userModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false);

        userModel = ViewModelProviders.of(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UserModel(getActivity());
            }
        }).get(UserModel.class);
        m_imageView = settingsView.findViewById(R.id.profilePhoto);

        userModel.getUserImage().observe(this, new Observer<Uri>() {
            @Override
            public void onChanged(Uri uri) {
                Picasso.get().load(uri).placeholder(R.drawable.avatar).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(m_imageView);
                setupToolbar(uri);
            }
        });

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
                Timber.i("changing: "+photoUri);
                userModel.updateImage(photoUri, getActivity());
            }
        }
    }
    private void setupToolbar(Uri uri) {
        ToolbarHelper toolbarHelper = new ToolbarHelper("Settings", settingsView);
        Toolbar toolbar = toolbarHelper.setupToolbar(uri);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }
}
