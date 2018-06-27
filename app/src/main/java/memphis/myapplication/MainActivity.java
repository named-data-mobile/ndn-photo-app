package memphis.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.RsaKeyParams;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static java.lang.Thread.sleep;

import memphis.myapplication.tasks.FetchingTask;

public class MainActivity extends AppCompatActivity {

    // not sure if globals instance is necessary here but this should ensure we have at least one instance so the vars exist
    Globals globals = (Globals) getApplication();
    private Session session;
    final MainActivity m_mainActivity = this;
    String retrieved_data = "";
    AndroidSqlite3IdentityStorage identityStorage;
    FilePrivateKeyStorage privateKeyStorage;
    IdentityManager identityManager;
    public KeyChain keyChain;
    public Face face;
    public FaceProxy faceProxy;
    // think about adding a memoryContentCache instead of faceProxy
    List<String> filesStrings = new ArrayList<String>();
    List<Uri> filesList = new ArrayList<Uri>();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    Name registered_prefix;

    private final int FILE_SELECT_REQUEST_CODE = 0;
    private final int FILE_QR_REQUEST_CODE = 1;
    private final int SCAN_QR_REQUEST_CODE = 2;
    private final int CAMERA_REQUEST_CODE = 3;
    private final int VIEW_FILE = 4;

