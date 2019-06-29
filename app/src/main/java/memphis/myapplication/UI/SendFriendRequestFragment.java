package memphis.myapplication.UI;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.FriendRequest;
import memphis.myapplication.R;
import memphis.myapplication.viewmodels.RealmViewModel;
import timber.log.Timber;

public class SendFriendRequestFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    Spinner newFriendSpinner;
    String mutualFriend;
    EditText mEdit;
    String friend;
    TextView message;
    private View sendFriendRequestView;
    private RealmViewModel databaseViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sendFriendRequestView = inflater.inflate(R.layout.fragment_send_friend_request, container, false);

        databaseViewModel = ViewModelProviders.of(getActivity()).get(RealmViewModel.class);

        // Edit text
        mEdit = sendFriendRequestView.findViewById(R.id.add_remote_friend_edit_text);

        // New Friend Spinner element
        newFriendSpinner = sendFriendRequestView.findViewById(R.id.spinnerNewFriends);

        // New Friend Spinner click listener
        newFriendSpinner.setOnItemSelectedListener(this);

        // New Friend Spinner Drop down elements
        ArrayList<User> potentialFriends = databaseViewModel.getPotentialFriends();
        List<String> potentialFriendsList = new ArrayList<>();
        for (User f : potentialFriends)
            potentialFriendsList.add(f.getUsername());

        // Creating adapter for spinner
        ArrayAdapter<String> newFriendDataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, potentialFriendsList);

        // Drop down layout style - list view with radio button
        newFriendDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        newFriendSpinner.setAdapter(newFriendDataAdapter);

        message = sendFriendRequestView.findViewById(R.id.trustTypeMessage);

        String friendsMessage = "";
        if (potentialFriends.isEmpty()) {
            friend = "";
        } else {
            friend = potentialFriends.get(0).getUsername();
            ArrayList<String> newFriendsList = databaseViewModel.getFriendsofFriend(friend);
            ArrayList<User> myFriends = databaseViewModel.getTrustedFriends();
            ArrayList<String> myFriendsList = new ArrayList<>();
            for (User u : myFriends) {
                myFriendsList.add(u.getNamespace());
            }
            myFriendsList.retainAll(newFriendsList);
            if (myFriendsList.isEmpty()) {
                friendsMessage = "No mutual friends";
            }
            for (String f : myFriendsList) {
                mutualFriend = f.substring(f.lastIndexOf("/")+1);
                friendsMessage = friendsMessage + f + "\n";
            }
            message.setText(friendsMessage);
        }

        // Radio buttons
        RadioButton rdb1 = sendFriendRequestView.findViewById(R.id.radioMutualFriend);
        rdb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Iterate through our mutual friends
                String friendsMessage = "";
                ArrayList<String> newFriendsList = databaseViewModel.getFriendsofFriend(friend);
                ArrayList<User> myFriends = databaseViewModel.getTrustedFriends();
                ArrayList<String> myFriendsList = new ArrayList<>();
                for (User u : myFriends) {
                    myFriendsList.add(u.getNamespace());
                }
                myFriendsList.retainAll(newFriendsList);
                if (myFriendsList.isEmpty()) {
                    friendsMessage = "No mutual friends";
                }
                for (String f : myFriendsList) {
                    mutualFriend = f.substring(f.lastIndexOf("/")+1);
                    friendsMessage = friendsMessage + f + "\n";
                }
                message.setText(friendsMessage);
            }
        });

        RadioButton rdb2 = sendFriendRequestView.findViewById(R.id.radioDomain);
        rdb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message.setText("Not currently supported");
            }
        });

        sendFriendRequestView.findViewById(R.id.sendRemoteFriendRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (friend == null)
                    friend = mEdit.getText().toString();
                Timber.d("Sending friend request to " + friend + " using mutual friend " + mutualFriend);
                FriendRequest friendRequest = new FriendRequest(friend, mutualFriend, getActivity());
                friendRequest.send();
                Navigation.findNavController(sendFriendRequestView).popBackStack();
            }
        });

        return sendFriendRequestView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.spinnerNewFriends) {
                mEdit.setVisibility(View.GONE);
                friend = parent.getItemAtPosition(position).toString();
        }
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
}
