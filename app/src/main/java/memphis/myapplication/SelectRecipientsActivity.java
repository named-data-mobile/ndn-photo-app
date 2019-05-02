package memphis.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class SelectRecipientsActivity extends AppCompatActivity implements ListDisplayRecyclerView.ItemClickListener {

    private ArrayList<String> m_selectedFriends;
    private Button m_sendButton;
    private ListDisplayRecyclerView adapter;
    private boolean m_feedSelected;


    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_recipients);
        m_selectedFriends = new ArrayList<>();
        feed();
        showFriends();
    }

    private void feed() {
        final TextView feed = findViewById(R.id.feed);
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
            Intent intent = getIntent();
            ArrayList<String> friendsList = intent.getStringArrayListExtra("friendsList");
            m_sendButton = findViewById(R.id.send_button);

            android.support.v7.widget.RecyclerView recyclerView = findViewById(R.id.friendList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            adapter = new ListDisplayRecyclerView(this, friendsList);
            adapter.setClickListener(this);
            recyclerView.setAdapter(adapter);
            m_sendButton.setVisibility(View.GONE);
        }

    }

    private void returnList() {
        // first ask for confirmation; do they want to send the photo to (show list of selected)
        // friends
        if (m_feedSelected) {
            AlertDialog.Builder question = new AlertDialog.Builder(SelectRecipientsActivity.this);
            question.setTitle("Publish photo to your feed?");
            question.setCancelable(false);

            question.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        String path = getIntent().getStringExtra("photo");
                        Intent data = new Intent();
                        data.putExtra("photo", path);
                        setResult(RESULT_OK, data);
                        finish();
                    } catch (Exception e) {
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
        else {
            AlertDialog.Builder question = new AlertDialog.Builder(SelectRecipientsActivity.this);
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
                        String path = getIntent().getStringExtra("photo");
                        Intent data = new Intent();
                        data.putStringArrayListExtra("recipients", m_selectedFriends);
                        data.putExtra("photo", path);
                        setResult(RESULT_OK, data);
                        finish();
                    } catch (Exception e) {
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

