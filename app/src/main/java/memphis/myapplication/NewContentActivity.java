package memphis.myapplication;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NewContentActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private int VIEW_PHOTOS = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        setupToolbar();
        linearLayout = findViewById(R.id.listLinearLayout);
        listReceivedContent();
    }

    /**
     * Retrieves all photos received from friends and lists them by username.
     */
    private void listReceivedContent() {
        FileManager manager = new FileManager(getApplicationContext());
        final ConcurrentHashMap<String, ArrayList<String>> userContent = manager.getReceivedPhotos();
        int accent = ContextCompat.getColor(this, R.color.colorPrimary);
        int black = ContextCompat.getColor(this, R.color.jetBlack);
        int white = ContextCompat.getColor(this, R.color.white);

        // if empty, tell the user there are "No new photos"
        if(userContent.isEmpty()) {
            TextView message = new TextView(this);
            String s = "No new photos";
            message.setText(s);
            message.setTextColor(white);
            message.setTextSize(34);
            message.setGravity(Gravity.CENTER);
            linearLayout.setGravity(Gravity.CENTER);
            linearLayout.addView(message);
        }
        // else make an entry for each user whom we have received photos from
        else {
            // add in the user's profile photo here too; to do: get profile photos when making friends
            Set<String> friends = userContent.keySet();
            for (final String friend : friends) {
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
                // make the TextView object span the line it is set on, so the whole line is clickable
                Log.d("newContentActivity", "parent: " + friendName.getParent().toString());
                friendName.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                friendName.setClickable(true);
                friendName.bringToFront();
                //friendName.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);*/
                friendName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("newContentActivity", "we hit onClick");
                        Intent intent = new Intent(view.getContext(), ViewPhotosActivity.class);
                        intent.putStringArrayListExtra("photos", userContent.get(friend));
                        Log.d("newContentActivity", "content: " + userContent.get(friend));
                        startActivityForResult(intent, VIEW_PHOTOS);
                    }
                });
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if(requestCode == VIEW_PHOTOS) {
            // this refreshes our page so we no longer see the user whose photos we just viewed
            // the current activity finishes and we start a new one which triggers onCreate again
            finish();
            startActivity(getIntent());
        }
    }
    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, "Photos");
        Toolbar toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }
}
