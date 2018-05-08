package memphis.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.VisibleForTesting;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.encoding.ElementListener;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.transport.TcpTransport;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MainActivity mainActivity = this;
    public static Context mContext;
    String retrieved_data = "";
    MemoryIdentityStorage identityStorage;
    MemoryPrivateKeyStorage privateKeyStorage;
    IdentityManager identityManager;
    KeyChain keyChain;
    public Face face;
    public Face face2;
    public FaceProxy faceProxy;
    List<String> filesStrings = new ArrayList<String>();
    List<Uri> filesList = new ArrayList<Uri>();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // maybe add a thread that runs and constantly calls face.processEvents; Ashlesh said that if
    // we have 2 faces, they could be blocking one another if they are on the same thread. So, if
    // we keep the two faces, they'll need their own threads.

    private boolean appThreadShouldStop = true;
    private boolean has_setup_security = false;
    public void setup_security() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                face = new Face();
                face2 = new Face();
                faceProxy = new FaceProxy();
                Face[] faces = {face, face2};
                for(int i = 0; i < faces.length; i++) {
                    // look at File equivalents to Memory in jndn; That should accomplish your basic
                    // idea while using Nick's use of jndn
                    // come back to this when we want a perm solution; will need to integrate SQLite3, but will solve storage questions
                    identityStorage = new MemoryIdentityStorage();
                    privateKeyStorage = new MemoryPrivateKeyStorage();
                    identityManager = new IdentityManager(identityStorage, privateKeyStorage);
                    keyChain = new KeyChain(identityManager);
                    keyChain.setFace(faces[i]);

                    // NOTE: This is based on apps-NDN-Whiteboard/helpers/Utils.buildTestKeyChain()...
                    Name testIdName = new Name("/test/identity");
                    Name defaultCertificateName;
                    try {
                        defaultCertificateName = keyChain.createIdentityAndCertificate(testIdName);
                        keyChain.getIdentityManager().setDefaultIdentity(testIdName);
                        Log.d("setup_security", "Certificate was generated.");

                    } catch (SecurityException e2) {
                        defaultCertificateName = new Name("/bogus/certificate/name");
                    }
                    faces[i].setCommandSigningInfo(keyChain, defaultCertificateName);
                    has_setup_security = true;
                    Log.d("setup_security", "Security was setup successfully");
                    try {
                        faces[i].processEvents();
                    } catch (IOException | EncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.run();
    }

    /*private final Thread appThread = new Thread(new Runnable() {
        @Override
        public void run() {
            if(!has_setup_security) {
                setup_security();
            }
            while(!appThreadShouldStop) {
                try {
                    face.processEvents();
                    appThread.sleep(100);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private void startAppThread() {
        if (!appThread.isAlive()) {
            appThreadShouldStop = false;
            appThread.start();
        }
    }

    private void stopNetworkThread() {
        appThreadShouldStop = true;
    }

    protected boolean networkThreadIsRunning() {
        return appThread.isAlive();
    }*/

    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        // this is not available when we are signing up, we need a universal main function to set this
        // context so I can use it elsewhere
        mContext = getApplicationContext();
        // check if logged in; if not, go to Login Page. From there you can access the signup page.
        // this is where we should check that we have all of our security setup maybe? If not, we
        // need to generate things again.
    }

    /** Called when the user taps the Send button */
    public void fetch_data(View view) {
        Log.d("fetch_data", "Called fetch_data");
        // Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        Log.d("fetch_data", "Message from editText: " + message);
        Interest interest = new Interest(new Name(message));
        Log.d("fetch_data", "Interest: " + interest.getName().toString());
        // interest.setInterestLifetimeMilliseconds(20000);

        final boolean[] enabled = new boolean[] { true };
        SegmentFetcher.fetch(
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
                        //!!!!!!!!! TEMPORARY !!!!!!!!!!!!
                        enabled[0] = false;
                        Log.d("fetch_data onComplete", "we got content");
                        retrieved_data = new String(content.getImmutableArray());
                        Log.d("fetch_data onComplete", "ShortContent: " + retrieved_data);
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        enabled[0] = false;
                        Log.d("fetch_data onError", message);
                    }
                });

        try {
            while (enabled[0]) {
                face.processEvents();
                // We need to sleep for a few milliseconds so we don't use 100% of
                //   the CPU.
                try {
                    Thread.sleep(5);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch(IOException | EncodingException e) {
            e.printStackTrace();
        }
        /*intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);*/
        // Do something in response to button
    }

    public void test_packetize(byte[] bytes) {
        Data[] datas = packetize(new Blob(bytes), new Name("/dummy/name"));
        List<Byte> reconstructed_file = new ArrayList<Byte>();
        for (Data data : datas) {
            for (byte data_byte : data.getContent().getImmutableArray()) {
                reconstructed_file.add(data_byte);
            }
        }
        byte[] file_bytes = new byte[reconstructed_file.size()];
        for (int i = 0; i < reconstructed_file.size(); i++) {
            file_bytes[i] = reconstructed_file.get(i);
        }
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "testfile");
            file.setWritable(true);
            FileOutputStream os = new FileOutputStream(file);
            os.write(file_bytes);
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void switchActivity(View view) {
        Intent intent = new Intent(this, FileSelectActivity.class);
        startActivity(intent);
    }

    public void show_dialog(Name prefix, boolean didFail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Prefix Registration failed? " + didFail).setMessage(prefix.toString()).show();

    }

    public void register_with_NFD(View view) {
        EditText editText = findViewById(R.id.editText);
        String msg = editText.getText().toString();
        try {
            Name name = new Name(msg);
            register_with_NFD(name);
        }
        catch(IOException | PibImpl.Error e) {
            e.printStackTrace();
        }
    }

    public void register_with_NFD(Name name) throws IOException, PibImpl.Error {

        if (!has_setup_security) {
            setup_security();
            while (!has_setup_security)
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        final boolean[] enabled = new boolean[] { true };
        try {
            Log.d("register_with_nfd", "Starting registration process.");
            long prefixId = face2.registerPrefix(name,
                    onDataInterest,
                    /*new OnInterestCallback() {
                        @Override
                        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                            Uri uri = find_file(prefix, interest);
                            if (uri != null) {
                                byte[] bytes;
                                try {
                                    bytes = IOUtils.toByteArray(MainActivity.this.getContentResolver().openInputStream(uri));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    bytes = new byte[0];
                                }
                                Blob blob = new Blob(bytes, true);
                                publishData(blob, prefix);
                            }
                            find_file(prefix, interest);
                        }
                    },*/
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            enabled[0] = false;
                            Log.d("OnRegisterFailed", "Registration Failure");
                            show_dialog(prefix, true);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            enabled[0] = false;
                            Log.d("OnRegisterSuccess", "Registration Success for prefix: " + prefix.toUri() + ", id: " + registeredPrefixId);
                            CharSequence text = "Successfully registered prefix: " + prefix.toString();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(MainActivity.this, text, duration);
                            toast.show();
                        }
                    });
            try {
                while (enabled[0]) {
                    face2.processEvents();
                    try {
                        Thread.sleep(5);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(IOException | EncodingException e) {
                e.printStackTrace();
            }
        }
        catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public void publishData(Blob blob, Name prefix) {
        try {
            for (Data data : packetize(blob, prefix)) {
                Log.d("publishData", "Publishing with prefix: "+ prefix);
                keyChain.sign(data);
                faceProxy.putInCache(data);
                face2.putData(data);
                try {
                    face2.processEvents();
                    Thread.sleep(10);
                }
                catch(IOException | EncodingException | InterruptedException e) {
                    e.printStackTrace();
                }
                // face2.setInterestFilter(prefix, onDataInterest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PibImpl.Error error) {
            error.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (TpmBackEnd.Error error) {
            error.printStackTrace();
        } catch (KeyChain.Error error) {
            error.printStackTrace();
        }
    }

    private Uri find_file(Name prefix, Interest interest) {
        for (Uri uri : filesList) {
            if (uri.toString().contentEquals(prefix.toUri())) {
                return uri;
            }
        }
        return null;
    }

    public void select_files(View view) {
        final ListView lv = (ListView) findViewById(R.id.listview);
        List<String> filesStrings = new ArrayList<String>();
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)

        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, 0);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                final Uri uri = filesList.get(pos);
                // add symbolic link to this path if not present?
                Log.d("onItemClick", "uri: " + uri);
                byte[] bytes;
                try {
                    InputStream is = MainActivity.this.getContentResolver().openInputStream(uri);
                    bytes = IOUtils.toByteArray(is);
                } catch (IOException e) {
                    Log.d("onItemClick", "failed to byte");
                    e.printStackTrace();
                    bytes = new byte[0];
                }

                final Blob blob = new Blob(bytes, true);
                String s = addFilenamePrefix(uri.toString());
                final Name prefix = new Name(s);
                publishData(blob, prefix);
                try {
                    register_with_NFD(prefix);
                }
                catch (IOException | PibImpl.Error e) {
                    e.printStackTrace();
                }

                QRExchange.makeQRFileCode(view.getContext(), s);

                AlertDialog.Builder builder = new AlertDialog.Builder(lv.getContext(), android.R.style.Theme_Material_Dialog_Alert);
                // Add the buttons
                builder.setPositiveButton(R.string.publish, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        publishData(blob, prefix);
                        Log.d("Publish Button", prefix.toString());
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
                builder.setTitle("rst").setMessage(Arrays.toString(bytes)).show();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);

        lv.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Log.d("onActivityResult", "requestCode: " + requestCode);
        Uri uri = null;
        if (resultData != null) {
            if (requestCode == 0) {
                final ListView lv = (ListView) findViewById(R.id.listview);

                uri = resultData.getData();
                filesList.add(uri);
                filesStrings.add(uri.toString());
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);
                lv.setAdapter(adapter);
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("You selected a file").setMessage(uri.toString()).show();
            }
            // We received a request to display a QR image
            else if (requestCode == 1) {
                try {
                    // set up a new Activity for displaying. This way the back button brings us back
                    // to main activity.
                    Intent display = new Intent(this, DisplayFileQRCode.class);
                    display.setData(resultData.getData());
                    startActivity(display);
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            else {
                Log.d("onActivityResult", "Unexpected activity requestcode caught");
            }
        }
    }

    /**
     * Start a file selection activity to find an QR image to display. This is triggered by pressing
     * the "Display QR" button.
     * @param view The view of MainActivity passed by our button press.
     */
    public void lookup_file_QR(View view) {
        // ACTION_GET_CONTENT is used for reading; no modifications
        // We're going to find a png file of our choosing (should be used for displaying QR codes,
        // but it can display any image)
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // fix this: this displays every file. We should limit the scope to a specific directory and
        // image files, if possible.
        intent.setType("*/*");
        // requestCode: 1 was arbitrary. Our select_file function sends a requestCode: 0 (also arbitrary)
        startActivityForResult(intent, 1);
    }

    public String addFilenamePrefix(String path) {
        // we could also allow the user to state their own name which will attach to the end of
        // /ndn-snapchat/<username>/
        // int index = path.lastIndexOf('/');
        // name = "/ndn-snapchat/<username>" + path.substring(index);
        FileManager manager = new FileManager(getApplicationContext());
        String username = manager.getUsername();
        if (username != null) {
            // check that path already comes with "/" prepended
            if(path.charAt(0) == '/') {
                return "/ndn-snapchat/" + username + path;
            }
            else {
                return "/ndn-snapchat/" + username + "/" + path;
            }
        }
        else {
            return null;
        }
    }

    public Data[] packetize(Blob raw_blob, Name prefix) {
        Name.Component finalBlockId =
                new Name.Component(new Blob(new byte[] { (byte)0 }, false));
        final int VERSION_NUMBER = 1;
        final int DEFAULT_PACKET_SIZE = 1400;
        final int PACKET_SIZE;
        PACKET_SIZE = (DEFAULT_PACKET_SIZE > raw_blob.size()) ? raw_blob.size() : DEFAULT_PACKET_SIZE;
        List<Data> datas = new ArrayList<Data>();
        byte[] segment_buffer = new byte[PACKET_SIZE];
        int segment_number = 0;
        int offset = 0;
        do {
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
                raw_blob.buf().get(segment_buffer, 0, PACKET_SIZE);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            data.setContent(new Blob(segment_buffer));
            MetaInfo meta_info = new MetaInfo();
            meta_info.setType(ContentType.BLOB);
            // not sure what is a good freshness period
            meta_info.setFreshnessPeriod(30000);
            segment_number++;
            offset += 1401; // Add another to start from
            if (offset > raw_blob.size()) {
                // Set the final component to have a final block id.
                meta_info.setFinalBlockId(finalBlockId);
                data.setMetaInfo(meta_info);
            }
            datas.add(data);

        } while (offset < raw_blob.size());
        return datas.toArray(new Data[datas.size()]);
    }

    private final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {

            Name interestName = interest.getName();
            Log.d("OnInterestCallback", "Called OnInterestCallback with Interest: " + interestName.toUri());
            faceProxy.process(interest, mainActivity);
        }
    };

    // maybe we need our own onData callback since it is used in expressInterest (which is called by the SegmentFetcher)
}
