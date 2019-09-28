package memphis.myapplication.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import net.named_data.jndn.Interest;

import memphis.myapplication.data.BackgroundJobs;

/**
 * BackgroundViewModel is the ViewModel to connect the UI to BackgroundJobs class
 */
public class BackgroundViewModel extends ViewModel {

    private final BackgroundJobs repository;

    public LiveData<String> toast() {
        return repository.toast();
    }

    public LiveData<Interest> friendRequest() {
        return repository.getFriendRequest();
    }

    public BackgroundViewModel(Context applicationContext) {
        repository = BackgroundJobs.getInstance(applicationContext);
    }

}
