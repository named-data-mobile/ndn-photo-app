package memphis.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.User;
import timber.log.Timber;

public class SendFriendRequest extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Spinner newFriendSpinner;
    String mutualFriend;
    Spinner mutualFriendSpinner;
    TextView mutualFriendSpinnerLabel;
    EditText mEdit;
    String friend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_friend_request);
        Realm realm = Realm.getDefaultInstance();

        // Edit text
        mEdit = findViewById(R.id.add_remote_friend_edit_text);

        // New Friend Spinner element
        newFriendSpinner = findViewById(R.id.spinnerNewFriends);

        // New Friend Spinner click listener
        newFriendSpinner.setOnItemSelectedListener(this);

        // New Friend Spinner Drop down elements
        RealmResults<User> potentialFriends = realm.where(User.class).equalTo("friend", false).findAll();
        List<String> potentialFriendsList = new ArrayList<>();
        for (User f : potentialFriends)
            potentialFriendsList.add(f.getUsername());

        potentialFriendsList.add("Enter user prefix");

        // Creating adapter for spinner
        ArrayAdapter<String> newFriendDataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, potentialFriendsList);

        // Drop down layout style - list view with radio button
        newFriendDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        newFriendSpinner.setAdapter(newFriendDataAdapter);

        // Mutual Friend Spinner element
        mutualFriendSpinner = findViewById(R.id.spinner);
        mutualFriendSpinnerLabel = findViewById(R.id.mutal_friend_label);

        // Mutual Friend Spinner click listener
        mutualFriendSpinner.setOnItemSelectedListener(this);

//         Mutual Friend Spinner Drop down elements
//        RealmResults<User> friends = realm.where(User.class).equalTo("trust", true).findAll();
        RealmResults<User> friends = realm.where(User.class).findAll();
        List<String> currentFriends = new ArrayList<>();
        for (User f : friends)
            currentFriends.add(f.getUsername());

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currentFriends);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        mutualFriendSpinner.setAdapter(dataAdapter);

        // Radio buttons
        RadioButton rdb1 = findViewById(R.id.radioMutualFriend);
        rdb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mutualFriendSpinner.setVisibility(View.VISIBLE);
                mutualFriendSpinnerLabel.setVisibility(View.VISIBLE);

            }
        });

        RadioButton rdb2 = findViewById(R.id.radioDomain);
        rdb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mutualFriendSpinner.setVisibility(View.GONE);
                mutualFriendSpinnerLabel.setVisibility(View.GONE);

            }
        });


    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        if (parent.getId() == R.id.spinner) {
            mutualFriend = parent.getItemAtPosition(position).toString();
        } else if (parent.getId() == R.id.spinnerNewFriends) {
            if (parent.getItemAtPosition(position).toString().equals("Enter user prefix")) {
                friend = null;
                mEdit.setVisibility(View.VISIBLE);
            } else {
                mEdit.setVisibility(View.GONE);
                friend = parent.getItemAtPosition(position).toString();
            }


        }
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void sendRemoteFriendRequest(View view) {
        if (friend == null)
            friend = mEdit.getText().toString();
        Timber.d("Sending friend request to " + friend + " using mutual friend " + mutualFriend);
        FriendRequest friendRequest = new FriendRequest(friend, mutualFriend, this);
        friendRequest.send();
        Intent intent = new Intent(this, AddFriendActivity.class);
        startActivity(intent);


    }
}
