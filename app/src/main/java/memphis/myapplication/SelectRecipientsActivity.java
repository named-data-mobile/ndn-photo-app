package memphis.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SelectRecipientsActivity extends AppCompatActivity implements ListDisplayRecyclerView.ItemClickListener {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;
    private ListDisplayRecyclerView adapter;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_recipients);
        m_selectedFriends = new ArrayList<>();
        showFriends();
    }

    private void showFriends() {
        Intent intent = getIntent();
        ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");
        m_sendButton = findViewById(R.id.send_button);

        android.support.v7.widget.RecyclerView recyclerView = findViewById(R.id.friendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // if we don't have any saved friends, we have nothing to display; tell user
        if(friendsList.isEmpty()) {
            m_sendButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(),R.string.no_friends,Toast.LENGTH_LONG).show();
        }
        else {
            adapter = new ListDisplayRecyclerView(this, friendsList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
            m_sendButton.setVisibility(View.GONE);
        }
    }

    private void returnList() {
        // first ask for confirmation; do they want to send the photo to (show list of selected)
        // friends
        AlertDialog.Builder question = new AlertDialog.Builder(SelectRecipientsActivity.this);
        question.setTitle("Send photo to these friends?");
        StringBuilder sb = new StringBuilder();
        for(String friend : m_selectedFriends) {
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
                    String path = getIntent().getStringExtra("photo");
                    Intent data = new Intent();
                    data.putStringArrayListExtra("recipients", m_selectedFriends);
                    data.putExtra("photo", path);
                    setResult(RESULT_OK, data);
                    finish();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    Intent data = new Intent();
                    data.putStringArrayListExtra("recipients", m_selectedFriends);
                    setResult(RESULT_CANCELED, data);
                    finish();
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

    @Override
    public void onItemClick(View view, int position) {
        // Toast.makeText(this, "Selected Friend Name: " + adapter.getItem(position),Toast.LENGTH_SHORT).show();
        // Assumes Each Friend Name will be unique.
        // This condition fails if duplicates exist in friendList
        if(!m_selectedFriends.contains(adapter.getItem(position))){
            m_selectedFriends.add(adapter.getItem(position));
            view.setBackgroundColor(Color.CYAN);
            Log.d("SelectRecipients", "We selected " + adapter.getItem(position));
            Log.d("showFriends", "After add: " + m_selectedFriends.size());
        }else{
            m_selectedFriends.remove(adapter.getItem(position));
            view.setBackgroundColor(Color.TRANSPARENT);
            Log.d("SelectRecipients", "We deselected " + adapter.getItem(position));
            Log.d("showFriends", "After removed: " + m_selectedFriends.size());
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

