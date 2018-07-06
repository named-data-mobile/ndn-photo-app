package memphis.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * This activity displays an image provided by the intent (intended for QR codes). This is its
 * only purpose.
 */

public class DisplayQRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_qr);
        Uri uri = getIntent().getData();
        ImageView image = findViewById(R.id.QRImage);
        image.setImageURI(uri);
    }

}
