package memphis.myapplication.UI;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import net.named_data.jndn.Face;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import memphis.myapplication.R;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.viewmodels.BackgroundViewModel;
import memphis.myapplication.viewmodels.RealmViewModel;
import memphis.myapplication.viewmodels.UserModel;
import timber.log.Timber;

public class MainFragment extends Fragment {

    public Face face;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final int CAMERA_REQUEST_CODE = 0;
    private File m_curr_photo_file;

    SharedPrefsManager sharedPrefsManager;
    private View mainView;
    private RealmViewModel databaseViewModel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_main, container, false);

        ViewModelProviders.of(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new BackgroundViewModel(getActivity().getApplicationContext());
            }
        }).get(BackgroundViewModel.class);

        UserModel userModel = ViewModelProviders.of(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UserModel(getActivity());
            }
        }).get(UserModel.class);
        databaseViewModel = ViewModelProviders.of(getActivity()).get(RealmViewModel.class);

        userModel.getUserImage().observe(this, new androidx.lifecycle.Observer<Uri>() {
            @Override
            public void onChanged(Uri uri) {
                setupToolbar(uri);
            }
        });

        setUpListeners();

        return mainView;
    }

    private void setUpListeners() {
        // start activity for add friends
        mainView.findViewById(R.id.friends).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_addFriendFragment);
            }
        });

        /*
          Triggered by button press. This acts as a helper function to first ask for permission to
          access the camera if we do not have it. If we are granted permission or have permission, we
          will call startCamera()
         */
        mainView.findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                } else {
                    startCamera();
                }
            }
        });

        mainView.findViewById(R.id.rcvdImages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_newContentFragment);
            }
        });

        mainView.findViewById(R.id.files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_filesFragment);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // check if user has given us permissions for storage manipulation (one time dialog box)
        int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    getActivity(),
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        sharedPrefsManager = SharedPrefsManager.getInstance(getActivity());
    }

    private void setupToolbar(Uri uri) {
        ToolbarHelper toolbarHelper = new ToolbarHelper(getString(R.string.app_name), mainView);
        Toolbar toolbar = toolbarHelper.setupToolbar(uri);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Timber.d("item: " + item.toString());
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_settingsFragment);
                return true;
            case R.id.action_about:
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_aboutFragment);
                return true;
            case R.id.action_register:
                Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_registerFragment);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Timber.d("requestCode: %s", requestCode);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Timber.d("Got result data");
            // check if we even took a picture
            if (m_curr_photo_file != null && m_curr_photo_file.length() > 0) {
                Timber.d("We have an actual file");

                FileOutputStream out = null;
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(m_curr_photo_file.getAbsolutePath());
                    out = new FileOutputStream(m_curr_photo_file);
                    Timber.d("bitmap is null?: %s", (bitmap == null));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.share_photo).setCancelable(false);

                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send them to a new page to select friends to send photo to
                        ArrayList<User> friendsList = databaseViewModel.getAllFriends();
                        ArrayList<String> friends = new ArrayList<>();
                        for (User f : friendsList) {
                            Timber.d("Adding friend to friendslist %s", f.getUsername());
                            friends.add(f.getUsername());
                        }
                        Bundle bundle = new Bundle();
                        bundle.putString("photo", m_curr_photo_file.toString());
                        bundle.putBoolean("isFile", false);
                        bundle.putSerializable("friendsList", friends);
                        m_curr_photo_file = null;
                        Navigation.findNavController(mainView).navigate(R.id.action_mainFragment_to_selectRecipientsFragment, bundle);
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(), "Photo was not shared but can be later.", Toast.LENGTH_SHORT).show();
                        m_curr_photo_file = null;
                    }
                });

                builder.show();
            }
        }
    }

    /**
     * Opens the camera so we can capture an image or video. See onActivityResult for how media
     * is handled.
     */
    public void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // name the photo by using current time
        String tsPhoto = (new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())) + ".jpg";
        /* The steps below are necessary for photo captures. We set up a temporary file for our
           photo and pass the information to the Camera Activity. This is where it will store the
           photo if we choose to save it. */
        FileManager manager = new FileManager(getActivity().getApplicationContext());
        File pic = new File(manager.getPhotosDir(), tsPhoto);
        m_curr_photo_file = pic;
        final Uri uri = FileProvider.getUriForFile(getActivity(),
                getActivity().getApplicationContext().getPackageName() +
                        ".fileProvider", pic);
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pic));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * This checks if the user gave us permission for the camera or not when the dialog box popped up.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getActivity(), "Can't access camera without your permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        Timber.d("Destroying memory cache");
        //memoryCache.destroy();
        super.onDestroyView();
    }
}
