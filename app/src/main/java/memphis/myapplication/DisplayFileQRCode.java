package memphis.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

public class DisplayFileQRCode extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_qr);
        Uri uri = getIntent().getData();
        ImageView image = findViewById(R.id.QRImage);
        image.setImageURI(uri);
    }

}
