package memphis.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
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

    MemoryIdentityStorage identityStorage;
    MemoryPrivateKeyStorage privateKeyStorage;
    IdentityManager identityManager;
    KeyChain keyChain;
    public Face face;
    SegmentFetcher fetcher;
    List<String> filesStrings = new ArrayList<String>();
    List<Uri> filesList = new ArrayList<Uri>();

    private boolean has_setup_security = false;
    public void setup_security() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                face = new Face();
                identityStorage = new MemoryIdentityStorage();
                privateKeyStorage = new MemoryPrivateKeyStorage();
                identityManager = new IdentityManager(identityStorage, privateKeyStorage);
                keyChain = new KeyChain(identityManager);
                keyChain.setFace(face);

                // NOTE: This is based on apps-NDN-Whiteboard/helpers/Utils.buildTestKeyChain()...
                Name testIdName = new Name("/test/identity");
                Name defaultCertificateName;
                try {
                    defaultCertificateName = keyChain.createIdentityAndCertificate(testIdName);
                    keyChain.getIdentityManager().setDefaultIdentity(testIdName);
                } catch (SecurityException e2) {
                    defaultCertificateName = new Name("/bogus/certificate/name");
                }
                face.setCommandSigningInfo(keyChain, defaultCertificateName);
                has_setup_security = true;
                try {
                    face.processEvents();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (EncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.run();
    }

    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.filesList = new ArrayList<Uri>();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    /** Called when the user taps the Send button */
    public void fetch_data(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        Interest interest = new Interest(new Name(message));

        SegmentFetcher.fetch(
                face,
                interest,
                new SegmentFetcher.VerifySegment() {
                    @Override
                    public boolean verifySegment(Data data) {
                        /* TODO: Implement this! */
                        return true;
                    }
                },
                new SegmentFetcher.OnComplete() {
                    @Override
                    public void onComplete(Blob content) {
                        /* TODO: Implement this! */
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        /* TODO: Implement this! */
                    }
                });
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
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

    public void register_with_NFD(View view) throws IOException, PibImpl.Error {
        EditText editText = (EditText) findViewById(R.id.editText);
        String msg = editText.getText().toString();
        Name name = new Name(msg);

        if (!has_setup_security) {
            setup_security();
            while (!has_setup_security)
                try {
                    wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        try {
            long prefixId = face.registerPrefix(name,
                    new OnInterestCallback() {
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
                    },
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            show_dialog(prefix, true);
                        }
                    },
                    new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            CharSequence text = "Successfully registered prefix: " + prefix.toString();
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(MainActivity.this, text, duration);
                            toast.show();
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void publishData(Blob blob, Name prefix) {
        try {
            for (Data data : packetize(blob, prefix)) {
                keyChain.sign(data);
                face.putData(data);
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
                Uri uri = filesList.get(pos);
                byte[] bytes;
                try {
                    InputStream is = MainActivity.this.getContentResolver().openInputStream(uri);
                    bytes = IOUtils.toByteArray(is);
                } catch (IOException e) {
                    e.printStackTrace();
                    bytes = new byte[0];
                }
                test_packetize(bytes);
                AlertDialog.Builder builder = new AlertDialog.Builder(lv.getContext(), android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("rst").setMessage(Arrays.toString(bytes)).show();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);

        lv.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        Uri uri = null;
        if (resultData != null) {
            final ListView lv = (ListView) findViewById(R.id.listview);

            uri = resultData.getData();
            filesList.add(uri);
            filesStrings.add(uri.toString());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);
            lv.setAdapter(adapter);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("You selected a file").setMessage(uri.toString()).show();
        }
    }

    public Data[] packetize(Blob raw_blob, Name prefix) {
        final int VERSION_NUMBER = 0;
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
            segment_name.appendSegment(0);
            data.setName(segment_name);
            try {
                raw_blob.buf().get(segment_buffer, 0 , PACKET_SIZE);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            data.setContent(new Blob(segment_buffer));
            MetaInfo meta_info = new MetaInfo();
            meta_info.setFreshnessPeriod(1000);
            segment_number++;
            offset += 1401; // Add another to start from
            if (offset > raw_blob.size()) {
                // Set the final component to have a final block id.
                meta_info.setFinalBlockId(data.getName().get(-1));
                data.setMetaInfo(meta_info);
            }
            datas.add(data);

        } while (offset < raw_blob.size());
        return datas.toArray(new Data[datas.size()]);
    }

}
