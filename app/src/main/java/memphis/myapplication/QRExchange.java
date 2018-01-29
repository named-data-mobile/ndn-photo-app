package memphis.myapplication;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.encoder.QRCode;

public class QRExchange extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_exchange);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_qr_exchange, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setupQR(View view) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKey pubKey = keyPair.getPublic();
            //byte[] pieces = pubKey.getEncoded();
            //Log.d("QR", "pubKey: " + pubKey.toString());
            //String stringKey = "";
            /*try {
                stringKey = new String(pieces, "UTF-8");
            }
            catch(UnsupportedEncodingException e) {
                Log.d("QR", "bytes to string problem");
            }*/
            String test = pubKey.toString();

            try {
                QRCodeWriter qrWriter = new QRCodeWriter();
                BitMatrix qrMatrix = qrWriter.encode(test, BarcodeFormat.QR_CODE, 400, 400);
                displayQR(view, qrMatrix);
            }
            catch (WriterException e) {
                Log.d("QR", "qrMatrix not encoded");
            }
        }
        catch (NoSuchAlgorithmException e) {
            Log.d("QR", "RSA did not work");
        }
    }

    public void qrPrefix(View view, String prefix) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix qrMatrix = qrWriter.encode(prefix, BarcodeFormat.QR_CODE, 400, 400);
            displayQR(view, qrMatrix);
        }
        catch (WriterException e) {
            Log.d("QR", "qrMatrix not encoded");
        }
    }

    public void getInput(View view) {
        EditText editText = (EditText) findViewById(R.id.editText2);
        String text = editText.getText().toString();
        qrPrefix(view, text);
    }

    public void displayQR(View view, BitMatrix qrMatrix) {
        int bitWidth = 400;
        int bitHeight = 400;
        Bitmap bitmap = Bitmap.createBitmap(bitWidth, bitHeight, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < bitHeight; i++) {
            for (int j = 0; j < bitWidth; j++) {
                bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }
        try {
            PrintHelper phPrinter = new PrintHelper(view.getContext());
            phPrinter.setScaleMode(phPrinter.SCALE_MODE_FIT);
            phPrinter.printBitmap("QRCode", bitmap);
        }
        catch (Exception e){
            Log.d("QR", e.toString());
        }
    }
}
