package memphis.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.User;

public class SendFriendRequest extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String mutualFriend;
    Spinner spinner;
    TextView spinnerLabel;
    EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_friend_request);
        Realm realm = Realm.getDefaultInstance();

        // Spinner element
        spinner = findViewById(R.id.spinner);
        spinnerLabel = findViewById(R.id.mutal_friend_label);

        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        RealmResults<User> friends = realm.where(User.class).equalTo("friend", true).findAll();
        List<String> currentFriends = new ArrayList<>();
        for (User f : friends)
            currentFriends.add(f.getUsername());

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currentFriends);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        // Radio buttons
        RadioButton rdb1 = findViewById(R.id.radioMutualFriend);
        rdb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.VISIBLE);
                spinnerLabel.setVisibility(View.VISIBLE);

            }
        });

        RadioButton rdb2 = findViewById(R.id.radioDomain);
        rdb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.GONE);
                spinnerLabel.setVisibility(View.GONE);

            }
        });

        // Edit text
        mEdit = findViewById(R.id.add_remote_friend_edit_text);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        mutualFriend = parent.getItemAtPosition(position).toString();
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    public void sendRemoteFriendRequest(View view) {
        String friend = mEdit.getText().toString();
        Log.d("SendFriendRequest", "Sending friend request to " + friend + " using mutual friend " + mutualFriend);
        new FriendRequest(friend, mutualFriend, this);

    }
}
