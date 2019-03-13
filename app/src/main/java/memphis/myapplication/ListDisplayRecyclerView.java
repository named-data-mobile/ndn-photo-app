package memphis.myapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ListDisplayRecyclerView extends android.support.v7.widget.RecyclerView.Adapter<ListDisplayRecyclerView.ViewHolder> {

    // This ListDisplayRecyclerView can be used to display a text in a cardView.
    // One Example where it is used in the Project is to display FriendsList.

    private List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    ListDisplayRecyclerView(Context context, List<String> data){
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @NonNull
    @Override
    public ListDisplayRecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ListDisplayRecyclerView.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListDisplayRecyclerView.ViewHolder holder, int position) {
        String friendName = mData.get(position);
        holder.textView.setText(friendName);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    String getItem(int id) {
        return mData.get(id);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends android.support.v7.widget.RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView;
        CardView cardView;
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.recyclerTextView);
            cardView = itemView.findViewById(R.id.recyclerCardView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
