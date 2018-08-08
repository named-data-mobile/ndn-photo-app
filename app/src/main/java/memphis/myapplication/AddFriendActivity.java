package memphis.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.ArrayList;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class AddFriendActivity extends AppCompatActivity {

    private final int FRIEND_QR_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
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
            manager.saveMyQRCode(QRExchange.makeQRFriendCode(manager));
        }
        Intent display = new Intent(this, DisplayQRActivity.class);
        display.setData(Uri.fromFile(file));
        startActivity(display);
    }

    public void viewFriendsList(View view) {
        FileManager manager = new FileManager(getApplicationContext());
        ArrayList<String> friendsList = manager.getFriendsList();
        Intent intent = new Intent(this, ViewFriendsActivity.class);
        intent.putStringArrayListExtra("friendsList", friendsList);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FRIEND_QR_REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
            if (result == null) {
                Toast.makeText(this, "Null", Toast.LENGTH_LONG).show();
            }
            if (result != null) {
                // check resultCode to determine what type of code we're scanning, file or friend

                if (result.getContents() == null) {
                    Toast.makeText(this, "Nothing is here", Toast.LENGTH_LONG).show();
                } else {
                    String content = result.getContents();
                    Log.d("ScannedFriend", content);
                    // need to check this content to determine if we are scanning file or friend code
                    Toast.makeText(this, content, Toast.LENGTH_LONG).show();
                    FileManager manager = new FileManager(getApplicationContext());
                    int saveResult = manager.saveFriend(content);
                    if (saveResult == 0) {
                        Toast.makeText(this, "Friend was saved successfully.", Toast.LENGTH_LONG).show();
                    }
                    else if (saveResult == 1) {
                        Toast.makeText(this, "You are already friends.", Toast.LENGTH_LONG).show();

                    }
                    else {
                        Toast.makeText(this, "Error saving friend.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
