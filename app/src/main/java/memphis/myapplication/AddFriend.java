package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class AddFriend extends AppCompatActivity {

    private final int FRIEND_QR_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                // need to check this content to determine if we are scanning file or friend code
                Toast.makeText(this, content, Toast.LENGTH_LONG).show();
                FileManager manager = new FileManager(getApplicationContext());
                if (requestCode == FRIEND_QR_REQUEST_CODE) {
                    // manager.saveFriend(content);
                    //Intent intent = new Intent();
                    //Save friend's information
                } else {
                    Log.d("onScanResult", "Unexpected requestCode: " + requestCode);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
