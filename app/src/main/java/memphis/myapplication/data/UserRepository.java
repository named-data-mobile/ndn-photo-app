package memphis.myapplication.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;

import memphis.myapplication.utilities.FileManager;
import timber.log.Timber;
/**
 * UserRepository handles all realm database related stuff for user data
 */
public class UserRepository {

    private static UserRepository instance;
    private final MutableLiveData<Uri> userImage;

    public UserRepository(Context context) {
        userImage = new MutableLiveData<>();
        FileManager manager = new FileManager(context);
        File file = manager.getProfilePhoto();
        userImage.setValue(Uri.fromFile(file));
    }

    /**
     * Create or get a single common instance of UserRepository
     * @param context
     * @return The UserRepository instance
     */
    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UserRepository(context);

        }
        return instance;
    }

    /**
     * Get the user profile image livedata
     */
    public LiveData<Uri> getUserImage() {
        return userImage;
    }

    /**
     * Update the user profile image
     * @param photoUri Uri for the picture
     * @param activity
     */
    public void updateImage(Uri photoUri, Context activity) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), photoUri);
            FileManager manager = new FileManager(activity);
            manager.setProfilePhoto(bitmap);
            userImage.setValue(photoUri);
        } catch (IOException e) {
            Timber.d("profilePhoto: %s", "Problem making bitmap from chosen photo");
        }
    }

}
