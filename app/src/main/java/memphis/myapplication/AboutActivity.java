package memphis.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupToolbar();
        findViewById(R.id.source_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/named-data-mobile/ndn-photo-app"));
                startActivity(i);
            }
        });
        findViewById(R.id.license_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/named-data-mobile/ndn-photo-app/blob/master/COPYING.md"));
                startActivity(i);
            }
        });
    }

    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(this, "About");
        Toolbar toolbar = toolbarHelper.setupToolbar();
        setSupportActionBar(toolbar);
    }

}
