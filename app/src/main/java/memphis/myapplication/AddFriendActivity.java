package memphis.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.DisplayMetrics;
import timber.log.Timber;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.SigningInfo;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class AddFriendActivity extends AppCompatActivity {

    private final int FRIEND_QR_REQUEST_CODE = 0;
    private final int ADD_FRIEND_QR_REQUEST_CODE = 1;

    private FileManager m_manager;
    private ToolbarHelper toolbarHelper;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        m_manager = new FileManager(getApplicationContext());
        setupToolbar();
        setButtonWidth();
    }

    private void setupToolbar() {
        toolbarHelper = new ToolbarHelper(this, getString(R.string.friends_title));
        toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }




    private void setButtonWidth() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int width = metrics.widthPixels/3;
        Button btn1 = findViewById(R.id.showYourCode);
        btn1.setWidth(width);
        Button btn2 = findViewById(R.id.scanFriendButton);
        btn2.setWidth(width);
        Button btn3 = findViewById(R.id.viewFriendsButton);
        btn3.setWidth(width);
    }

        public void addFriend(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AddFriendActivity.this);
        builder.setTitle("Select whether you want to display your QR code or scan your friend's code").setCancelable(false);
        final View tempView = view;

        builder.setPositiveButton("Display QR code", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Send them to a new page to select friends to send photo to
                scanFriendQR(tempView);
                displayMyQR(tempView);

            }
        });
        builder.setNegativeButton("Scan QR code", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                displayMyQR(tempView);
                scanFriendQR(tempView);

            }
        });

        builder.show();
    }


    // To do: add new Intent for the remote friend button; this new activity should allow the user
    // to search for usernames and befriend them (send friendship interest)

    public void scanFriendQR(View view) {
        IntentIntegrator scanner = new IntentIntegrator(this);
        // only want QR code scanner
        scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
        scanner.setOrientationLocked(true);
        // back facing camera id
        scanner.setCameraId(0);
        Intent intent = scanner.createScanIntent();
        startActivityForResult(intent, FRIEND_QR_REQUEST_CODE);
    }

    public void displayMyQR(View view) {
        // need to retrieve our QR file, and if it does not exist, create one first.
        FileManager manager = new FileManager(getApplicationContext());
        File file = new File(manager.getMyQRPath());
        if(!file.exists()) {
            manager.saveMyQRCode(QRExchange.makeQRFriendCode(AddFriendActivity.this, manager));
        }
        Intent display = new Intent(this, DisplayQRActivity.class);
        display.setData(Uri.fromFile(file));
        startActivity(display);
    }

    public void viewFriendsList(View view) {
        ArrayList<String> friendsList = SharedPrefsManager.getInstance(this).getFriendsList();
        Intent intent = new Intent(this, ViewFriendsActivity.class);
        intent.putStringArrayListExtra("friendsList", friendsList);
        startActivity(intent);
    }

    // save friends
    public int saveFriend(String friendContent) {
        Timber.d("Saving: %s", friendContent);
        if (friendContent.length() > 0) {
            int index = friendContent.indexOf(" ");
            String username = friendContent.substring(0, index);

            // A friend's filename will be their username. Another reason why we must ensure uniqueness
            if (!SharedPrefsManager.getInstance(this).addFriend(username)) {
                return 1;
            }

            // Save friend's certificate
            String cert = friendContent.substring(index + 1);

            byte[] certBytes = Base64.decode(cert, 0);
            CertificateV2 certificateV2 = null;
            try {
                SigningInfo signingInfo = new SigningInfo();
                signingInfo.setSigningKeyName(Globals.pubKeyName);


                certificateV2 = new CertificateV2();
                certificateV2.wireDecode(ByteBuffer.wrap(certBytes));

                Name certName = certificateV2.getName();
                Name newCertName = new Name();
                newCertName.append(certName.getSubName(0, 4));
                newCertName.append(SharedPrefsManager.getInstance(this).getUsername());
                newCertName.append(certName.getSubName(5));
                Globals.keyChain.sign(certificateV2, signingInfo);
                certificateV2.setName(newCertName);


            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (PibImpl.Error error) {
                error.printStackTrace();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            } catch (KeyChain.Error error) {
                error.printStackTrace();
            }


            SharedPrefsManager.getInstance(this).storeFriendCert(username, certificateV2);
            Timber.d("AddFriendActivity: %s", certificateV2.getKeyName().toString());
            return 0;
        }
        return -1;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == FRIEND_QR_REQUEST_CODE) || (requestCode == ADD_FRIEND_QR_REQUEST_CODE)) {
            IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
            if (result == null) {
                Toast.makeText(this, "Null", Toast.LENGTH_LONG).show();
            }
            if (result != null) {
                // check resultCode to determine what type of code we're scanning, file or friend

                if (result.getContents() != null) {
                    String content = result.getContents();
                    Timber.d("ScannedFriend: %s", content);
                    // need to check this content to determine if we are scanning file or friend code
                    // Toast.makeText(this, content, Toast.LENGTH_LONG).show();
                    int saveResult = saveFriend(content);
                    if (saveResult == 0) {
                        Toast.makeText(this, "Friend was saved successfully.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent();
                        if (content.length() > 0) {
                            int index = content.indexOf(" ");
                            String username = content.substring(0, index);
                            intent.putExtra("username", username);
                            System.out.println("Friend result ok");
                            setResult(RESULT_OK, intent);

                        }
                        else {
                            setResult(RESULT_CANCELED, intent);
                        }
                    }
                    else if (saveResult == 1) {
                        Toast.makeText(this, "You are already friends.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED, data);
                    }
                    else {
                        Toast.makeText(this, "Error saving friend.", Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED, data);
                    }
                }
            } else if (resultCode == 99) {
                System.out.println("Scanned");
                scanFriendQR(findViewById(android.R.id.content));

            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
