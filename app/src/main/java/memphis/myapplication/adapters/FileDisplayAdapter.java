package memphis.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import memphis.myapplication.R;
import memphis.myapplication.viewmodels.FileViewModel;

/**
 * Adapter to display received files
 */
public class FileDisplayAdapter extends RecyclerView.Adapter<FileDisplayAdapter.ViewHolder> {

    private List<File> files;
    private LayoutInflater mInflater;
    private FileViewModel fileViewModel;

    public FileDisplayAdapter(Context context, List<File> data, FileViewModel fileViewModel) {
        this.mInflater = LayoutInflater.from(context);
        this.files = data;
        this.fileViewModel = fileViewModel;
    }

    @NonNull
    @Override
    public FileDisplayAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.fragment_file, parent, false);
        return new FileDisplayAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileDisplayAdapter.ViewHolder holder, int position) {
        File file = files.get(position);
        holder.filename.setText(file.getName());
        holder.size.setText(humanReadableByteCount(file.length()));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView filename;
        TextView size;

        ViewHolder(View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.file_name);
            size = itemView.findViewById(R.id.size);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            fileViewModel.clickedFilePosition.setValue(getAdapterPosition());
        }
    }

    /**
     * Convert the file size from Bytes to human readable format
     * @param bytes File size in Bytes
     * @return File size in human readable form
     */
    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
