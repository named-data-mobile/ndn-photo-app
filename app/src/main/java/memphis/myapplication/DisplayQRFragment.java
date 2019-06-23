package memphis.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


/**
 * This activity displays an image provided by the intent (intended for QR codes). This is its
 * only purpose.
 */

public class DisplayQRFragment extends Fragment {
    private int QR_SCANNED = 99;
    private View displayQRView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        displayQRView = inflater.inflate(R.layout.fragment_display_qr, container, false);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            Uri uri = Uri.parse(bundle.getString("uri"));
            ImageView image = displayQRView.findViewById(R.id.QRImage);
            image.setImageURI(uri);
        }
        return displayQRView;
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        setResult(QR_SCANNED);
//    }

}
