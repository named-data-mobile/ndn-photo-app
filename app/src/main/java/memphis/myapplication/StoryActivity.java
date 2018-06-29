package memphis.myapplication;

import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public class StoryActivity extends AppCompatActivity {

    private ArrayList<Story> storyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);
        fillStoryList();
    }

    private void fillStoryList() {
        // retrieve all stories (sync will do this)
        // for(Story story : stories)
    }

}
