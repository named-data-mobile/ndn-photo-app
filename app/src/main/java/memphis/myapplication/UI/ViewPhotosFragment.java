package memphis.myapplication.UI;

import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import memphis.myapplication.R;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is used to view received photos. This differs from the DisplayQRFragment (which also
 * displays an image) because we want the user to be able to skip past a photo without having to
 * wait for the timer to end by tapping the screen. We do not want this behavior when displaying
 * a QR code to someone. One can view one or more photos and will be started from two different
 * places. One is from the user's received photos page (not currently created) and the other is
 * from the Story page, containing friends' Stories.
 */

public class ViewPhotosFragment extends Fragment {

    private ImageView m_imgView;
    private int m_index;
    private View viewPhotosView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewPhotosView = inflater.inflate(R.layout.fragment_view_photos, container, false);

        m_imgView = viewPhotosView.findViewById(R.id.photoImgView);
        // get content from Intent; should be an ArrayList of Files
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            bundle.getSerializable("photos");
            ArrayList<String> photos = (ArrayList<String>) bundle.getSerializable("photos");
            if (photos == null)
                Navigation.findNavController(viewPhotosView).popBackStack();
           else viewPhotos(photos);
        }else  Navigation.findNavController(viewPhotosView).popBackStack();
        return viewPhotosView;
    }

    /**
     * Take an ArrayList of absolute paths to photos, make a file, and set the image to the
     * ImageView so the user can see the photo. A timer is set so each photo is shown for 5 seconds.
     * The photos are deleted after viewing.
     * @param photos ArrayList<String> of absolute paths
     */
    private void viewPhotos(final ArrayList<String> photos) {
        m_index = 0;
        Timber.d("photos.size(): " + photos.size());
        // kind of an annoying issue, I had to add 100 milliseconds so the correct number of onTicks
        // would be called. We may need to update this in the future so it adapts to more photos. For
        // instance, it might not be enough for 10 photos because I think the transition from photo
        // to photo is where some of our time is lost; maybe do 5050*photos.size() to get that extra time window per photo
        new CountDownTimer(5050*photos.size(),5000) { // 5000 = 5 sec

            public void onTick(long tick) {
                if(tick/5000>=1) {
                    String photo = photos.get(m_index);
                    File photoFile = new File(photo);
                    Picasso.get().load(photoFile).fit().centerCrop().into(m_imgView);
                    // check that we are not deleting the first one otherwise it would be 0 - 1 and we
                    // would delete the last one. So we don't want to delete it while viewing
                    if (m_index - 1 >= 0) {
                        // unless we just set the first photo, delete the previously set photo using
                        // the path in our photos arrayList
                        String photoToDelete = photos.get(m_index - 1);
                        File fileToDelete = new File(photoToDelete);
                        boolean wasDeleted = fileToDelete.delete();
                        Timber.d("file with path: " + photoToDelete + " was deleted? " + wasDeleted);
                    }
                    Timber.d("We set the imageURI with index number: " + m_index);
                    m_index++;
                }
            }

            public void onFinish() {
                // since we are deleting photos only after we set the next one, we need to delete the
                // last photo
                String photoToDelete = photos.get(m_index-1);
                File fileToDelete = new File(photoToDelete);
                fileToDelete.delete();
                Navigation.findNavController(viewPhotosView).popBackStack();
            }

        }.start();
    }
}
