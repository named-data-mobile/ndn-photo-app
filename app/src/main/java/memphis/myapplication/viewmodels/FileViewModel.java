package memphis.myapplication.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * FileViewModel is a ViewModel to connect FileDisplayAdapter to the ReceivedFilesFragment Fragment
 */
public class FileViewModel extends ViewModel {

    public MutableLiveData<Integer> clickedFilePosition;

    public FileViewModel() {
        this.clickedFilePosition = new MutableLiveData<>();
    }
}
