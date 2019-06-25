package memphis.myapplication.UI;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import memphis.myapplication.R;
import memphis.myapplication.data.Common;
import timber.log.Timber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SelectRecipientsFragment extends Fragment implements ListDisplayRecyclerView.ItemClickListener {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;
    private ListDisplayRecyclerView adapter;
    private boolean m_feedSelected;
    private View selectReceipientsView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        selectReceipientsView = inflater.inflate(R.layout.fragment_select_recipients, container, false);

        m_selectedFriends = new ArrayList<>();
        feed();
        showFriends();
        
        return selectReceipientsView;
    }

    private void feed() {
        final TextView feed = selectReceipientsView.findViewById(R.id.feed);
        final GradientDrawable drawable1 = new GradientDrawable();
        drawable1.setColor(Color.CYAN);
        drawable1.setStroke(2, Color.BLACK);
        feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_selectedFriends.size() <= 0) {
                    if (feed.isSelected()) {
                        m_feedSelected = false;
                        feed.setSelected(false);
                        feed.setBackground(null);
                        m_sendButton.setVisibility(View.GONE);
                    } else {
                        m_feedSelected = true;
                        feed.setSelected(true);
                        feed.setBackground(drawable1);
                        m_sendButton.setVisibility(View.VISIBLE);
                        m_sendButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                returnList();
                            }
                        });
                    }
                }
            }
        });

    }

    private void showFriends() {
        if (!m_feedSelected) {
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                ArrayList<String> friendsList = (ArrayList<String>) bundle.getSerializable("friendsList");
                m_sendButton = selectReceipientsView.findViewById(R.id.send_button);

                RecyclerView recyclerView = selectReceipientsView.findViewById(R.id.friendList);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                adapter = new ListDisplayRecyclerView(getActivity(), friendsList);
                adapter.setClickListener(this);
                recyclerView.setAdapter(adapter);
                m_sendButton.setVisibility(View.GONE);
            }else Navigation.findNavController(selectReceipientsView).popBackStack();
        }
    }

    private void returnList() {
        // first ask for confirmation; do they want to send the photo to (show list of selected)
        // friends
        if (m_feedSelected) {
            AlertDialog.Builder question = new AlertDialog.Builder(getActivity());
            question.setTitle("Publish photo to your feed?");
            question.setCancelable(false);

            question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Bundle bundle = getArguments();
                        if (bundle != null) {
                            String path = bundle.getString("photo");
                            Intent data = new Intent();
                            data.putExtra("photo", path);
                            Common.encryptAndPublish(data, getActivity());
                            Navigation.findNavController(selectReceipientsView).popBackStack();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Intent data = new Intent();
                        data.putStringArrayListExtra("recipients", m_selectedFriends);
                        getActivity().runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
                        Navigation.findNavController(selectReceipientsView).popBackStack();
                    }
                }
            });

            question.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // do nothing; we'll just go back to selecting friends
                }
            });

            question.show();

        }
        else {
            AlertDialog.Builder question = new AlertDialog.Builder(getActivity());
            question.setTitle("Send photo to these friends?");
            StringBuilder sb = new StringBuilder();
            for (String friend : m_selectedFriends) {
                sb.append(friend);
                sb.append(", ");
            }
            sb.delete(sb.lastIndexOf(","), sb.length());
            question.setMessage(sb.toString());
            question.setCancelable(false);

            question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    try {
                        Bundle bundle = getArguments();
                        if (bundle != null) {
                            String path = bundle.getString("photo");
                            Intent data = new Intent();
                            data.putStringArrayListExtra("recipients", m_selectedFriends);
                            data.putExtra("photo", path);
                            Common.encryptAndPublish(data, getActivity());
                            Navigation.findNavController(selectReceipientsView).popBackStack();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Intent data = new Intent();
                        data.putStringArrayListExtra("recipients", m_selectedFriends);
                        getActivity().runOnUiThread(makeToast("Something went wrong with sending photo. Try resending"));
                        Navigation.findNavController(selectReceipientsView).popBackStack();
                    }
                }
            });

            question.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // do nothing; we'll just go back to selecting friends
                }
            });

            question.show();
        }
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public void onItemClick(View view, int position) {
        // Toast.makeText(getActivity(), "Selected Friend Name: " + adapter.getItem(position),Toast.LENGTH_SHORT).show();
        // Assumes Each Friend Name will be unique.
        // This condition fails if duplicates exist in friendList
        if(!m_selectedFriends.contains(adapter.getItem(position))){
            m_selectedFriends.add(adapter.getItem(position));
            view.setBackgroundColor(Color.CYAN);
            Timber.d("SelectRecipients: %s", "We selected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After add: " + m_selectedFriends.size());
        }else{
            m_selectedFriends.remove(adapter.getItem(position));
            view.setBackgroundColor(Color.TRANSPARENT);
            Timber.d("SelectRecipients: %s", "We deselected " + adapter.getItem(position));
            Timber.d("showFriends: %s", "After removed: " + m_selectedFriends.size());
        }

        if(m_selectedFriends.size() == 0) {
            // remove button since we have selected 0 friends now
            m_sendButton.setVisibility(View.GONE);
        }else{
            m_sendButton.setVisibility(View.VISIBLE);
        }

        m_sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnList();
            }
        });
    }
}

