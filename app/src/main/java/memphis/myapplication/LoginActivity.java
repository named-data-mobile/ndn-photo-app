package memphis.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LoginActivity extends AppCompatActivity {

    final private int MISSING_ELEMENT = 1;
    // final private int NOT_IN_DATABASE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setBackgroundImage();
    }

    private void setBackgroundImage() {
        try {
            ImageView backImg = findViewById(R.id.backImg);
            Picasso.get().load(R.drawable.hotel).rotate(90f).fit().centerCrop().into(backImg);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // we are heading towards a decentralized network (no database), so this is where the user will
    // ask for the user manifest (to be added) to see if their username is unique
    private static void onSignUp() {
        // redirect to a new page
    }

    // Error checking; see comment block below as well
    private boolean isFormFilled(String[] args) {
        if(args.length == 2) {
            return true;
        }
        return false;
    }

    // what needs to happen for login:
    //// we need to query a DB for the username and password
    ////// if true: redirect to mainpage
    ////// if false: print message stating username and/or password is incorrect

    public void login(View view) {
        EditText name = (EditText) findViewById(R.id.username_text);
        EditText pass = (EditText) findViewById(R.id.password_text);
        String username = name.getText().toString();
        String password = pass.getText().toString();
        int attempt = loginAttempt(username, password);
        if(attempt == 0) {
            // save username and go to mainpage
            FileManager manager = new FileManager(getApplicationContext());
            manager.saveUsername(username);
            Session session = new Session(getApplicationContext());
            session.setLoginStatus();
            // uncomment this later; in manifest, add no history option for LoginActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            // finish();
        }
        else if(attempt == MISSING_ELEMENT) {
            Toast.makeText(this, "Please fill out form completely", Toast.LENGTH_LONG).show();
        }
        /*else if (attempt == NOT_IN_DATABASE) {
            Toast.makeText(this, "username and/or password incorrect", Toast.LENGTH_LONG).show();
        }*/
        else {
            Toast.makeText(this, "Something went wrong. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private int loginAttempt(String username, String password) {
        String[] array = {username, password};
        if(username.isEmpty() || password.isEmpty()) {
            return MISSING_ELEMENT;
        }
        // if not in database return NOT_IN_DATABASE
        else {
            return 0;
        }
    }

}
