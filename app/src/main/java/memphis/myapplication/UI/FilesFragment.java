package memphis.myapplication.UI;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import android.util.DisplayMetrics;

import memphis.myapplication.data.Common;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.utilities.QRExchange;
import memphis.myapplication.R;
import memphis.myapplication.viewmodels.RealmViewModel;
import memphis.myapplication.viewmodels.UserModel;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class FilesFragment extends Fragment {

    private final int FILE_SELECT_REQUEST_CODE = 0;
    private final int PICTURE_SELECT_REQUEST_CODE = 4;
    private final int FILE_QR_REQUEST_CODE = 1;
    private final int SCAN_QR_REQUEST_CODE = 2;
    private final int VIEW_FILE = 3;

    private FileManager m_manager;
    private ToolbarHelper toolbarHelper;
    private Toolbar toolbar;
    private View filesView;
    private RealmViewModel databaseViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        filesView = inflater.inflate(R.layout.fragment_files, container, false);

        m_manager = new FileManager(getActivity().getApplicationContext());
        UserModel userModel = ViewModelProviders.of(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UserModel(getActivity());
            }
        }).get(UserModel.class);
        databaseViewModel = ViewModelProviders.of(getActivity()).get(RealmViewModel.class);

        setupToolbar(userModel.getUserImage().getValue());
        setButtonWidth();

        setupListeners();
        return filesView;
    }

    private void setupListeners() {
        /*
          Choose a file to share.
         */
        filesView.findViewById(R.id.picSelectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICTURE_SELECT_REQUEST_CODE);
            }
        });

        filesView.findViewById(R.id.fileSelectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                // browser.
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                // To search for all documents available via installed storage providers,
                // it would be "*/*".
                intent.setType("*/*");
                startActivityForResult(intent, FILE_SELECT_REQUEST_CODE);
            }
        });

        /*
          initiate scan for QR codes upon button press
         */
        filesView.findViewById(R.id.scanFileQR).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanner = new IntentIntegrator(getActivity());
                // only want QR code scanner
                scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
                scanner.setOrientationLocked(true);
                // back facing camera id
                scanner.setCameraId(0);
                Intent intent = scanner.createScanIntent();
                startActivityForResult(intent, SCAN_QR_REQUEST_CODE);
            }
        });

        /*
          Start a file selection activity to find a QR image to display. getActivity() is triggered by pressing
          the "Display QR" button.
         */
        filesView.findViewById(R.id.QRButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ACTION_GET_CONTENT is used for reading; no modifications
                // We're going to find a png file of our choosing (should be used for displaying QR codes,
                // but it can display any image)
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                File appDir = new File(m_manager.getFilesDir());
                Timber.d(appDir.toString());
                // Uri uri = Uri.fromFile(appDir);
                Uri uri = Uri.parse(appDir.toString());
                // start in app's file directory and limit allowable selections to .png files
                intent.setDataAndType(uri, "image/png");
                startActivityForResult(intent, FILE_QR_REQUEST_CODE);
            }
        });

        // browse your rcv'd files; start in rcv'd files dir; for right now, we will have a typical
        // file explorer and opener. getActivity() is intended for testing.
        filesView.findViewById(R.id.viewRcvdButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                File rcvFilesDir = new File(m_manager.getRcvdFilesDir());
                //Uri uri = Uri.fromFile(rcvFilesDir);
                Uri uri = Uri.parse(rcvFilesDir.toString());
                Timber.d("browse: %s", uri.toString());
                // start in app's file directory and limit allowable selections to .png files
                intent.setDataAndType(uri, "*/*");
                startActivityForResult(intent, VIEW_FILE);
            }
        });
    }

    private void setupToolbar(Uri uri) {
        toolbarHelper = new ToolbarHelper(getString(R.string.files), filesView);
        toolbar = toolbarHelper.setupToolbar(uri);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

    private void setButtonWidth() {
        DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        int width = metrics.widthPixels/3;
        Button btn1 = filesView.findViewById(R.id.fileSelectButton);
        btn1.setWidth(width);
        Button btn2 = filesView.findViewById(R.id.scanFileQR);
        btn2.setWidth(width);
        Button btn3 = filesView.findViewById(R.id.QRButton);
        btn3.setWidth(width);
        Button btn4 = filesView.findViewById(R.id.viewRcvdButton);
        btn4.setWidth(width);
    }

    public FileManager getFileManager() {
        return m_manager;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Timber.d("requestCode: " + requestCode);
        Uri uri;
        if (resultData != null) {
            if (requestCode == PICTURE_SELECT_REQUEST_CODE) {

                uri = resultData.getData();
                String path = getFilePath(uri);
                final File m_curr_photo_file = new File(path);
                Timber.d("Path selected: " + path);

                if (path != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle(R.string.share_photo).setCancelable(false);

                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Send them to a new page to select friends to send photo to
                            ArrayList<User> friendsList = databaseViewModel.getAllFriends();
                            ArrayList<String> friends = new ArrayList<>();
                            for (User f : friendsList) {
                                Timber.d("Adding friend to friendsList %s", f.getUsername());
                                friends.add(f.getUsername());
                            }
                            Bundle bundle = new Bundle();
                            bundle.putString("photo", m_curr_photo_file.toString());
                            bundle.putSerializable("friendsList", friends);
                            Navigation.findNavController(filesView).navigate(R.id.action_filesFragment_to_selectRecipientsFragment, bundle);
                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(getActivity(), "Photo was not shared but can be later.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.show();
                }
                else {
                    String msg = "File path could not be resolved";
                    getActivity().runOnUiThread(makeToast(msg));
                }
            }
            else if (requestCode == FILE_SELECT_REQUEST_CODE) {

                uri = resultData.getData();
                String path = getFilePath(uri);

                if (path != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("You selected a file").setMessage(path).show();
                    byte[] bytes;
                    try {
                        InputStream is = getActivity().getContentResolver().openInputStream(uri);
                        bytes = IOUtils.toByteArray(is);
                        Timber.d("select file activity: %s", "file byte array size: " + bytes.length);
                    } catch (IOException e) {
                        Timber.d("onItemClick: %s", "failed to byte");
                        e.printStackTrace();
                        bytes = new byte[0];
                    }
                    Timber.d("file selection result: %s", "file path: " + path);
                    final Blob blob = new Blob(bytes, true);
                    String s = m_manager.addAppPrefix(path);
                    Name prefix = new Name(s);
                    Common.publishData(blob, prefix);

                    String filename = prefix.toUri();
                    Bitmap bitmap = QRExchange.makeQRCode(filename);
                    Timber.d("publishData: %s", "filename: " + filename + " bitmap: " + (bitmap == null));
                    m_manager.saveFileQR(bitmap, filename);
                }
                else {
                    String msg = "File path could not be resolved";
                    getActivity().runOnUiThread(makeToast(msg));
                }
            }
            // We received a request to display a QR image
            else if (requestCode == FILE_QR_REQUEST_CODE) {
                try {
                    // set up a new Activity for displaying. getActivity() way the back button brings us back
                    // to main activity.
                    Bundle bundle = new Bundle();
                    bundle.putString("uri", resultData.getData().toString());
                    Navigation.findNavController(filesView).navigate(R.id.action_filesFragment_to_displayQRFragment, bundle);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SCAN_QR_REQUEST_CODE) {
                IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, resultData);
                if (result == null) {
                    getActivity().runOnUiThread(makeToast("Null"));
                }
                if (result != null) {
                    // check resultCode to determine what type of code we're scanning, file or friend

                    if (result.getContents() == null) {
                        getActivity().runOnUiThread(makeToast("Nothing is here"));
                    } else {
                        String content = result.getContents();
                        // need to check getActivity() content to determine if we are scanning file or friend code
                        getActivity().runOnUiThread(makeToast(content));
                        final Interest interest = new Interest(new Name(content));
                        fetch_data(interest);
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, resultData);
                }
            }
            else if (requestCode == VIEW_FILE){
                ContentResolver cr = getActivity().getContentResolver();
                try {
                    uri = resultData.getData();
                    // cr.openInputStream(uri);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(makeToast("Unable to open file."));
                }
            }
            else {
                Timber.d("Unexpected activity requestcode caught");
            }
        }
    }

    // credit: https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri/41520090

    /**
     * Converts a uri to its appropriate file pathname
     * @param uri file uri
     * @return
     */
    public String getFilePath(Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        if (DocumentsContract.isDocumentUri(getActivity().getApplicationContext(), uri)) {
            if (uri.getAuthority().equals("com.android.externalstorage.documents")) {
                final String docId = DocumentsContract.getDocumentId(uri);
                Timber.d("file selection: %s", "docId: " + docId);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }
            else if (uri.getAuthority().equals("com.android.providers.downloads.documents")) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            }
            else if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
            }
        }

        if (uri.getScheme().equalsIgnoreCase("content")) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;
            try {
                cursor = getActivity().getApplicationContext().getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                return null;
            }
        }
        else if (uri.getScheme().equalsIgnoreCase("file")) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Runs FetchingTask, which will use the SegmentFetcher to retrieve data using the provided Interest
     * @param interest the interest for the data we want
     */
    public void fetch_data(final Interest interest) {
        // /tasks/FetchingTask
//        new FetchingTask(getActivity()).execute(interest);
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }
}
