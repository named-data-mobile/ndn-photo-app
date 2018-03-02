package memphis.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends AppCompatActivity {

    final private int MISSING_ELEMENT = 1;
    final private int NOT_IN_DATABASE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

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

    protected void login(View view, EditText username, EditText password) {
        int attempt = loginAttempt(view, username, password);
        if(attempt == 0) {
            // go to mainpage
        }
        else if(attempt == MISSING_ELEMENT) {
            Toast.makeText(this, "Please fill out form completely", Toast.LENGTH_LONG).show();
        }
        else if (attempt == NOT_IN_DATABASE) {
            Toast.makeText(this, "username and/or password incorrect", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "Something went wrong. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private int loginAttempt(View view, EditText username, EditText password) {
        String[] array = {username.toString(), password.toString()};
        if(!isFormFilled(array)) {
            return MISSING_ELEMENT;
        }
        // if not in database return NOT_IN_DATABASE
        else {
            return 0;
        }
    }

}
