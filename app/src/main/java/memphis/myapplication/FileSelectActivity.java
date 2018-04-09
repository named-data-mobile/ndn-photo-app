package memphis.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class FileSelectActivity extends AppCompatActivity {

    // The path to the root of this app's internal storage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up an Intent to send back to apps that request a file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);
    }



}