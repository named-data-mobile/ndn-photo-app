package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = new Session(getApplicationContext());
        // session checks sharedPreferences where we store our login boolean variable
        // if we're not logged in, start LoginActivity
        if (!session.getLoginStatus()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        // we are logged in; take us to MainActivity
        else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
}
