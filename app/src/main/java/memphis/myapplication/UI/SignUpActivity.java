package memphis.myapplication.UI;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SignUpActivity extends AppCompatActivity {

/*    // Error return values to indicate what the user needs to correct in their signup form
    // some form elements are missing content (e.g., no email address provided)
    final private int MISSING_ELEMENT = 1;
    // the password and the confirm password content are not equal
    final private int PASSWORD_CONFLICT = 2;
    // this email address has already been registered
    final private int EMAIL_TAKEN = 3;
    // this username has already been registered
    final private int USERNAME_TAKEN = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    protected boolean isFormFilled(String[] args) {
        if (args.length == 4) {
            return true;
        }
        return false;
    }

    protected boolean isSamePassword(String password, String conPassword) {
        return password.equals(conPassword);
    }

    // We'll need to check the DB if the user's email and/or username already exist. But for now,
    // let's assume the credentials are valid.

    // removed string parameters for now (just doing easy signup)
    protected int setUpAccount(View view) {
        // Requires the DB functions I mentioned in the above comment
        // if email is already registered return EMAIL_TAKEN
        // if username is already taken return USERNAME_TAKEN
*/
        /*if(!isFormFilled(args)) {
            return MISSING_ELEMENT;
        }
        if(!isSamePassword(args[2], args[3])) {
            return PASSWORD_CONFLICT;
        }*/

        // add user to DB
        // now generate and save key pairs
        /*Context currContext = view.getContext();
        int permissionsCheck = ContextCompat.checkSelfPermission(currContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionsCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) currContext,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
            permissionsCheck = ContextCompat.checkSelfPermission(currContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionsCheck == PackageManager.PERMISSION_GRANTED){
            Timber.d("Permission granted");
            Timber.d("currContext: " + currContext);
            Timber.d("view.getContext(): " + view.getContext());
            QRExchange generator = new QRExchange();
            Context appContext = getApplicationContext();
            FileManager manager = new FileManager(appContext);
            boolean areKeysSaved = manager.saveKeys();
            if (areKeysSaved) {
                Bitmap myQRCode = generator.makeQRFriendCode(appContext);
                // save QR code of your information
                manager.saveMyQRCode(myQRCode);
                generator.displayMyQR(view);
            } else {
                Timber.d(""Keys not saved");
                Toast.makeText(this, "Keys not saved", Toast.LENGTH_LONG).show();
            }
            return 0;
        }
        Timber.d("Permission denied");
        return -1;
    }

    protected void onFormSubmission(View view) {
        int attempt = setUpAccount(view);
        if (attempt == 0) {
            Toast.makeText(this, "signup complete", Toast.LENGTH_LONG).show();
        }
    }*/

    /*@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case 0:
                if(grantResults.length > 0) {
                    QRExchange generator = new QRExchange();
                    FileManager manager = new FileManager(view);
                    boolean areKeysSaved = manager.saveKeys();
                    if (areKeysSaved) {
                        Bitmap myQRCode = generator.makeQRFriendCode(view);
                        // save QR code of your information
                        manager.saveYourself(myQRCode);
                        generator.displayMyQR(view);
                    } else {
                        Toast.makeText(this, "Keys not saved", Toast.LENGTH_LONG).show();
                    }
                }
        }
    }*/
}