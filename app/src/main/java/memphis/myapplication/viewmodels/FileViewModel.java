package memphis.myapplication.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FileViewModel extends ViewModel {

    public MutableLiveData<Integer> clickedFilePosition;

    public FileViewModel() {
        this.clickedFilePosition = new MutableLiveData<>();
    }
}
