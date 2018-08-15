package memphis.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import memphis.myapplication.R;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private final LayoutInflater mInflater;

    // all Images in array
    public Integer[] m_photos;

    ImageAdapter(Context ct){
        this.context = ct;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return m_photos.length;
    }

    public void setPhotoResources(Integer[] photos) {
        m_photos = photos;
    }

    @Override
    public Object getItem(int position) {
        return m_photos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new GridView.LayoutParams(185, 185));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(5, 5, 5, 5);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(m_photos[position]);
        return imageView;
    }
}