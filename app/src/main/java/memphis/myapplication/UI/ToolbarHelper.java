package memphis.myapplication.UI;

import androidx.appcompat.widget.Toolbar;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import memphis.myapplication.R;
/**
 * ToolbarHelper helps setting up a custom toolbar
 */
public class ToolbarHelper {

    private String title;
    private View view;

    public ToolbarHelper(String title, View view) {
        this.title = title;
        this.view = view;
    }

    /**
     * Get a custom toolbar with an image
     * @param uri URI for the profile picture
     * @return new custom toolbar
     */
    public Toolbar setupToolbar(Uri uri) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ImageView imageView = view.findViewById(R.id.toolbar_main_photo);
        TextView textView = view.findViewById(R.id.main_title);
        textView.setText(this.title);
        Picasso.get().load(uri).placeholder(R.drawable.avatar).memoryPolicy(MemoryPolicy.NO_CACHE).fit().centerCrop().into(imageView);
        return toolbar;
    }
}
