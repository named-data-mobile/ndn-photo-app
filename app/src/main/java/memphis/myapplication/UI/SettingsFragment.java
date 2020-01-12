package memphis.myapplication.UI;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.viewmodels.UserModel;
import timber.log.Timber;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

import static android.provider.MediaStore.Images.Media.getBitmap;

public class SettingsFragment extends Fragment {

    final int PICK_PHOTO = 0;
    private ImageView m_imageView;
    private View settingsView;
    private UserModel userModel;
    private boolean sharing;
    private SharedPrefsManager sharedPrefsManager;
    private int mCurrRotation = 0;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false);
        sharedPrefsManager = SharedPrefsManager.getInstance(getContext());
        sharing = sharedPrefsManager.getSharing();


        //rotateImageIfRequired();

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

                 //       Picasso.get().load(uri).placeholder(R.drawable.avatar).resize(120, 120).centerCrop()
               //             .into(m_imageView);

            }
        });

        settingsView.findViewById(R.id.Rotate1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
              // Picasso.get().load(R.drawable.avatar).rotate(90f).into(m_imageView);
                m_imageView.setRotation(m_imageView.getRotation() + 90);

                
            }
        });

        settingsView.findViewById(R.id.changePhotoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePhoto(v);
            }
        });

        final CheckBox checkBox = settingsView.findViewById(R.id.check_box_share_friends);
        checkBox.setChecked(sharing);
        checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    sharing = true;
                } else {
                    sharing = false;
                }
                if (sharing == true) {
                    sharedPrefsManager.shareFriendsList(true);
                } else {
                    sharedPrefsManager.shareFriendsList(false);
                }
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

    private  Uri rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) {

        // Detect rotation
         int rotation = getRotation(context, selectedImage);
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotatedImg = Bitmap.createBitmap(img, 0,
                    0, img.getWidth(), img.getHeight(), matrix, true);
            img.recycle();
            return getImageUri(context,rotatedImg);
        }
        else{
            return selectedImage;
        }
    }

    /** * Get the rotation of the last image added.
     * @param context
     * @param selectedImage
     * @return */private  int getRotation(Context context, Uri selectedImage) {

        int rotation = 0;
        ContentResolver content = context.getContentResolver();

        Cursor mediaCursor = content.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { "orientation", "date_added" },
                null, null, "date_added desc");

        if (mediaCursor != null && mediaCursor.getCount() != 0) {
            while(mediaCursor.moveToNext()){
                rotation = mediaCursor.getInt(0);
                break;
            }
        }
        mediaCursor.close();
        return rotation;
    }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(),
                inImage, "Title", null);
        return Uri.parse(path);
    }
}