    private boolean netThreadShouldStop = true;
    // private boolean has_setup_security = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new Session(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.filesList = new ArrayList<Uri>();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        // new addition; we need to check if these are even set though
        // Application app = (Globals) getApplicationContext();
        boolean faceExists = Globals.face == null;
        Log.d("onCreate", "Globals face is null?: " + faceExists + "; Globals security is setup: " + Globals.has_setup_security);
        if (faceExists || !Globals.has_setup_security) {
            setup_security();
        }
        face = Globals.face;
        faceProxy = Globals.faceProxy;
        keyChain = Globals.keyChain;
        // problem here. Security is being setup twice which means something wrong is going on with
        // the Globals class. The static variables aren't being set or we're losing them.
        // since setup_security is its own thread, we are moving forward and hit startNetworkThread before setup_security finishes.
        startNetworkThread();
    }
    // Still think of MainActivity as our true MainActivity, but things will change towards background
    // automation and a better UI. A user is likely not going to know the actual filenames to ask for.
    // The app will take care of this process behind the scenes thanks to our synchronization protocol
    // (and other things) letting the app know what to request. We are in a purely functional stage
    // at the moment.

    public void setup_security() {
        /*Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {*/
        FileManager manager = new FileManager(getApplicationContext());
        // /ndn-snapchat/<username>/KEY
        Name appAndUsername = new Name("/ndn-snapchat/" + manager.getUsername());

        face = new Face();
        //faceProxy = new FaceProxy();
        // check if identityStorage, privateKeyStorage, identityManager, and keyChain already exist in our phone.
        if (identityStorage == null) {
            identityStorage = new AndroidSqlite3IdentityStorage(
                    AndroidSqlite3IdentityStorage.getDefaultFilePath(getApplicationContext().getFilesDir())
            );
            Globals.setIdentityStorage(identityStorage);
        }
        if (privateKeyStorage == null) {
            privateKeyStorage = new FilePrivateKeyStorage(
                    FilePrivateKeyStorage.getDefaultDirecoryPath(getApplicationContext().getFilesDir())
            );
            Globals.setFilePrivateKeyStorage(privateKeyStorage);
        }
        try {
            // check if key storage exists
            Name keyName = new Name(appAndUsername + "/KEY");
            privateKeyStorage.generateKeyPair(keyName, new RsaKeyParams(2048));
        } catch (SecurityException e) {
            // keys already exist; no need to generate them again.
            e.printStackTrace();
        }
        // this is fine if we haven't changed anything with storage
        identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        Globals.setIdentityManager(identityManager);
        keyChain = new KeyChain(identityManager);
        keyChain.setFace(face);

        Name defaultCertificateName;
        try {
            defaultCertificateName = keyChain.createIdentityAndCertificate(appAndUsername);
            keyChain.getIdentityManager().setDefaultIdentity(appAndUsername);
            Log.d("setup_security", "Certificate was generated.");

        } catch (SecurityException e2) {
            defaultCertificateName = new Name("/bogus/certificate/name");
        }
        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setFaceProxy(new FaceProxy());
        Globals.setHasSecurity(true);
        Log.d("setup_security", "Security was setup successfully");
        Name username = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());
        try {
            register_with_NFD(username);
        } catch (IOException | PibImpl.Error e) {
            e.printStackTrace();
        }
    }

    private final Thread networkThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean faceExists = Globals.face == null;
            Log.d("onCreate", "Globals face is null?: " + faceExists + "; Globals security is setup: " + Globals.has_setup_security);
            if (!Globals.has_setup_security) {
                setup_security();
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!netThreadShouldStop) {
                try {
                    face.processEvents();
                    sleep(100);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // face.shutdown();
        }
    });

    private void startNetworkThread() {
        if (!networkThread.isAlive()) {
            netThreadShouldStop = false;
            networkThread.start();
        }
    }

    private void stopNetworkThread() {
        netThreadShouldStop = true;
    }

    protected boolean appThreadIsRunning() {
        return networkThread.isAlive();
    }

    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }

    /**
     * Called when the user taps the fetch data button
     */
    public void fetch_data_button(View view) {
        Log.d("fetch_data", "Called fetch_data");
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        Log.d("fetch_data", "Message from editText: " + message);
        final Interest interest = new Interest(new Name(message));
        Log.d("fetch_data", "Interest: " + interest.getName().toString());
        fetch_data(interest);
    }

    public void fetch_data(final Interest interest) {
        interest.setInterestLifetimeMilliseconds(6000);
        /*SegmentFetcher.fetch(
                face,
                interest,
                new SegmentFetcher.VerifySegment() {
                    @Override
                    public boolean verifySegment(Data data) {
                        Log.d("VerifySegment", "We just return true.");
                        return true;
                    }
                },
                new SegmentFetcher.OnComplete() {
                    @Override
                    public void onComplete(Blob content) {
                        FileManager manager = new FileManager(getApplicationContext());
                        boolean wasSaved = manager.saveContentToFile(content, interest.getName().toUri());
                        if(wasSaved) {
                            String msg = "We got content.";
                            runOnUiThread(makeToast(msg));
                        }
                        else {
                            String msg = "Failed to save retrieved content";
                            runOnUiThread(makeToast(msg));
                        }
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        Log.d("fetch_data onError", message);
                        runOnUiThread(makeToast(message));
                    }
                });*/
        new FetchingTask(m_mainActivity).execute(interest);
        /*Thread fetchingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                new FetchingTask(m_mainActivity).execute(interest);
            }
        });
        fetchingThread.start();*/
    }

    public void register_with_NFD(View view) {
        EditText editText = findViewById(R.id.editText);
        String msg = editText.getText().toString();
        try {
            Name name = new Name(msg);
            register_with_NFD(name);
        } catch (IOException | PibImpl.Error e) {
            e.printStackTrace();
        }
    }

    public void register_with_NFD(Name name) throws IOException, PibImpl.Error {

        if (!Globals.has_setup_security) {
            setup_security();
            while (!Globals.has_setup_security)
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        try {
            Log.d("register_with_nfd", "Starting registration process.");
            long prefixId = face.registerPrefix(name,
                    onDataInterest,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Log.d("OnRegisterFailed", "Registration Failure");
                            String msg = "Registration failed for prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Log.d("OnRegisterSuccess", "Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            String msg = "Successfully registered prefix: " + prefix.toUri();
                            runOnUiThread(makeToast(msg));
                        }
                    });
        }
        catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public void publishData(final Blob blob, final Name prefix) {
        Thread publishingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ArrayList<Data> fileData = new ArrayList<>();
                    Log.d("publishData", "Publishing with prefix: " + prefix);
                    for (Data data : packetize(blob, prefix)) {
                        keyChain.sign(data);
                        fileData.add(data);
                    }
                    faceProxy.putInCache(fileData);
                    FileManager manager = new FileManager(getApplicationContext());
                    String filename = prefix.toUri();
                    Bitmap bitmap = QRExchange.makeQRCode(filename);
                    manager.saveFileQR(bitmap, filename);
                } catch (PibImpl.Error | SecurityException | TpmBackEnd.Error |
                        KeyChain.Error e)

                {
                    e.printStackTrace();
                }
            }
        });
        publishingThread.start();
    }

    public void select_files(View view) {
        /* final ListView lv = (ListView) findViewById(R.id.listview);
        List<String> filesStrings = new ArrayList<String>();*/
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
        Uri uri = null;
        if (resultData != null) {
            if (requestCode == FILE_SELECT_REQUEST_CODE) {
                final ListView lv = (ListView) findViewById(R.id.listview);

                uri = resultData.getData();
                String path = getFilePath(uri);

                if (path != null) {
                    // Log.d("file select result", "String s: " + uri.getPath().toString());
                    filesList.add(uri);
                    // filesStrings.add(uri.toString());
                    filesStrings.add(path);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);
                    lv.setAdapter(adapter);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                    builder.setTitle("You selected a file").setMessage(path).show();
                    byte[] bytes;
                    try {
                        InputStream is = MainActivity.this.getContentResolver().openInputStream(uri);
                        bytes = IOUtils.toByteArray(is);
                        Log.d("select file activity", "file byte array size: " + bytes.length);
                    } catch (IOException e) {
                        Log.d("onItemClick", "failed to byte");
                        e.printStackTrace();
                        bytes = new byte[0];
                    }
                    Log.d("file selection result", "file path: " + path);
                    final Blob blob = new Blob(bytes, true);
                    FileManager manager = new FileManager(getApplicationContext());
                    String prefix = manager.addAppPrefix(path);
                    Log.d("added file prefix", "prefix: " + prefix);
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
                    Intent display = new Intent(this, DisplayFileQRCode.class);
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
            else if (requestCode == CAMERA_REQUEST_CODE) {
                try {
                    Bitmap pic = (Bitmap) resultData.getExtras().get("data");
                    // runOnUiThread(makeToast(pic.toString()));
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                    // runOnUiThread(makeToast("Something went wrong. Null image."));
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
        FileManager manager = new FileManager(getApplicationContext());
        File appDir = new File(manager.getFilesDir());
        Uri uri = Uri.fromFile(appDir);
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

    public ArrayList<Data> packetize(Blob raw_blob, Name prefix) {
        final int VERSION_NUMBER = 0;
        final int DEFAULT_PACKET_SIZE = 8000;
        int PACKET_SIZE = (DEFAULT_PACKET_SIZE > raw_blob.size()) ? raw_blob.size() : DEFAULT_PACKET_SIZE;
        ArrayList<Data> datas = new ArrayList<>();
        int segment_number = 0;
        ByteBuffer byteBuffer = raw_blob.buf();
        do {
            // need to check for the size of the last segment; if lastSeg < PACKET_SIZE, then we
            // should not send an unnecessarily large packet. Also, if smaller, we need to prevent BufferUnderFlow error
            if(byteBuffer.remaining() < PACKET_SIZE) {
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
            meta_info.setFreshnessPeriod(30000);
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

    // start activity for add friends
    public void startMakingFriends(View view) {
        Intent intent = new Intent(this, AddFriend.class);
        startActivity(intent);
    }

    // browse your rcv'd files; start in rcv'd files dir; for right now, we will have a typical
    // file expolorer and opener.
    public void browseRcvdFiles(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        FileManager manager = new FileManager(getApplicationContext());
        File rcvFilesDir = new File(manager.getRcvdFilesDir());
        Uri uri = Uri.fromFile(rcvFilesDir);
        Log.d("browse", uri.toString());
        // start in app's file directory and limit allowable selections to .png files
        intent.setDataAndType(uri, "*/*");
        startActivityForResult(intent, VIEW_FILE);
    }

    /**
     * Triggered by button press. This acts as a helper function to first ask for permission to
     * access the camera if we do not have it. If we are granted permission or have permission, we
     * will call startCamera()
     */
    public void startCamera(View view) {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        else {
            startCamera();
        }
    }

    /**
     * Opens the camera so we can capture an image or video. See onActivityResult for how media
     * is handled.
     */
    public void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // String tsPhoto = new Timestamp(System.currentTimeMillis()).toString() + ".jpg";
        String tsPhoto = (new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())) + ".jpg";
        // String tsPhoto = getDateTimeInstance().toString() + ".jpg";
        FileManager manager = new FileManager(getApplicationContext());
        File pic = new File(manager.getPhotosDir(), tsPhoto);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pic));
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
            else {
                runOnUiThread(makeToast("Can't access camera without your permission."));
            }
        }
    }

    private final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Log.d("OnInterestCallback", "Called OnInterestCallback with Interest: " + interest.getName().toUri());
            faceProxy.process(interest, m_mainActivity);
        }
    };

    // maybe we need our own onData callback since it is used in expressInterest (which is called by the SegmentFetcher)
}
