package memphis.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSelectActivity extends AppCompatActivity {

    final List<String> filesList = new ArrayList<String>();

    // The path to the root of this app's internal storage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up an Intent to send back to apps that request a file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);

    }
    public void list_files(View view) {
        final ListView lv = (ListView) findViewById(R.id.listview);
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, 0);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                AlertDialog.Builder builder = new AlertDialog.Builder(lv.getContext(), android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("You selected a file").setMessage("File:" + filesList.get(pos)).show();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesList);

        lv.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        Uri uri = null;
        if (resultData != null) {
            final ListView lv = (ListView) findViewById(R.id.listview);
            uri = resultData.getData();
            filesList.add(uri.toString());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filesList);
            lv.setAdapter(adapter);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle("You selected a file").setMessage(uri.toString()).show();
        }
    }

}
