package memphis.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.DigestAlgorithm;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.RsaKeyParams;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.Tpm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.tpm.TpmKeyHandle;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SignedBlob;

import org.apache.commons.io.IOUtils;

import java.io.File;
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

    // security v2 experimental changes
    // look at KeyChain.java; with V2, it works with CertificateV2, Pib, Tpm, and Validator
    public AndroidSqlite3Pib m_pib;
    public TpmBackEndFile m_tpm;
    public PibIdentity m_pibIdentity;

    //
    // not sure if globals instance is necessary here but this should ensure we have at least one instance so the vars exist
    Globals globals = (Globals) getApplication();
    final MainActivity m_mainActivity = this;
    AndroidSqlite3IdentityStorage identityStorage;
    FilePrivateKeyStorage privateKeyStorage;
    IdentityManager identityManager;
    public KeyChain keyChain;
    public Face face;
    public FaceProxy faceProxy;
    // think about adding a memoryContentCache instead of faceProxy
    // eventually remove these lists; they are used to display the file path of one we selected to publish
    List<String> filesStrings = new ArrayList<String>();
    List<Uri> filesList = new ArrayList<Uri>();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final int FILE_SELECT_REQUEST_CODE = 0;
    private final int FILE_QR_REQUEST_CODE = 1;
    private final int SCAN_QR_REQUEST_CODE = 2;
    private final int CAMERA_REQUEST_CODE = 3;
    private final int VIEW_FILE = 4;

    private boolean netThreadShouldStop = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();

        this.filesList = new ArrayList<Uri>();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // check if user has given us permissions for storage manipulation (one time dialog box)
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        boolean faceExists = (Globals.face == null);
        Log.d("onCreate", "Globals face is null?: " + faceExists +
                "; Globals security is setup: " + Globals.has_setup_security);
        // need to check if we have an existing face or if security is not setup; either way, we
        // need to make changes; see setup_security()
        if (faceExists || !Globals.has_setup_security) {
            setup_security();
        }

        face = Globals.face;
        faceProxy = Globals.faceProxy;
        keyChain = Globals.keyChain;
        try {
            Log.d("MainActivity", "pubKey der: " + Globals.identityManager.getPublicKey(Globals.pubKeyName).getKeyDer().toString());
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
        startNetworkThread();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        FileManager manager = new FileManager(getApplicationContext());
        ImageView imageView = (ImageView) findViewById(R.id.toolbar_photo);
        File file = manager.getProfilePhoto();
        if(file.length() == 0) {
            imageView.setImageResource(R.drawable.bandit);
        }
        else {
            imageView.setImageURI(Uri.fromFile(file));
        }
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        Log.d("menuInflation", "Inflated");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Log.d("item", item.toString());
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Still think of MainActivity as our true MainActivity, but things will change towards background
    // automation and a better UI. A user is likely not going to know the actual filenames to ask for.
    // The app will take care of this process behind the scenes thanks to our synchronization protocol
    // (and other things) letting the app know what to request. We are in a purely functional stage
    // at the moment.

    /**
     * This function sets up identity storage, keys, and the face our app will use.
     */
    public void setup_security() {
        FileManager manager = new FileManager(getApplicationContext());
        // /ndn-snapchat/<username>
        Name appAndUsername = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());

        /*// v2 changes
        Context context = getApplicationContext();
        String rootPath = getApplicationContext().getFilesDir().toString();
        String pibPath = rootPath + "/pib.db";

        face = new Face();
        try {
            m_pib = new AndroidSqlite3Pib(rootPath, "pib.db");
        }
        catch(PibImpl.Error e) {
            e.printStackTrace();
        }

        m_tpm = new TpmBackEndFile(TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
        try {
            m_pib.setTpmLocator(TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
        }
        catch(PibImpl.Error e) {
            e.printStackTrace();
        }

        try {
            keyChain = new KeyChain(pibPath, m_pib.getTpmLocator());
        }
        catch(SecurityException | IOException | PibImpl.Error | KeyChain.Error e) {
            e.printStackTrace();
        }

        Name identity = new Name(appAndUsername);
        Name defaultCertificateName;

        try {
            m_pibIdentity = keyChain.createIdentityV2(identity);
            defaultCertificateName = keyChain.getDefaultCertificateName();
        }
        catch(PibImpl.Error | Pib.Error | Tpm.Error | TpmBackEnd.Error | KeyChain.Error | SecurityException e) {
            e.printStackTrace();
            defaultCertificateName = new Name("/bogus/certificate/name");
        }

        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setFaceProxy(new FaceProxy());
        Globals.setHasSecurity(true);
        Log.d("setup_security", "Security was setup successfully");

        //*/
        face = new Face();
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

        /* Name keyName = new Name(appAndUsername + "/KEY");
        Log.d("setup_security", keyName.getPrefix(-1).toUri());
        Globals.setPubKeyName(keyName);*/

        /*try {
            // check if key storage exists
            privateKeyStorage.generateKeyPair(keyName, new RsaKeyParams(2048));
        } catch (SecurityException e) {
            // keys already exist; no need to generate them again.
            e.printStackTrace();
        }*/
        // this is fine if we haven't changed anything with storage
        identityManager = new IdentityManager(identityStorage, privateKeyStorage);

        /*try {
            identityManager.setDefaultIdentity(appAndUsername);
            identityManager.setDefaultKeyForIdentity(keyName, appAndUsername);
            Name key = identityManager.getDefaultKeyNameForIdentity(appAndUsername);
            Log.d("setup_security", "DefaultKeyName: " + key.toUri());
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }*/

        Globals.setIdentityManager(identityManager);
        keyChain = new KeyChain(identityManager);
        keyChain.setFace(face);

        Name defaultCertificateName;
        try {
            if (keyChain.getIdentityManager().getDefaultCertificate() == null) {
                defaultCertificateName = keyChain.createIdentityAndCertificate(appAndUsername, new RsaKeyParams(2048));
                identityManager.setDefaultIdentity(appAndUsername);
                ArrayList<Name> identities = new ArrayList<>();
                identityManager.getAllIdentities(identities, false);
                for(Name name : identities) {
                    Log.d("Non-Default:", name.toUri());
                }
                identities = new ArrayList<>();
                identityManager.getAllIdentities(identities, true);
                for(Name name : identities) {
                    Log.d("Default:", name.toUri());
                }
                Name key = identityManager.getDefaultKeyNameForIdentity(appAndUsername);
                Globals.setPubKeyName(key);
                Log.d("key", key.toUri());
            }
            else {
                defaultCertificateName = keyChain.getIdentityManager().getDefaultCertificateName();
            }
            Log.d("setup_security", "Certificate was generated. " + defaultCertificateName.toUri());

        } catch (SecurityException e2) {
            defaultCertificateName = new Name("/bogus/certificate/name");
        }

        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setFaceProxy(new FaceProxy());
        Globals.setHasSecurity(true);
        Log.d("setup_security", "Security was setup successfully");

        try {
            // since everyone is a potential producer, register your prefix
            register_with_NFD(appAndUsername);
        } catch (IOException | PibImpl.Error e) {
            e.printStackTrace();
        }
    }

    // Eventually, we should move this to a Service, but for now, this thread consistently calls
    // face.processEvents() to check for any changes, such as publishing or fetching.
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

    /**
     * Runs FetchingTask, which will use the SegmentFetcher to retrieve data using the provided Interest
     * @param interest the interest for the data we want
     */
    public void fetch_data(final Interest interest) {
        interest.setInterestLifetimeMilliseconds(10000);
        // /tasks/FetchingTask
        new FetchingTask(m_mainActivity).execute(interest);
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

    /**
     * Registers the provided name with NFD. This is intended to occur whenever the app starts up.
     * @param name The provided name should be /ndn-snapchat/<username>
     * @throws IOException
     * @throws PibImpl.Error
     */
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
            face.registerPrefix(name,
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
                    Log.d("publishData", "Publishing with prefix: " + prefix);
                    for (Data data : packetize(blob, prefix)) {
                        keyChain.sign(data);
                        fileData.add(data);
                    }
                    // new idea: verify your own data to cross off incompatible keys problem

                    Data data = fileData.get(0);
                    SignedBlob encoding = data.wireEncode(WireFormat.getDefaultWireFormat());
                    boolean wasVerified = FetchingTask.verifySignature(encoding.signedBuf(), data.getSignature().getSignature().getImmutableArray(),
                            Globals.identityManager.getPublicKey(Globals.pubKeyName), DigestAlgorithm.SHA256);
                    Log.d("wasVerifiedMain", "" + wasVerified);

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

    /**
     * This takes a Blob and divides it into NDN data packets
     * @param raw_blob The full content of data in Blob format
     * @param prefix
     * @return returns an ArrayList of all the data packets
     */
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
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivity(intent);
    }

    // browse your rcv'd files; start in rcv'd files dir; for right now, we will have a typical
    // file explorer and opener. This is intended for testing.
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
        // name the photo by using current time
        String tsPhoto = (new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())) + ".jpg";
        /* The steps below are necessary for photo captures. We set up a temporary file for our
           photo and pass the information to the Camera Activity. This is where it will store the
           photo if we choose to save it. */
        FileManager manager = new FileManager(getApplicationContext());
        File pic = new File(manager.getPhotosDir(), tsPhoto);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pic));
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * This checks if the user gave us permission for the camera or not when the dialog box popped up.
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
            }
            else {
                runOnUiThread(makeToast("Can't access camera without your permission."));
            }
        }
    }

    /**
     * This is registered with our prefix. Any interest sent with prefix /ndn-snapchat/<username>
     * will be caught by this callback. We send it to the faceProxy to deal with it.
     */
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
