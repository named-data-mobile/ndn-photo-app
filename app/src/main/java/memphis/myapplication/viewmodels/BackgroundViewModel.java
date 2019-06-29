package memphis.myapplication.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import net.named_data.jndn.Interest;

import memphis.myapplication.data.BackgroundJobs;
import timber.log.Timber;

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
