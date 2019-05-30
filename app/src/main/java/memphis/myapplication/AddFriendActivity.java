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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SigningInfo;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;

import java.io.File;
import java.nio.ByteBuffer;


import io.realm.Realm;
import memphis.myapplication.RealmObjects.User;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class AddFriendActivity extends AppCompatActivity {

    private final int FRIEND_QR_REQUEST_CODE = 0;
    private final int ADD_FRIEND_QR_REQUEST_CODE = 1;
    private final int REMOTE_FRIEND_REQUEST_CODE = 2;
    private final int RESULT_ALREADY_TRUST = 5;

    private FileManager m_manager;
    private ToolbarHelper toolbarHelper;
    private Toolbar toolbar;
    private String friendName;
    private String friendDomain;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        m_manager = new FileManager(getApplicationContext());
        setupToolbar();
        setButtonWidth();
        mRealm = Realm.getDefaultInstance();

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
        builder.setTitle("Display or scan QR code first?").setCancelable(false);
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
//        ArrayList<String> friendsList = SharedPrefsManager.getInstance(this).getFriendsList();
        Intent intent = new Intent(this, ViewFriendsActivity.class);
//        intent.putStringArrayListExtra("friendsList", friendsList);
        startActivity(intent);
    }

    // save friends
    public int saveFriend(String friendContent) {
        KeyChain keyChain = Globals.keyChain;
        Log.d("Saving friend", friendContent);
        if (friendContent.length() > 0) {
            try {


                byte[] certBytes = Base64.decode(friendContent, 0);
                CertificateV2 certificateV2 = null;

                certificateV2 = new CertificateV2();
                certificateV2.wireDecode(ByteBuffer.wrap(certBytes));

                // Save self-signed cert for friend
                friendName = certificateV2.getName().getSubName(-5, 1).toUri().substring(1);
                System.out.println("Friend name: " + friendName);
                for (int i = 0; i <= certificateV2.getName().size(); i++) {
                    System.out.println(certificateV2.getName().getSubName(i, 1).toUri());
                    if (certificateV2.getName().getSubName(i, 1).toUri().equals("/npChat")) {
                        friendDomain = certificateV2.getName().getPrefix(i).toUri();
                    }
                }

                mRealm.beginTransaction();
                User friend = mRealm.where(User.class).equalTo("username", friendName).findFirst();
                if (friend == null) {
                    friend = mRealm.createObject(User.class, friendName);
                    friend.setDomain(friendDomain);
                }
                else if (friend.isFriend()) {
                    mRealm.cancelTransaction();
                    mRealm.close();
                    return 1;
                }
                else if (friend.haveTrust()) {
                    mRealm.cancelTransaction();
                    mRealm.close();
                    return 3;
                }
                // A friend's filename will be their username. Another reason why we must ensure uniqueness


                // Change cert name
                Name certName = certificateV2.getName();
                Name newCertName = new Name();
                int signerComp = 0;
                for (int i = 0; i<=certName.size(); i++) {
                    if (certName.getSubName(i, 1).toUri().equals("/KEY")) {
                        signerComp = i+2;
                        break;
                    }
                }
                newCertName.append(certName.getSubName(0, signerComp));
                newCertName.append(SharedPrefsManager.getInstance(this).getUsername());
                newCertName.append(certName.getSubName(signerComp+1));
                certificateV2.setName(newCertName);

                //Sign cert with our key
                Globals.keyChain.sign(certificateV2, new SigningInfo(SigningInfo.SignerType.KEY, Globals.pubKeyName));
                Log.d("Friend's cert", certificateV2.toString());


                // Store friend's cert signed by us
//            SharedPrefsManager.getInstance(this).storeFriendCert(friendName, certificateV2);
                friend.setCert(certificateV2);
                mRealm.commitTransaction();
            } catch (PibImpl.Error error) {
                error.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (KeyChain.Error error) {
                error.printStackTrace();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            }
            return 0;
        }
        return -1;
    }

    public void sendFriendRequest(View view) {
        Intent intent = new Intent(this, SendFriendRequest.class);
        startActivityForResult(intent, REMOTE_FRIEND_REQUEST_CODE);


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
                    Log.d("ScannedFriend", content);
                    // need to check this content to determine if we are scanning file or friend code
                    // Toast.makeText(this, content, Toast.LENGTH_LONG).show();
                    int saveResult = saveFriend(content);
                    if (saveResult == 0) {
                        Toast.makeText(this, "Retrieving certificate.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent();
                        if (content.length() > 0) {
                            String username = friendName;
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
                    } else if (saveResult == 3) {
                        Toast.makeText(this, "Already have certificate.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent();
                        if (content.length() > 0) {
                            String username = friendName;
                            intent.putExtra("username", username);
                            System.out.println("Friend result ok");
                            setResult(RESULT_ALREADY_TRUST, intent);
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
}
