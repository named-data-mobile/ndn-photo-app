package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class AppNavigator extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_navigator);
        Toolbar toolbar = (Toolbar) findViewById(R.id.appNavToolbar);
        setSupportActionBar(toolbar);
    }

    // start activity for camera

    // start activity for files
    public void startFiles(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    // start activity for add friends
    public void startMakingFriends(View view) {
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivity(intent);
    }
}
