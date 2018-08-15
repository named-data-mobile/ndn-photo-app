package memphis.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boxes);

        GridView gridView = (GridView) findViewById(R.id.mainGrid);
        ImageAdapter imgAdapter = new ImageAdapter(this);
        Integer[] images = {R.drawable.mountain, R.drawable.hotel, R.drawable.bandit,
                            R.drawable.bandit, R.drawable.bandit, R.drawable.bandit};
        imgAdapter.setPhotoResources(images);
        gridView.setAdapter(imgAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent,
                                    View v, int position, long id)
            {
                Toast.makeText(getBaseContext(),
                        "pic" + (position + 1) + " selected",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


}
