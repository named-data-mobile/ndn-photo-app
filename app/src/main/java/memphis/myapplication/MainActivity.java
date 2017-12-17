package memphis.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
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
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    Name registered_prefix;

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
    }

    public void fetch_data(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        final Interest interest = new Interest(new Name(message));

        SegmentFetcher.fetch(
                face,
                interest,
                new SegmentFetcher.VerifySegment() {
                    @Override
                    public boolean verifySegment(Data data) {
                        return true;
                    }
                },
                new SegmentFetcher.OnComplete() {
                    @Override
                    public void onComplete(Blob content) {
                        try {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), interest.getName().toString());
                            FileOutputStream os = new FileOutputStream(file);
                            os.write(content.getImmutableArray());
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("File fetch complete").setMessage("Successfully fetched: " + interest.getName().toString()).show();
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                        builder.setTitle("File fetch failed!").setMessage("Failed to fetch data: "
                                + interest.getName().toString() + "!\nReason: " + message
                                + "\nErrorcode: " + errorCode.toString());
                    }
                });
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

    public void show_dialog(Name prefix, boolean didFail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("Prefix Registration failed? " + didFail).setMessage(prefix.toString()).show();

    }

    public void register_with_NFD(View view) throws IOException, PibImpl.Error {
        EditText editText = (EditText) findViewById(R.id.editText);
        String msg = editText.getText().toString();
        final Name name = new Name(msg);

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
                            registered_prefix = name;
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

    public void test_find_file(View view) {
        Name prefix = new Name("/test/prefix/somefile");
        Interest interest = new Interest(prefix);
        Uri uri = find_file(prefix, interest);
        boolean isNull = uri == null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("interest and name").setMessage("Interest: "
                + interest.getName() + "\nName: " + prefix.toUri()
                + "\nnull uri: " + isNull).show();
    }

    private Uri find_file(Name prefix, Interest interest) {
        Name file_name = interest.getName().getSubName(registered_prefix.size()-1);
        for (Uri uri : filesList) {
            // Get everything after the prefix we registered in NFD, accounting for 0-indexing
            if (getFileName(uri).contentEquals(file_name.toString())) {
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

    /* This is where the file picker intent activity ends up.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Uri uri = null;
        if (resultData != null) {
            final ListView lv = (ListView) findViewById(R.id.listview);
            uri = resultData.getData();
            filesList.add(uri);
            filesStrings.add(getFileName(uri));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesStrings);
            lv.setAdapter(adapter);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("You selected a file").setMessage(getFileName(uri)).show();
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
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
