package memphis.myapplication.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import memphis.myapplication.R;
import memphis.myapplication.adapters.FileDisplayAdapter;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.viewmodels.FileViewModel;
import timber.log.Timber;

public class ReceivedFilesFragment extends Fragment {

    private View receivedFilesView;
    private FileDisplayAdapter adapter;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        receivedFilesView = inflater.inflate(R.layout.fragment_file_list, container, false);

        FileManager fileManager = new FileManager(getActivity());
        final File[] receivedFiles = new File(fileManager.getRcvdFilesDir()).listFiles();

        FileViewModel fileViewModel = ViewModelProviders.of(this).get(FileViewModel.class);
        fileViewModel.clickedFilePosition.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer position) {
                if (position < 0 || position >= receivedFiles.length) return;
                try {
                    Timber.d("Image position selected: " + position);
                    File clicked = receivedFiles[position];

                    Uri uri = FileProvider.getUriForFile(getActivity(),
                            getActivity().getApplicationContext().getPackageName() + ".fileProvider", clicked);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Arrays.sort(receivedFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.lastModified() > o2.lastModified() ? 1 : -1;
            }
        });

        RecyclerView recyclerView = receivedFilesView.findViewById(R.id.friendList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new FileDisplayAdapter(getActivity().getApplicationContext(), Arrays.asList(receivedFiles), fileViewModel);
        recyclerView.setAdapter(adapter);

        return receivedFilesView;
    }
}
