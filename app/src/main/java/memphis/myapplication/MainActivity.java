package memphis.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;



import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.AndroidSqlite3Pib;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibIdentity;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.pib.PibKey;
import net.named_data.jndn.security.tpm.Tpm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.util.Blob;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import static java.lang.Thread.sleep;

import memphis.myapplication.psync.Consumer;
import memphis.myapplication.psync.Consumer.ReceiveSyncCallback;
import memphis.myapplication.psync.Producer;
import memphis.myapplication.tasks.FetchingTask;

public class MainActivity extends AppCompatActivity {

    public AndroidSqlite3Pib m_pib;
    public TpmBackEndFile m_tpm;

    // not sure if this globals instance is necessary here but this should ensure we have at least
    // one instance so the security vars exist
    Globals globals = (Globals) getApplication();
    public KeyChain keyChain;
    public Face face;
    public MemoryCache memoryCache;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final int CAMERA_REQUEST_CODE = 0;
    private final int SELECT_RECIPIENTS_CODE = 1;
    private final int ADD_FRIEND_CODE = 2;
    private final int SETTINGS_CODE = 3;
    private File m_curr_photo_file;

    private boolean netThreadShouldStop = true;

    Producer m_producer;
    ArrayList<Consumer> m_consumers = new ArrayList<Consumer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        System.out.println("Testing startup");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        boolean faceExists = (Globals.face == null);
        Log.d("onCreate", "Globals face is null?: " + faceExists +
                "; Globals security is setup: " + Globals.has_setup_security);
        // need to check if we have an existing face or if security is not setup; either way, we
        // need to make changes; see setup_security()
        if (faceExists || !Globals.has_setup_security) {
            setup_security();
        }

        face = Globals.face;
        memoryCache = Globals.memoryCache;
        keyChain = Globals.keyChain;

        startNetworkThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            register_with_NFD(Globals.getDefaultIdName());
        } catch (Exception e){
            e.printStackTrace();
        }
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
    }

    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, getString(R.string.app_name));
        Toolbar toolbar = toolbarHelper.setupToolbar();
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
                startActivityForResult(intent, SETTINGS_CODE);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function sets up identity storage, keys, and the face our app will use.
     */
    public void setup_security() {
        Log.d("setup_security", "Setting up security");
        FileManager manager = new FileManager(getApplicationContext());
        // /npChat/<username>
        Name appAndUsername = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());

        Context context = getApplicationContext();
        String rootPath = getApplicationContext().getFilesDir().toString();
        String pibPath = "pib-sqlite3:" + rootPath;

        face = new Face();
        try {
            m_pib = new AndroidSqlite3Pib(rootPath, "/pib.db");
            Globals.setPib(m_pib);
        }
        catch(PibImpl.Error e) {
            e.printStackTrace();
        }

        // jndn has a typo in its getter
        m_tpm = new TpmBackEndFile(TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
        Globals.setTpmBackEndFile(m_tpm);
        try {
            m_pib.setTpmLocator("tpm-file:" + TpmBackEndFile.getDefaultDirecoryPath(context.getFilesDir()));
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
        PibIdentity pibId;
        PibKey key;

        try {
            // see if the identity exists; if it doesn't, this will throw an error
            pibId = keyChain.getPib().getIdentity(identity);
            key = pibId.getDefaultKey();
            keyChain.setDefaultIdentity(pibId);
            keyChain.setDefaultKey(pibId, key);
            keyChain.getPib().setDefaultIdentity_(identity);
            Globals.setPubKeyName(key.getName());
            Globals.setPublicKey(key.getPublicKey());
            Globals.setDefaultPibId(pibId);
        }
        catch(PibImpl.Error | Pib.Error e) {
            try {
                pibId = keyChain.createIdentityV2(identity);
                key = pibId.getDefaultKey();
                keyChain.setDefaultIdentity(pibId);
                keyChain.setDefaultKey(pibId, key);
                keyChain.getPib().setDefaultIdentity_(identity);
                Globals.setPubKeyName(key.getName());
                Globals.setPublicKey(key.getPublicKey());
                Globals.setDefaultPibId(pibId);
            }
            catch(PibImpl.Error | Pib.Error | TpmBackEnd.Error | Tpm.Error | KeyChain.Error ex) {
                ex.printStackTrace();
            }
        }

        Globals.setDefaultIdName(appAndUsername);

        try {
            defaultCertificateName = keyChain.getDefaultCertificateName();
        }
        catch(SecurityException e) {
            e.printStackTrace();
            defaultCertificateName = new Name("/bogus/certificate/name");
        }

        Globals.setKeyChain(keyChain);
        face.setCommandSigningInfo(keyChain, defaultCertificateName);
        Globals.setFace(face);
        Globals.setMemoryCache(new MemoryCache(face, getApplicationContext()));
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
     * Registers the provided name with NFD. This is intended to occur whenever the app starts up.
     * @param name The provided name should be /npChat/<username>
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
            Log.d("register_with_nfd","Starting registration process.");
            Globals.memoryCache.getmCache().registerPrefix(name,
                new OnRegisterFailed() {
                    @Override
                    public void onRegisterFailed(Name prefix) {
                        Log.d("OnRegisterFailed", "Registration Failure");
                        String msg = "Registration failed for prefix: " + prefix.toUri();
                        runOnUiThread(makeToast(msg));
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getString(R.string.nfd_package));
                        if (launchIntent != null) {
                            Toast.makeText(getApplicationContext(), "Please Start NFD.",
                                    Toast.LENGTH_LONG).show();
                            startActivity(launchIntent);//null pointer check in case package name was not found
                        }
                        finish();
                    }
                },
                new OnRegisterSuccess() {
                    @Override
                    public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                        Log.d("OnRegisterSuccess", "Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                        String msg = "Successfully registered prefix: " + prefix.toUri();
                        runOnUiThread(makeToast(msg));
                    }
                }, onDataInterest
                );
        }
        catch (IOException | SecurityException e) {
            e.printStackTrace();
        }

        registerRouteToAp();

        Log.d("MainActivity", "Creating producer");
        m_producer = new Producer(face, new Name(getString(R.string.app_name)), name, 10000, 10000, keyChain);

        FileManager manager = new FileManager(getApplicationContext());
        for (String friend : manager.getFriendsList()) {
            startConsumer(friend);
        }
    }

    public void startConsumer(String friend) {
        FileManager manager = new FileManager(getApplicationContext());
        Name appAndUsername = new Name("/" + getString(R.string.app_name) + "/" + manager.getUsername());
        Name friendsUserName = new Name(friend);
        Consumer consumer = new Consumer(new Name(getString(R.string.app_name)), appAndUsername, friendsUserName, face, onSyncData);
        m_consumers.add(consumer);
        Log.d("Consumer", "Added consumer for friend for " + friend);
    }

    public void registerRouteToAp() {
        Name prefix = new Name(getString(R.string.app_name));
        int myFace = 0;

        try {
            final List<FaceStatus> faceList = Nfdc.getFaceList(face);
            final List<RibEntry> routeInfo = Nfdc.getRouteList(face);
            boolean wifiDirect = false;
            for (RibEntry r : routeInfo) {
                String name = r.getName().toString();
                if (name.contains("wifidirect")) {
                    Log.d("registerRouteToAp", "Connected via WifiDirect");
                    wifiDirect = true;
                    break;
                }
            }

            if (!wifiDirect) {
                for (FaceStatus f : faceList) {
                    if (f.getRemoteUri().contains("udp4://224")) {
                        Log.d("registerRouteToAp", "Using multicast face: " + f.getRemoteUri());
                        System.out.println("Testing this");
                        myFace = f.getFaceId();

                    }
                }
            }

            if (myFace != 0) {
                try {
                    Nfdc.register(face, myFace, prefix, 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Log.d("onActivityResult", "requestCode: " + requestCode);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.d("onActivityResult", "Got result data");
            // check if we even took a picture
            if (m_curr_photo_file != null && m_curr_photo_file.length() > 0) {
                Log.d("onActivityResult", "We have an actual file");

                FileOutputStream out = null;;
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(m_curr_photo_file.getAbsolutePath());
                    out = new FileOutputStream(m_curr_photo_file);
                    Log.d("bitmapOnActivity", "bitmap is null?: " + (bitmap == null));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.share_photo).setCancelable(false);

                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send them to a new page to select friends to send photo to
                        Intent intent = new Intent(MainActivity.this, SelectRecipientsActivity.class);

                        FileManager manager = new FileManager(getApplicationContext());
                        ArrayList<String> friendsList = manager.getFriendsList();
                        intent.putStringArrayListExtra("friendsList", friendsList);
                        // make this startActivityForResult and catch the list of recipients;
                        intent.putExtra("photo", m_curr_photo_file.toString());
                        m_curr_photo_file = null;
                        //startActivityForResult(intent, SELECT_RECIPIENTS_CODE);
                        startActivityForResult(intent, SELECT_RECIPIENTS_CODE);
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        runOnUiThread(makeToast("Photo was not shared but can be later."));
                        m_curr_photo_file = null;
                    }
                });

                builder.show();
            }
        }
        else if(requestCode == SELECT_RECIPIENTS_CODE) {
            if(resultCode == RESULT_OK) {
                try {
                    final String path = resultData.getStringExtra("photo");
                    final File photo = new File(path);
                    final Uri uri = UriFileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() +
                                    ".UriFileProvider", photo);

                    Thread publishingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
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
                            final FileManager manager = new FileManager(getApplicationContext());
                            final String prefix = manager.addAppPrefix(path);

                            Common.publishData(blob, new Name(prefix));
                            Bitmap bitmap = QRExchange.makeQRCode(prefix);
                            manager.saveFileQR(bitmap, prefix);
                            runOnUiThread(makeToast("Sending photo"));
                        }
                    });
                    publishingThread.run();
                    ArrayList<String> recipients;
                    try {
                        recipients = resultData.getStringArrayListExtra("recipients");
                        final FileManager manager = new FileManager(getApplicationContext());
                        final String prefix = manager.addAppPrefix(path);
                        m_producer.publishName(new Name(prefix), recipients);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
                }
            }
            else {
                runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
            }
        }
        else if (requestCode == ADD_FRIEND_CODE) {
            if(resultCode == RESULT_OK) {
                startConsumer(resultData.getStringExtra("username"));
            }
        }
        else if (requestCode == SETTINGS_CODE) {
            Log.d("onActivityResult", "SETTINGS_CODE hit");
            FileManager manager = new FileManager(getApplicationContext());
            ImageView imageView = findViewById(R.id.toolbar_main_photo);
            File file = manager.getProfilePhoto();
            if(file == null || file.length() == 0) {
                Picasso.get().load(R.drawable.avatar).fit().centerCrop().into(imageView);
            }
            else {
                // needed the MemoryPolicy.NO_CACHE because it was using the cached (last) profile photo
                // and was not showing the new one when we changed it in SettingsActivity
                Picasso.get().load(file).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(imageView);
            }
        }
        else {
            Log.d("onActivityResult", "Unexpected activity requestcode caught");
        }
    }

    private final ReceiveSyncCallback onSyncData = new ReceiveSyncCallback() {
        public void onReceivedSyncData(Name fileName) {
            Log.d("Consumer", "Will fetch file: " + fileName);
            runOnUiThread(makeToast("Fetching: " + fileName));
            fetch_data(new Interest(fileName));
        }
    };

    private void fetch_data(final Interest interest) {
        // /tasks/FetchingTask
        new FetchingTask(this).execute(interest);
    }

    // start activity for add friends
    public void startMakingFriends(View view) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivityForResult(intent, ADD_FRIEND_CODE);
    }

    public void seeRcvdPhotos(View view) {
        Intent intent = new Intent(this, NewContentActivity.class);
        startActivity(intent);
    }

    /**
     * Triggered by button press. This acts as a helper function to first ask for permission to
     * access the camera if we do not have it. If we are granted permission or have permission, we
     * will call startCamera()
     */
    public void startUpCamera(View view) {
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
        m_curr_photo_file = pic;
        final Uri uri = UriFileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() +
                        ".UriFileProvider", pic);
        // intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(pic));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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

    public void startFiles(View view) {
        Intent intent = new Intent(this, FilesActivity.class);
        startActivity(intent);
    }

    /**
     * This is registered with our prefix. Any interest sent with prefix /npChat/<username>
     * will be caught by this callback. We send it to the faceProxy to deal with it.
     */
    private final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Log.d("OnInterestCallback", "Called OnInterestCallback with Interest: " + interest.getName().toUri());
            memoryCache.process(interest);
        }
    };

    @Override
    protected void onDestroy() {
        memoryCache.destroy();
        super.onDestroy();
    }
}
