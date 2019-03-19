package memphis.myapplication;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ViewFriendsActivity extends AppCompatActivity {

    // add border around each friend; increase font size; change colors
    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_with_list);
        Intent intent = getIntent();
        int count = 0;
        LinearLayout linearLayout = findViewById(R.id.listLinearLayout);
        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");

        int accent = ContextCompat.getColor(this, R.color.colorPrimary);
        int black = ContextCompat.getColor(this, R.color.jetBlack);
        int white = ContextCompat.getColor(this, R.color.white);
        // if we don't have any saved friends, we have nothing to display; tell user
        if(friendsList.isEmpty()) {
            TextView message = new TextView(this);
            message.setText(R.string.no_friends);
            message.setTextColor(white);
            message.setTextSize(24);
            message.setGravity(Gravity.CENTER);
            linearLayout.setGravity(Gravity.CENTER);
            linearLayout.addView(message);
        }
        else {
            // programmatically create TextViews to place in the LinearLayout since we don't know how
            // many friends a person will have
            for (String friend : friendsList) {
                Log.d("ViewFriends", "Friend" + count + " " + friend);
                TextView friendName = new TextView(this);
                friendName.setText(friend);
                friendName.setTextColor(white);
                friendName.setTextSize(34);
                // create a border for each TextView (friend slot)
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(accent);
                drawable.setStroke(2, black);
                // place border around TextView
                friendName.setBackground(drawable);
                // Add TextView to LinearLayout
                linearLayout.addView(friendName);
                count++;
            }
        }
    }

    // definitely need some functionality for the friends in the list; should we make them removable
    // here or link off to a different page where you can view their profile and remove them, if you want?
    public void removeFriend(View view) {
        FileManager manager = new FileManager(getApplicationContext());

    }
}
