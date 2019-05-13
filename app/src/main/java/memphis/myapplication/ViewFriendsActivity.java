package memphis.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.User;

public class ViewFriendsActivity extends AppCompatActivity implements ListDisplayRecyclerView.ItemClickListener {

    ListDisplayRecyclerView adapter;
    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_with_list);
        Realm realm = Realm.getDefaultInstance();
        RealmResults<User> friends = realm.where(User.class).equalTo("friend", true).findAll();
        realm.close();
        List<String> friendsList = new ArrayList<>();
        for (User f : friends)
            friendsList.add(f.getUsername());
//        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");
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
        final String friend = adapter.getItem(position);
        Toast.makeText(this, "Friend Name: " + adapter.getItem(position), Toast.LENGTH_SHORT).show();
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Delete friend");
        alert.setMessage("Are you sure you want to delete this friend?");

        // Specifying a listener allows you to take an action before dismissing the dialog.
        // The dialog is automatically dismissed when a dialog button is clicked.
        alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Continue with delete operation
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                User user = realm.where(User.class).equalTo("username", friend).findFirst();
                user.setFriend(false);
                realm.commitTransaction();
                realm.close();
                Globals.consumerManager.removeConsumer(friend);
            }
        });

        // A null listener allows the button to dismiss the dialog and take no further action.
        alert.setNegativeButton(android.R.string.no, null);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.show();

    }
}
