package memphis.myapplication.UI;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.SigningInfo;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import memphis.myapplication.data.Common;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.Globals;
import memphis.myapplication.utilities.QRExchange;
import memphis.myapplication.R;
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.viewmodels.RealmViewModel;
import memphis.myapplication.viewmodels.UserModel;
import timber.log.Timber;

import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

public class AddFriendFragment extends Fragment {

    private final int FRIEND_QR_REQUEST_CODE = 0;
    private final int ADD_FRIEND_QR_REQUEST_CODE = 1;
    private final int REMOTE_FRIEND_REQUEST_CODE = 2;
    private final int RESULT_ALREADY_TRUST = 5;
    private final int DISPLAY_QR = 1;
    private final int SCAN_QR = -1;
    private final int NONE = 0;

    private RealmViewModel databaseViewModel;
    private ToolbarHelper toolbarHelper;
    private Toolbar toolbar;
    private String friendName;
    private String friendDomain;
    private View addFriendView;
    private int nextState = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        addFriendView = inflater.inflate(R.layout.fragment_add_friend, container, false);

        databaseViewModel = ViewModelProviders.of(getActivity()).get(RealmViewModel.class);

        UserModel userModel = ViewModelProviders.of(getActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UserModel(getActivity());
            }
        }).get(UserModel.class);

        setupToolbar(userModel.getUserImage().getValue());
        setButtonWidth();

        setListeners();

        if (nextState != NONE){
            if (nextState == DISPLAY_QR){
                displayMyQR(new View(getActivity()));
                nextState = NONE;
            }else{
                scanFriendQR(new View(getActivity()));
                nextState = NONE;
            }
        }
        return addFriendView;
    }

    private void setListeners() {
        addFriendView.findViewById(R.id.scanFriendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanFriendQR(v);
            }
        });

        addFriendView.findViewById(R.id.showYourCode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayMyQR(v);
            }
        });

        addFriendView.findViewById(R.id.addFriend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Display or scan QR code first?").setCancelable(false);
                final View tempView = view;

                builder.setPositiveButton("Display QR code", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send them to a new page to select friends to send photo to
                        scanFriendQR(tempView);
                        nextState = DISPLAY_QR;
//                        displayMyQR(tempView);

                    }
                });
                builder.setNegativeButton("Scan QR code", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        displayMyQR(tempView);
                        nextState = SCAN_QR;
//                        scanFriendQR(tempView);

                    }
                });

                builder.show();
            }
        });

        addFriendView.findViewById(R.id.remoteFriend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(addFriendView).navigate(R.id.action_addFriendFragment_to_sendFriendRequest);
            }
        });

        addFriendView.findViewById(R.id.viewFriendsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(addFriendView).navigate(R.id.action_addFriendFragment_to_viewFriendsFragment);
//        ArrayList<String> friendsList = SharedPrefsManager.getInstance(getActivity()).getFriendsList();
//        intent.putStringArrayListExtra("friendsList", friendsList);
            }
        });
    }

    private void setupToolbar(Uri uri) {
        toolbarHelper = new ToolbarHelper(getString(R.string.friends_title), addFriendView);
        toolbar = toolbarHelper.setupToolbar(uri);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

    private void setButtonWidth() {
        DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        int width = metrics.widthPixels / 3;
        Button btn1 = addFriendView.findViewById(R.id.showYourCode);
        btn1.setWidth(width);
        Button btn2 = addFriendView.findViewById(R.id.scanFriendButton);
        btn2.setWidth(width);
        Button btn3 = addFriendView.findViewById(R.id.viewFriendsButton);
        btn3.setWidth(width);
    }

    // To do: add new Intent for the remote friend button; getActivity() new activity should allow the user
    // to search for usernames and befriend them (send friendship interest)

    public void scanFriendQR(View view) {
        IntentIntegrator scanner = new IntentIntegrator(getActivity());
        // only want QR code scanner
        scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
        scanner.setOrientationLocked(true);
        // back facing camera id
        scanner.setCameraId(0);
        Intent intent = scanner.createScanIntent();
        startActivityForResult(intent, FRIEND_QR_REQUEST_CODE);

    }

    public void displayMyQR(View view) {
        // need to retrieve our QR file, and if it does not exist, create one first.
        FileManager manager = new FileManager(getActivity().getApplicationContext());
        File file = new File(manager.getMyQRPath());
        if (!file.exists()) {
            manager.saveMyQRCode(QRExchange.makeQRFriendCode(getActivity(), manager));
        }
        Bundle bundle = new Bundle();
        bundle.putString("uri", Uri.fromFile(file).toString());
        Navigation.findNavController(addFriendView).navigate(R.id.action_addFriendFragment_to_displayQRFragment, bundle);
    }

    // save friends
    public int saveFriend(String friendContent) {
        KeyChain keyChain = Globals.keyChain;
        Timber.d("Saving: %s", friendContent);

        if (friendContent.length() > 0) {
            try {


                byte[] certBytes = Base64.decode(friendContent, 0);
                CertificateV2 certificateV2 = null;

                certificateV2 = new CertificateV2();
                certificateV2.wireDecode(ByteBuffer.wrap(certBytes));

                // Save self-signed cert for friend
                friendName = certificateV2.getName().getSubName(-5, 1).toUri().substring(1);
                Timber.d("Friend name: %s", friendName);
                for (int i = 0; i <= certificateV2.getName().size(); i++) {
                    Timber.d(certificateV2.getName().getSubName(i, 1).toUri());
                    if (certificateV2.getName().getSubName(i, 1).toUri().equals("/npChat")) {
                        friendDomain = certificateV2.getName().getPrefix(i).toUri();
                    }
                }

                int result = databaseViewModel.saveFriend(friendName);
                if(result != -1) return result;
                // A friend's filename will be their username. Another reason why we must ensure uniqueness


                // Change cert name
                Name certName = certificateV2.getName();
                Name newCertName = new Name();
                int signerComp = 0;
                for (int i = 0; i <= certName.size(); i++) {
                    if (certName.getSubName(i, 1).toUri().equals("/KEY")) {
                        signerComp = i + 2;
                        break;
                    }
                }
                newCertName.append(certName.getSubName(0, signerComp));
                newCertName.append(SharedPrefsManager.getInstance(getActivity()).getUsername());
                newCertName.append(certName.getSubName(signerComp + 1));
                certificateV2.setName(newCertName);

                //Sign cert with our key
                Globals.keyChain.sign(certificateV2, new SigningInfo(SigningInfo.SignerType.KEY, Globals.pubKeyName));
                Timber.d(certificateV2.toString());


                // Store friend's cert signed by us
//            SharedPrefsManager.getInstance(getActivity()).storeFriendCert(friendName, certificateV2);
                databaseViewModel.saveNewFriend(friendName, friendDomain, certificateV2);
                return 0;
            } catch (PibImpl.Error error) {
                error.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (KeyChain.Error error) {
                error.printStackTrace();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            }
        }
        return -1;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == FRIEND_QR_REQUEST_CODE) || (requestCode == ADD_FRIEND_QR_REQUEST_CODE)) {
            IntentResult result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data);
            if (result == null) {
                Toast.makeText(getActivity(), "Null", Toast.LENGTH_LONG).show();
            }
            if (result != null) {
                // check resultCode to determine what type of code we're scanning, file or friend

                if (result.getContents() != null) {
                    String content = result.getContents();
                    Timber.d("ScannedFriend: %s", content);
                    // need to check getActivity() content to determine if we are scanning file or friend code
                    // Toast.makeText(getActivity(), content, Toast.LENGTH_LONG).show();
                    int saveResult = saveFriend(content);
                    if (saveResult == 0) {
                        Toast.makeText(getActivity(), "Retrieving certificate.", Toast.LENGTH_LONG).show();
                        if (content.length() > 0) {
                            Timber.d("Friend result ok");
                            // After adding friend, wait 5 seconds and then send interest for your own certificate signed by your friend
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Common.generateCertificateInterest(friendName);
                                    } catch (SecurityException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 5000);
                            if(nextState == DISPLAY_QR){
                                displayMyQR(new View(getActivity()));
                                nextState = NONE;
                            }else
                            Navigation.findNavController(addFriendView).popBackStack();

                        } else {
                            Navigation.findNavController(addFriendView).popBackStack();
                        }
                    } else if (saveResult == 1) {
                        Toast.makeText(getActivity(), "You are already friends.", Toast.LENGTH_LONG).show();
                        Navigation.findNavController(addFriendView).popBackStack();
                    } else if (saveResult == 3) {
                        Toast.makeText(getActivity(), "Already have certificate.", Toast.LENGTH_LONG).show();
                        if (content.length() > 0) {
                            Timber.d("Friend result ok");
                            User friend = databaseViewModel.setFriendship(friendName);
                            Globals.consumerManager.createConsumer(friend.getNamespace());
                            Navigation.findNavController(addFriendView).popBackStack();

                        } else {
                            Toast.makeText(getActivity(), "Error saving friend.", Toast.LENGTH_LONG).show();
                            Navigation.findNavController(addFriendView).popBackStack();
                        }
                    }
                } else if (resultCode == 99) {
                    scanFriendQR(addFriendView.findViewById(android.R.id.content));

                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
            }
        } else super.onActivityResult(requestCode, resultCode, data);
    }
}
