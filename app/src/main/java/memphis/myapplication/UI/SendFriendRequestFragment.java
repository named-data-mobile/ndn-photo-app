package memphis.myapplication.UI;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

import memphis.myapplication.R;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.FriendRequest;
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
    private List<String> friendFriendsList;

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
        ArrayList<User> friendFriends = databaseViewModel.getPotentialFriends();
        friendFriendsList = new ArrayList<>();
        for (User f : friendFriends)
            friendFriendsList.add(f.getUsername());

        // Creating adapter for spinner
        ArrayAdapter<String> newFriendDataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, friendFriendsList);

        // Drop down layout style - list view with radio button
        newFriendDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        newFriendSpinner.setAdapter(newFriendDataAdapter);

        message = sendFriendRequestView.findViewById(R.id.trustTypeMessage);
        if (friendFriendsList.isEmpty()) {
            friend = "";
        } else {
            friend = friendFriendsList.get(0);
        }
        // Radio buttons
        RadioButton rdb1 = sendFriendRequestView.findViewById(R.id.radioMutualFriend);
        rdb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMutual();
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
                if (friendFriendsList.isEmpty() || friend == null)
                    Toast.makeText(getActivity(), "No friend found", Toast.LENGTH_SHORT).show();
                else {
                    Timber.d("Sending friend request to " + friend + " using mutual friend " + mutualFriend);
                    FriendRequest friendRequest = new FriendRequest(friend, mutualFriend, getActivity());
                    friendRequest.send();
                    Navigation.findNavController(sendFriendRequestView).popBackStack();
                }
            }
        });

        return sendFriendRequestView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.spinnerNewFriends) {
            mEdit.setVisibility(View.GONE);
            friend = parent.getItemAtPosition(position).toString();
            getMutual();
        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    private void getMutual(){

        String friendsMessage;
        if (friendFriendsList.isEmpty()) {
            friend = "";
        } else {
            ArrayList<User> myFriends = databaseViewModel.getTrustedFriends();
            ArrayList<String> trusted = new ArrayList<>();
            for (User u : myFriends) {
                trusted.add(u.getUsername());
            }
            friendsMessage = "No mutual friends";
            if (!trusted.isEmpty()) {
                for (String st : trusted) {
                    ArrayList<String> newFriendsList = databaseViewModel.getFriendsofFriend(st);
                    for (String user: newFriendsList){
                        String friendOfFriend = user.substring(user.lastIndexOf("/") + 1);
                        Timber.d("Mutual friend is: " + friendOfFriend);
                        if(friendOfFriend.equals(friend)){
                            if (mutualFriend == null) {
                                mutualFriend = st;
                            }
                            if(friendsMessage.equals("No mutual friends")) {
                                friendsMessage = st + "\n";
                            }
                            else {
                                friendsMessage = friendsMessage + st + "\n";
                            }
                        }
                    }

                }
            }
            message.setText(friendsMessage);
        }

    }
}
