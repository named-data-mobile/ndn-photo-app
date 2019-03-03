package memphis.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SelectRecipientsActivity extends AppCompatActivity {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_recipients);
        m_selectedFriends = new ArrayList<>();
        showFriends();
    }

    private void showFriends() {
        Intent intent = getIntent();
        LinearLayout linearLayout = findViewById(R.id.listLinearLayout);
        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");

        final int primary = ContextCompat.getColor(this, R.color.colorPrimary);
        final int black = ContextCompat.getColor(this, R.color.jetBlack);
        final int white = ContextCompat.getColor(this, R.color.primaryTextColor);
        // if we don't have any saved friends, we have nothing to display; tell user
        if(friendsList.isEmpty()) {
            m_sendButton = findViewById(R.id.send_button);
            m_sendButton.setVisibility(View.GONE);
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
                final GradientDrawable drawable1 = new GradientDrawable();
                drawable1.setColor(primary);
                drawable1.setStroke(2, black);

                final GradientDrawable drawable2 = new GradientDrawable();
                drawable2.setColor(Color.parseColor("#333377"));
                drawable2.setStroke(2, black);

                // place border around TextView and set background
                friendName.setBackground(drawable1);

                friendName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("SelectRecipients", "We selected " + friendName.getText());
                        if (friendName.isSelected()) {
                            friendName.setBackground(drawable1);
                            friendName.setSelected(false);
                            m_selectedFriends.remove(friendName.getText().toString());
                            if(m_selectedFriends.size() < 1) {
                                // remove button since we have selected 0 friends now
                                m_sendButton.setVisibility(View.GONE);
                            }
                            Log.d("showFriends", "After removed: " + m_selectedFriends.size());
                        } else {
                            friendName.setBackground(drawable2);
                            friendName.setSelected(true);
                            m_selectedFriends.add(friendName.getText().toString());
                            if(m_selectedFriends.size() == 1) {
                                // only need to set visibility for button when we add the first friend
                                m_sendButton.setVisibility(View.VISIBLE);
                            }
                            Log.d("showFriends", "After add: " + m_selectedFriends.size());
                        }
                    }
                });
                // Add TextView to LinearLayout
                linearLayout.addView(friendName);
            }

            m_sendButton = findViewById(R.id.send_button);
            // make send button invisible and unclickable until we actually select a friend
            m_sendButton.setVisibility(View.GONE);
            m_sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    returnList();
                }
            });
        }
    }

    private void returnList() {
        // first ask for confirmation; do they want to send the photo to (show list of selected)
        // friends
        AlertDialog.Builder question = new AlertDialog.Builder(SelectRecipientsActivity.this);
        question.setTitle("Send photo to these friends?");
        StringBuilder sb = new StringBuilder();
        for(String friend : m_selectedFriends) {
            sb.append(friend);
            sb.append(", ");
        }
        sb.delete(sb.lastIndexOf(","), sb.length());
        question.setMessage(sb.toString());
        question.setCancelable(false);

        question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                try {
                    String path = getIntent().getStringExtra("photo");
                    Intent data = new Intent();
                    data.putStringArrayListExtra("recipients", m_selectedFriends);
                    data.putExtra("photo", path);
                    setResult(RESULT_OK, data);
                    finish();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    Intent data = new Intent();
                    data.putStringArrayListExtra("recipients", m_selectedFriends);
                    setResult(RESULT_CANCELED, data);
                    finish();
                }
            }
        });

        question.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // do nothing; we'll just go back to selecting friends
            }
        });

        question.show();
    }
}
