package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session session = new Session(getApplicationContext());
        if (!session.getLoginStatus()) {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
        }
        // not working as expected. Login finishes but IntroActivity does not call onCreate again
        else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
}
