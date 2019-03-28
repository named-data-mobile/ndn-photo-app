package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.ArrayList;

public class ViewFriendsActivity extends AppCompatActivity implements ListDisplayRecyclerView.ItemClickListener {

    ListDisplayRecyclerView adapter;
    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_with_list);
        setToolbar();
      
        Intent intent = getIntent();
        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");
        // if we don't have any saved friends, we have nothing to display; tell user
        android.support.v7.widget.RecyclerView recyclerView = findViewById(R.id.friendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if(friendsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(),R.string.no_friends,Toast.LENGTH_LONG).show();
        }
        else {
            // set up the ListDisplayRecyclerView, and display the list of friend in ListDisplayRecyclerView
            adapter = new ListDisplayRecyclerView(this, friendsList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
        }
    }

    // definitely need some functionality for the friends in the list; should we make them removable
    // here or link off to a different page where you can view their profile and remove them, if you want?
    public void removeFriend(View view) {
        FileManager manager = new FileManager(getApplicationContext());
    }

    @Override
    public void onItemClick(View view, int position) {
        // The Functionality to Link off to a different page can be handled here.
        // OnClick of an Item in the recyclerView.
        Toast.makeText(this, "Friend Name: " + adapter.getItem(position), Toast.LENGTH_SHORT).show();
    }

    private void setToolbar() {
        String str = getApplicationContext().getResources().getString(R.string.friend_list);
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, str );
        Toolbar toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }
}
