package memphis.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class NewContentFragment extends Fragment implements ListDisplayRecyclerView.ItemClickListener {

    private int VIEW_PHOTOS = 0;
    private ListDisplayRecyclerView adapter;
    ConcurrentHashMap<String, ArrayList<String>> userContent;
    private View newContentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        newContentView = inflater.inflate(R.layout.fragment_with_list, container, false);
        setupToolbar();
        listReceivedContent();

        return newContentView;
    }

    /**
     * Retrieves all photos received from friends and lists them by username.
     */
    private void listReceivedContent() {
        FileManager manager = new FileManager(getActivity().getApplicationContext());
        userContent = manager.getReceivedPhotos();
        RecyclerView recyclerView = newContentView.findViewById(R.id.friendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // if empty, tell the user there are "No new photos"
        if (userContent.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            Toast.makeText(getActivity().getApplicationContext(),R.string.no_photos,Toast.LENGTH_LONG).show();
        }
        // else make an entry for each user whom we have received photos from
        else {
            // add in the user's profile photo here too; to do: get profile photos when making friends
            ArrayList<String> friendsList = new ArrayList<>();
            friendsList.addAll(userContent.keySet());
            adapter = new ListDisplayRecyclerView(getActivity().getApplicationContext(), friendsList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
//        if (requestCode == VIEW_PHOTOS) {
            // this refreshes our page so we no longer see the user whose photos we just viewed
            // the current activity finishes and we start a new one which triggers onCreate again
            //TODO: update list
//            finish();
//            startActivity(getIntent());
//        }
    }

    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(getActivity(), "Photos", newContentView);
        Toolbar toolbar = toolbarHelper.setupToolbar();
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

    @Override
    public void onItemClick(View view, int position) {
        Timber.d("we hit onClick");
        Bundle bundle = new Bundle();
        bundle.putSerializable("photos", userContent.get(adapter.getItem(position)));
        Timber.d("content: " + userContent.get(adapter.getItem(position)));
        Navigation.findNavController(newContentView).navigate(R.id.action_newContentActivity_to_viewPhotosActivity, bundle);
//        startActivityForResult(intent, VIEW_PHOTOS);
    }
}
