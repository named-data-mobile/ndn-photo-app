package memphis.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.pib.PibImpl;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MemoryIdentityStorage identityStorage;
    MemoryPrivateKeyStorage privateKeyStorage;
    IdentityManager identityManager;
    KeyChain keyChain;
    protected Face face;

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
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        // Do something in response to button
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
                            show_dialog(prefix, false);
                        }
                    },
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            show_dialog(prefix, true);
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }



}
