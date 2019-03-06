package memphis.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import memphis.myapplication.R;

public class ImageAdapter extends BaseAdapter {
    private Context m_context;
    private final LayoutInflater m_inflater;
    private int m_actionBarHeight;
    private GridView m_gridView;

    // all Images in array
    private Integer[] m_photos;
    private String[] m_text;

    ImageAdapter(Context ct, int abHeight){
        m_context = ct;
        m_inflater = LayoutInflater.from(m_context);
        m_actionBarHeight = abHeight;
    }

    @Override
    public int getCount() {
        return m_photos.length;
    }

    public void setPhotoResources(Integer[] photos) {
        m_photos = photos;
    }

    public void setTextValues(String[] text) {m_text = text; }

    public void setGridView(GridView grid) {m_gridView = grid; }

    @Override
    public Object getItem(int position) {
        return m_photos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // This sets each image we provided in our activity to a gridview of 4 images.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View grid;
        LayoutInflater inflater = (LayoutInflater) m_context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {

            grid = new View(m_context);
            grid = inflater.inflate(R.layout.grid_item, null);

            // get screen size
            DisplayMetrics metrics = m_context.getResources().getDisplayMetrics();
            // set up two columns
            int width = metrics.widthPixels/2;
            // account for the actionBar so pictures on both rows are the same size
            int height = (metrics.heightPixels - m_actionBarHeight)/2;

            TextView textView = (TextView) grid.findViewById(R.id.grid_text);
            ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(width, height-20));
            imageView.setPadding(10, 10, 10, 10);
            Picasso.get().load(m_photos[position]).fit().centerCrop().into(imageView);

            textView.setText(m_text[position]);

        }
        else {
            grid = convertView;
        }

        return grid;
    }
}