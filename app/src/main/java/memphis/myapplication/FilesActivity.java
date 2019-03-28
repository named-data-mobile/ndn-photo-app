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
import android.util.Log;
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

import memphis.myapplication.tasks.FetchingTask;

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

        Log.d("onActivityResult", "requestCode: " + requestCode);
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
                        Log.d("select file activity", "file byte array size: " + bytes.length);
                    } catch (IOException e) {
                        Log.d("onItemClick", "failed to byte");
                        e.printStackTrace();
                        bytes = new byte[0];
                    }
                    Log.d("file selection result", "file path: " + path);
                    final Blob blob = new Blob(bytes, true);
                    String prefix = m_manager.addAppPrefix(path);
                    publishData(blob, new Name(prefix));
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
                Log.d("onActivityResult", "Unexpected activity requestcode caught");
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
                Log.d("file selection", "docId: " + docId);
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
        Log.d("lookup_file_QR", appDir.toString());
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
        Log.d("browse", uri.toString());
        // start in app's file directory and limit allowable selections to .png files
        intent.setDataAndType(uri, "*/*");
        startActivityForResult(intent, VIEW_FILE);
    }

    /**
     * This takes a Blob and divides it into NDN data packets
     * @param raw_blob The full content of data in Blob format
     * @param prefix
     * @return returns an ArrayList of all the data packets
     */
    private static ArrayList<Data> packetize(Blob raw_blob, Name prefix) {
        final int VERSION_NUMBER = 0;
        final int DEFAULT_PACKET_SIZE = 8000;
        int PACKET_SIZE = (DEFAULT_PACKET_SIZE > raw_blob.size()) ? raw_blob.size() : DEFAULT_PACKET_SIZE;
        ArrayList<Data> datas = new ArrayList<>();
        int segment_number = 0;
        ByteBuffer byteBuffer = raw_blob.buf();
        do {
            // need to check for the size of the last segment; if lastSeg < PACKET_SIZE, then we
            // should not send an unnecessarily large packet. Also, if smaller, we need to prevent BufferUnderFlow error
            if (byteBuffer.remaining() < PACKET_SIZE) {
                PACKET_SIZE = byteBuffer.remaining();
            }
            Log.d("packetize things", "PACKET_SIZE: " + PACKET_SIZE);
            byte[] segment_buffer = new byte[PACKET_SIZE];
            Data data = new Data();
            Name segment_name = new Name(prefix);
            segment_name.appendVersion(VERSION_NUMBER);
            segment_name.appendSegment(segment_number);
            data.setName(segment_name);
            try {
                Log.d("packetize things", "full data name: " + data.getFullName().toString());
            } catch (EncodingException e) {
                Log.d("packetize things", "unable to print full name");
            }
            try {
                Log.d("packetize things", "byteBuffer position: " + byteBuffer.position());
                byteBuffer.get(segment_buffer, 0, PACKET_SIZE);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            data.setContent(new Blob(segment_buffer));
            MetaInfo meta_info = new MetaInfo();
            meta_info.setType(ContentType.BLOB);
            // not sure what is a good freshness period
            meta_info.setFreshnessPeriod(90000);
            if (!byteBuffer.hasRemaining()) {
                // Set the final component to have a final block id.
                Name.Component finalBlockId = Name.Component.fromSegment(segment_number);
                meta_info.setFinalBlockId(finalBlockId);
            }
            data.setMetaInfo(meta_info);
            datas.add(data);
            segment_number++;
        } while (byteBuffer.hasRemaining());
        return datas;
    }

    /**
     * Starts a new thread to publish the file/photo data.
     * @param blob Blob of content
     * @param prefix Name of the file (currently absolute path)
     */
    public void publishData(final Blob blob, final Name prefix) {
        Thread publishingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ArrayList<Data> fileData = new ArrayList<>();
                    ArrayList<Data> packets = packetize(blob, prefix);
                    // it would be null if this file is already in our cache so we do not packetize
                    if(packets != null) {
                        Log.d("publishData", "Publishing with prefix: " + prefix);
                        for (Data data : packetize(blob, prefix)) {
                            Globals.keyChain.sign(data);
                            fileData.add(data);
                        }

                        Globals.memoryCache.putInCache(fileData);
                        String filename = prefix.toUri();
                        Bitmap bitmap = QRExchange.makeQRCode(filename);
                        Log.d("publishData", "filename: " + filename + " bitmap: " + (bitmap == null));
                        m_manager.saveFileQR(bitmap, filename);
                    }
                    else {
                        Log.d("publishData", "No need to publish; " + prefix.toUri() + " already in cache.");
                    }
                } catch (PibImpl.Error | SecurityException | TpmBackEnd.Error |
                        KeyChain.Error e)

                {
                    e.printStackTrace();
                }
            }
        });
        publishingThread.start();
    }

    /**
     * Runs FetchingTask, which will use the SegmentFetcher to retrieve data using the provided Interest
     * @param interest the interest for the data we want
     */
    public void fetch_data(final Interest interest) {
        // /tasks/FetchingTask
        new FetchingTask(this).execute(interest);
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
