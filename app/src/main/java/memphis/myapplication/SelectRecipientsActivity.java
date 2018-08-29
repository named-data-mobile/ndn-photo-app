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

public class SelectRecipientsActivity extends AppCompatActivity {

    private ArrayList<String> m_selectedFriends;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        showFriends();
    }

    private void showFriends() {
        Intent intent = getIntent();
        LinearLayout linearLayout = findViewById(R.id.listLinearLayout);
        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");

        int accent = ContextCompat.getColor(this, R.color.colorPrimary);
        int black = ContextCompat.getColor(this, R.color.jetBlack);
        int white = ContextCompat.getColor(this, R.color.white);
        // if we don't have any saved friends, we have nothing to display; tell user
        if(friendsList.isEmpty()) {
            TextView message = new TextView(this);
            String s = "You currently haven't added any friends.";
            message.setText(s);
            message.setTextColor(white);
            message.setTextSize(24);
            message.setGravity(Gravity.LEFT);
            linearLayout.addView(message);
        }
        else {
            // programmatically create TextViews to place in the LinearLayout since we don't know how
            // many friends a person will have
            for (String friend : friendsList) {
                final TextView friendName = new TextView(this);
                friendName.setText(friend);
                friendName.setTextColor(white);
                friendName.setTextSize(34);
                // create a border for each TextView (friend slot)
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(accent);
                drawable.setStroke(2, black);
                // place border around TextView
                friendName.setBackground(drawable);
                friendName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("SelectRecipients", "We selected " + friendName.getText());
                        // friendName.setSelected(true);
                        // m_selectedFriends.add(friendName.getText());
                    }
                });
                // Add TextView to LinearLayout
                linearLayout.addView(friendName);
            }
        }
    }
}
