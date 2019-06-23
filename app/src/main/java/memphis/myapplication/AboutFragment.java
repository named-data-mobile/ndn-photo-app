package memphis.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AboutFragment extends Fragment {

    private View aboutView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        aboutView = inflater.inflate(R.layout.fragment_about, container, false);
        setupToolbar();
        aboutView.findViewById(R.id.source_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/named-data-mobile/ndn-photo-app"));
                startActivity(i);
            }
        });
        aboutView.findViewById(R.id.license_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/named-data-mobile/ndn-photo-app/blob/master/COPYING.md"));
                startActivity(i);
            }
        });

        return aboutView;
    }

    private void setupToolbar() {
        ToolbarHelper toolbarHelper = new ToolbarHelper(getActivity(), "About", aboutView);
        Toolbar toolbar = toolbarHelper.setupToolbar();
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

}
