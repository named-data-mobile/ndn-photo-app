package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class NewContentActivity extends AppCompatActivity implements ListDisplayRecyclerView.ItemClickListener {

    private int VIEW_PHOTOS = 0;
    private ListDisplayRecyclerView adapter;
    ConcurrentHashMap<String, ArrayList<String>> userContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_list);
        setupToolbar();
        listReceivedContent();
    }

    /**
     * Retrieves all photos received from friends and lists them by username.
     */
    private void listReceivedContent() {
        FileManager manager = new FileManager(getApplicationContext());
        userContent = manager.getReceivedPhotos();
        // if empty, tell the user there are "No new photos"
        if (userContent.isEmpty()) {
            Toast.makeText(getApplicationContext(),"No Photos",Toast.LENGTH_SHORT).show();
        }
        // else make an entry for each user whom we have received photos from
        else {
            // add in the user's profile photo here too; to do: get profile photos when making friends
            ArrayList<String> friendsList = new ArrayList<>();
            friendsList.addAll(userContent.keySet());

            android.support.v7.widget.RecyclerView recyclerView = findViewById(R.id.friendList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ListDisplayRecyclerView(getApplicationContext(), friendsList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == VIEW_PHOTOS) {
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

    @Override
    public void onItemClick(View view, int position) {
        Log.d("newContentActivity", "we hit onClick");
        Intent intent = new Intent(view.getContext(), ViewPhotosActivity.class);
        intent.putStringArrayListExtra("photos", userContent.get(adapter.getItem(position)));
        Log.d("newContentActivity", "content: " + userContent.get(adapter.getItem(position)));
        Toast.makeText(getApplicationContext(),userContent.get(adapter.getItem(position)).toString(),Toast.LENGTH_LONG).show();
        startActivityForResult(intent, VIEW_PHOTOS);
    }
}
