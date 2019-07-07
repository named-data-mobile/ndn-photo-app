package memphis.myapplication.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import memphis.myapplication.R;
import memphis.myapplication.viewmodels.UserModel;

public class AboutFragment extends Fragment {

    private View aboutView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        aboutView = inflater.inflate(R.layout.fragment_about, container, false);
        UserModel userModel = ViewModelProviders.of(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UserModel(getActivity());
            }
        }).get(UserModel.class);

        setupToolbar(userModel.getUserImage().getValue());
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

    private void setupToolbar(Uri uri) {
        ToolbarHelper toolbarHelper = new ToolbarHelper("About", aboutView);
        Toolbar toolbar = toolbarHelper.setupToolbar(uri);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

}
