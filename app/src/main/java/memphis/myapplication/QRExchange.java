package memphis.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
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

import static android.support.v4.print.PrintHelper.SCALE_MODE_FIT;

public class QRExchange extends AppCompatActivity {

    private final int BIT_HEIGHT = 400;
    private final int BIT_WIDTH = 400;

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

    // right now we have a single chain of methods to display the QR upon clicking the button
    // "QR ME", but we are going to need to separate methods and rename some things so I can
    // create specific types of QR codes (key pairs vs images/files)
    // also, registering a user will have their username and their public key

    public KeyPair generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyGen.generateKeyPair();
            try {
                // save keys to storage; need to develop a filesystem
                // we'll generate the QR code for public key on demand
            }
            // Be more specific about the type of Exception. Should be storage related.
            catch (Exception e) {
                Log.d("Internal Storage", "Keys not saved. Err: " + e.toString());
                return null;
            }
            return keyPair;
        }
        catch (NoSuchAlgorithmException e) {
            Log.d("QR", "RSA algorithm not found. Keys were not generated.");
            return null;
        }
    }

    ////////This is not working as it should. The problem is the context is null, so it is dying
    ////////somewhere.
    public void displayMyQR(View view) {
        FileManager manager = new FileManager(view);
        String imgPath = manager.getYourself();
        try {
            ImageView imgView = new ImageView(this);
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
            Log.d("displayMyQR", bitmap.toString());
            //imgView.setImageBitmap(bitmap);
        }
        catch(Exception e) {
            Log.d("displayMyQR", e.toString());
            Toast.makeText(this, "displayMyQR " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    // currently a duplicate of content in makeQRFriendCode, but we may need multiple QR generation
    // processes (file names), so I'm just leaving this here.
    private Bitmap makeQRCode(String qrContents){
        QRCodeWriter qrWriter = new QRCodeWriter();
        try {
            BitMatrix qrMatrix = qrWriter.encode(qrContents, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
            Bitmap bitmap = Bitmap.createBitmap(BIT_WIDTH, BIT_HEIGHT, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < BIT_HEIGHT; i++) {
                for (int j = 0; j < BIT_WIDTH; j++) {
                    bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        }
        catch(WriterException we) {
            Log.d("makeQrFriendCode", "qrWriter failed");
        }
        catch(Exception e) {
            Log.d("makeQrFriendCode", "bitmap was not created");
        }
        return null;
    }

    /**
     *
     */
    public Bitmap makeQRFriendCode(View view) {
        FileManager manager = new FileManager(view);
        String name = manager.getUsername();
        String pubKey = manager.getPubKey();
        // make sure we check later during registration that a username has no spaces
        String qrContents = name + " " + pubKey;
        QRCodeWriter qrWriter = new QRCodeWriter();
        try {
            BitMatrix qrMatrix = qrWriter.encode(qrContents, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
            Bitmap bitmap = Bitmap.createBitmap(BIT_WIDTH, BIT_HEIGHT, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < BIT_HEIGHT; i++) {
                for (int j = 0; j < BIT_WIDTH; j++) {
                    bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        }
        catch(WriterException we) {
            Log.d("makeQrFriendCode", "qrWriter failed");
        }
        catch(Exception e) {
            Log.d("makeQrFriendCode", "bitmap was not created");
        }
        // if it failed to make the bitmap
        return null;
    }

    /* // This might not be necessary. It depends on implementation. We may be passing completely
    // different parameters depending on the file activity we do.
    public Bitmap makeQRFileCode(View view, String path) {
        return makeQRCode(qrContents);
    }*/
    // setup function should take a string (username && pubKey, filename) and create its QR code
    public void setupQR(View view) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKey pubKey = keyPair.getPublic();
            String test = pubKey.toString();

            try {
                QRCodeWriter qrWriter = new QRCodeWriter();
                BitMatrix qrMatrix = qrWriter.encode(test, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
                displayQR(view, qrMatrix);
            }
            catch (WriterException we) {
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
            BitMatrix qrMatrix = qrWriter.encode(prefix, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
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
        Bitmap bitmap = Bitmap.createBitmap(BIT_WIDTH, BIT_HEIGHT, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < BIT_HEIGHT; i++) {
            for (int j = 0; j < BIT_WIDTH; j++) {
                bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }
        try {
            PrintHelper phPrinter = new PrintHelper(view.getContext());
            phPrinter.setScaleMode(SCALE_MODE_FIT);
            phPrinter.printBitmap("QRCode", bitmap);
        }
        catch (Exception e){
            Log.d("QR", e.toString());
        }
    }
}
