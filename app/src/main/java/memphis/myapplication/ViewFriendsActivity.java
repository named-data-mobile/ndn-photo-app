package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewFriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_view_friends_list);
        Intent intent = getIntent();
        int count = 0;
        LinearLayout linearLayout = findViewById(R.id.friendLinearLayout);
        for(String friend : intent.getStringArrayListExtra("friendsList")) {
            TextView friendName = new TextView(this);
            friendName.setText(friend);
            linearLayout.addView(friendName);
            count++;
        }
        if(count == 0) {
            TextView message = new TextView(this);
            message.setText("You currently haven't added any friends.");
            linearLayout.addView(message);
        }
    }

    public void removeFriend(View view) {
        FileManager manager = new FileManager(getApplicationContext());

    }
}
