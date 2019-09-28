package memphis.myapplication.viewmodels;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import memphis.myapplication.data.UserRepository;

/**
 * UserModel is the ViewModel used to pass the user profile information from UI to UserRepository
 */
public class UserModel extends ViewModel {

    private final UserRepository repository;

    public LiveData<Uri> getUserImage() {
        return repository.getUserImage();
    }

    public UserModel(Context context) {
        repository = UserRepository.getInstance(context);
    }

    public void updateImage(Uri photoUri, Context activity) {
        repository.updateImage(photoUri, activity);
    }
}
