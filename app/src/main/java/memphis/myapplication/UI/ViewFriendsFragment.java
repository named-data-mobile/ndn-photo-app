package memphis.myapplication.UI;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import memphis.myapplication.R;
import memphis.myapplication.data.Common;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.viewmodels.RealmViewModel;

public class ViewFriendsFragment extends Fragment implements ListDisplayRecyclerView.ItemClickListener {

    ListDisplayRecyclerView adapter;
    private View friendsView;
    private RealmViewModel databaseViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        friendsView = inflater.inflate(R.layout.fragment_with_list, container, false);

        databaseViewModel = ViewModelProviders.of(getActivity()).get(RealmViewModel.class);
        final List<String> friendsList;
        friendsList = databaseViewModel.observeAllFriends().getValue();

        // if we don't have any saved friends, we have nothing to display; tell user
        final RecyclerView recyclerView = friendsView.findViewById(R.id.friendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        databaseViewModel.observeAllFriends().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                adapter.notifyDataSetChanged();
                if (strings.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), R.string.no_friends, Toast.LENGTH_LONG).show();
                }
            }
        });

        // set up the ListDisplayRecyclerView, and display the list of friend in ListDisplayRecyclerView
        adapter = new ListDisplayRecyclerView(getActivity(), friendsList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        return friendsView;
    }

    // definitely need some functionality for the friends in the list; should we make them removable
    // here or link off to a different page where you can view their profile and remove them, if you want?
    public void removeFriend(View view) {
        FileManager manager = new FileManager(getActivity().getApplicationContext());
    }

    @Override
    public void onItemClick(View view, int position) {
        // The Functionality to Link off to a different page can be handled here.
        // OnClick of an Item in the recyclerView.
        final String friend = adapter.getItem(position);
        Toast.makeText(getActivity(), "Friend Name: " + adapter.getItem(position), Toast.LENGTH_SHORT).show();
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("Delete friend");
        alert.setMessage("Are you sure you want to delete " + friend);

        // Specifying a listener allows you to take an action before dismissing the dialog.
        // The dialog is automatically dismissed when a dialog button is clicked.
        alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Continue with delete operation
                Common.unfriend(friend);
            }
        });

        // A null listener allows the button to dismiss the dialog and take no further action.
        alert.setNegativeButton(android.R.string.no, null);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.show();

    }
}
