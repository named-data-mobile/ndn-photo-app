package memphis.myapplication.UI;

import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import memphis.myapplication.R;
import memphis.myapplication.data.RealmObjects.FilesInfo;
import memphis.myapplication.data.RealmRepository;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
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
    private TextView m_location;
    private int m_index;
    private View viewPhotosView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewPhotosView = inflater.inflate(R.layout.fragment_view_photos, container, false);

        m_imgView = viewPhotosView.findViewById(R.id.photoImgView);
        m_location = viewPhotosView.findViewById(R.id.location);
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
                if(tick/5000>=1 && isAdded() && getActivity() != null) {
                    String photo = photos.get(m_index);
                    File photoFile = new File(photo);
                    Picasso.get().load(photoFile).fit().centerCrop().into(m_imgView);

                    Timber.d("Displating: "+photo.substring(photo.lastIndexOf('_')+ 1));
                    FilesInfo filesInfo = RealmRepository.getInstance().getFileInfo(photo.substring(photo.lastIndexOf('_')+ 1));
                    if(filesInfo.location){
                        float latitude = 0;
                        float longitude = 0;
                        ExifInterface exif;

                        try {
                            exif = new ExifInterface(photoFile.getAbsolutePath());
                            String attrLATITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                            String attrLATITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                            String attrLONGITUDE = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                            String attrLONGITUDE_REF = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                            Timber.d(attrLATITUDE + " : " + attrLONGITUDE );

                            if((attrLATITUDE !=null) && (attrLATITUDE_REF !=null) && (attrLONGITUDE != null)
                                    && (attrLONGITUDE_REF !=null))
                            {
                                if(attrLATITUDE_REF.equals("N")){
                                    latitude = convertToDegree(attrLATITUDE);
                                }
                                else{
                                    latitude = 0 - convertToDegree(attrLATITUDE);
                                }

                                if(attrLONGITUDE_REF.equals("E")){
                                    longitude = convertToDegree(attrLONGITUDE);
                                }
                                else{
                                    longitude = 0 - convertToDegree(attrLONGITUDE);
                                }
                            }
                            
                            Timber.d(latitude + " : " + longitude );
                            m_location.setVisibility(View.VISIBLE);
                            m_location.setText(latitude + " : "+ longitude);
                        } catch (IOException e) {
                            e.printStackTrace();
                            m_location.setVisibility(View.GONE);
                            m_location.setText("");
                            Toast.makeText(getActivity(), "Error in getting location", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        m_location.setVisibility(View.GONE);
                        m_location.setText("");
                    }
                    // check that we are not deleting the first one otherwise it would be 0 - 1 and we
                    // would delete the last one. So we don't want to delete it while viewing
                    if (m_index - 1 >= 0) {
                        // unless we just set the first photo, delete the previously set photo using
                        // the path in our photos arrayList
                        String photoToDelete = photos.get(m_index - 1);
                        File fileToDelete = new File(photoToDelete);
                        boolean wasDeleted = fileToDelete.delete();
                        if(wasDeleted){
                            RealmRepository.getInstance().deleteFileInfo(photoToDelete.substring(photoToDelete.lastIndexOf('_')+ 1));
                        }
                        Timber.d("file with path: " + photoToDelete + " was deleted? " + wasDeleted);
                    }
                    Timber.d("We set the imageURI with index number: " + m_index);
                    m_index++;
                }
            }

        public void onFinish() {
                // since we are deleting photos only after we set the next one, we need to delete the
                // last photo
            if (isAdded() && getActivity() != null) {
                    String photoToDelete = photos.get(m_index - 1);
                    File fileToDelete = new File(photoToDelete);
                    fileToDelete.delete();
                    Navigation.findNavController(viewPhotosView).popBackStack();
                }
            }

        }.start();
    }
    private static float convertToDegree(String stringDMS){
        Float result;
        String[] dms = stringDMS.split(",", 3);

        String[] stringD = dms[0].split("/", 2);
        double d0 = Double.valueOf(stringD[0]);
        double d1 = Double.valueOf(stringD[1]);
        double degree = d0/d1;

        String[] stringM = dms[1].split("/", 2);
        double m0 = Double.valueOf(stringM[0]);
        double m1 = Double.valueOf(stringM[1]);
        double minute = m0/m1;

        String[] stringS = dms[2].split("/", 2);
        double S0 = Double.valueOf(stringS[0]);
        double S1 = Double.valueOf(stringS[1]);
        double second = S0/S1;

        result = (float) (degree + (minute / 60) + (second / 3600));
        return result;
    }
}
