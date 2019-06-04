package memphis.myapplication;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import timber.log.Timber;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class FilesActivity extends AppCompatActivity {

    private final int FILE_SELECT_REQUEST_CODE = 0;
    private final int FILE_QR_REQUEST_CODE = 1;
    private final int SCAN_QR_REQUEST_CODE = 2;
    private final int VIEW_FILE = 3;

    private FileManager m_manager;
    private ToolbarHelper toolbarHelper;
    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        m_manager = new FileManager(getApplicationContext());

        setupToolbar();
        setButtonWidth();
    }

    private void setupToolbar() {
        toolbarHelper = new ToolbarHelper(this, getString(R.string.files));
        toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }

    private void setButtonWidth() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int width = metrics.widthPixels/3;
        Button btn1 = findViewById(R.id.fileSelectButton);
        btn1.setWidth(width);
        Button btn2 = findViewById(R.id.scanFileQR);
        btn2.setWidth(width);
        Button btn3 = findViewById(R.id.QRButton);
        btn3.setWidth(width);
        Button btn4 = findViewById(R.id.viewRcvdButton);
        btn4.setWidth(width);
    }

    public FileManager getFileManager() {
        return m_manager;
    }

    /**
     * Choose a file to share.
     */
    public void select_files(View view) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");
        startActivityForResult(intent, FILE_SELECT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Timber.d("requestCode: " + requestCode);
        Uri uri;
        if (resultData != null) {
            if (requestCode == FILE_SELECT_REQUEST_CODE) {

                uri = resultData.getData();
                String path = getFilePath(uri);

                if (path != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("You selected a file").setMessage(path).show();
                    byte[] bytes;
                    try {
                        InputStream is = this.getContentResolver().openInputStream(uri);
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
                    runOnUiThread(makeToast(msg));
                }
            }
            // We received a request to display a QR image
            else if (requestCode == FILE_QR_REQUEST_CODE) {
                try {
                    // set up a new Activity for displaying. This way the back button brings us back
                    // to main activity.
                    Intent display = new Intent(this, DisplayQRActivity.class);
                    display.setData(resultData.getData());
                    startActivity(display);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SCAN_QR_REQUEST_CODE) {
                IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, resultData);
                if (result == null) {
                    runOnUiThread(makeToast("Null"));
                }
                if (result != null) {
                    // check resultCode to determine what type of code we're scanning, file or friend

                    if (result.getContents() == null) {
                        runOnUiThread(makeToast("Nothing is here"));
                    } else {
                        String content = result.getContents();
                        // need to check this content to determine if we are scanning file or friend code
                        runOnUiThread(makeToast(content));
                        final Interest interest = new Interest(new Name(content));
                        fetch_data(interest);
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, resultData);
                }
            }
            else if (requestCode == VIEW_FILE){
                ContentResolver cr = getContentResolver();
                try {
                    uri = resultData.getData();
                    // cr.openInputStream(uri);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    runOnUiThread(makeToast("Unable to open file."));
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
        if (DocumentsContract.isDocumentUri(getApplicationContext(), uri)) {
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
                cursor = getApplicationContext().getContentResolver().query(uri, projection, selection, selectionArgs, null);
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
     * Start a file selection activity to find a QR image to display. This is triggered by pressing
     * the "Display QR" button.
     * @param view The view of MainActivity passed by our button press.
     */
    public void lookup_file_QR(View view) {
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

    /**
     * initiate scan for QR codes upon button press
     */
    public void scanFileQR(View view) {
        IntentIntegrator scanner = new IntentIntegrator(this);
        // only want QR code scanner
        scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
        scanner.setOrientationLocked(true);
        // back facing camera id
        scanner.setCameraId(0);
        Intent intent = scanner.createScanIntent();
        startActivityForResult(intent, SCAN_QR_REQUEST_CODE);
    }

    // browse your rcv'd files; start in rcv'd files dir; for right now, we will have a typical
    // file explorer and opener. This is intended for testing.
    public void browseRcvdFiles(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        File rcvFilesDir = new File(m_manager.getRcvdFilesDir());
        //Uri uri = Uri.fromFile(rcvFilesDir);
        Uri uri = Uri.parse(rcvFilesDir.toString());
        Timber.d("browse: %s", uri.toString());
        // start in app's file directory and limit allowable selections to .png files
        intent.setDataAndType(uri, "*/*");
        startActivityForResult(intent, VIEW_FILE);
    }

    /**
     * Runs FetchingTask, which will use the SegmentFetcher to retrieve data using the provided Interest
     * @param interest the interest for the data we want
     */
    public void fetch_data(final Interest interest) {
        // /tasks/FetchingTask
//        new FetchingTask(this).execute(interest);
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }
}
