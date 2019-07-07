package memphis.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.Observable;

import io.realm.Realm;
import memphis.myapplication.utilities.FriendRequest;
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.viewmodels.BackgroundViewModel;
import memphis.myapplication.viewmodels.RealmViewModel;
import timber.log.Timber;

public class IntroActivity extends AppCompatActivity {


    private BackgroundViewModel backgroundViewModel;
    private RealmViewModel realmViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        NavController navController = Navigation.findNavController(this, R.id.nav_host);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Realm.init(this);

        realmViewModel = ViewModelProviders.of(this).get(RealmViewModel.class);
        realmViewModel.createInstance();

        if (navController.getCurrentDestination().getId() != R.id.blankFragment) return;
        // Check if NDN Forwarding Daemon is installed.
        // If installed -> Continue Regular Onboarding
        // If not installed -> Show a Message and request memphis.myapplication.data.RealmObjects.UserRealm to install NDN Forwarding Daemon.
        final String nfdAppPackageName = getString(R.string.nfd_package);
        Context context = getApplicationContext();
        PackageManager pm = context.getPackageManager();
        if (isPackageInstalled(nfdAppPackageName, pm)) {
            // session checks sharedPreferences where we store our login boolean variable
            // if we're not logged in, start LoginFragment
            System.out.println("Logged in? " + SharedPrefsManager.getInstance(this).getLogInStatus());
            if (!SharedPrefsManager.getInstance(this).getLogInStatus()) {
                navController.navigate(R.id.action_blankFragment_to_loginFragment);
            }
            // we are logged in; take us to MainFragment
            else {
                backgroundViewModel = ViewModelProviders.of(this, new ViewModelProvider.Factory() {
                    @NonNull
                    @Override
                    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                        return (T) new BackgroundViewModel(getApplication());
                    }
                }).get(BackgroundViewModel.class);

                backgroundViewModel.toast().observe(this, new Observer<String>() {
                    @Override
                    public void onChanged(String s) {
                        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    }
                });

                backgroundViewModel.friendRequest().observe(this, new Observer<Interest>() {
                    @Override
                    public void onChanged(Interest interest) {
                        final FriendRequest friendRequest = new FriendRequest(interest, getApplicationContext());
                        friendRequest.receive();
                        friendRequest.addObserver(new java.util.Observer() {
                            @Override
                            public void update(Observable o, Object arg) {
                                int updateCode = (int) arg;
                                if (updateCode == 1) {
                                    runOnUiThread(new Thread(new Runnable() {
                                        public void run() {
                                            AlertDialog.Builder alert = new AlertDialog.Builder(getApplicationContext());
                                            alert.setTitle("Friend request");
                                            alert.setMessage("Accept friend request from " + friendRequest.getPendingFriend());
                                            alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    // Accept incoming friend request
                                                    try {
                                                        friendRequest.accept();
                                                    } catch (EncodingException e) {
                                                        e.printStackTrace();
                                                    }

                                                }
                                            });

                                            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    // reject incoming friend request
                                                    friendRequest.reject();
                                                }
                                            });

                                            alert.show();
                                        }
                                    }));
                                } else if (updateCode == 2) {
                                    Timber.d("Could not be verified");
                                    Toast.makeText(getApplicationContext(), "Received unverifiable friend request.", Toast.LENGTH_LONG).show();

                                } else if (updateCode == 3) {
                                    Timber.d("Already friends");
                                    Data data = new Data();
                                    data.setContent(new Blob("Friends"));
                                    try {
                                        Globals.face.putData(data);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else if (updateCode == 4) {
                                    Timber.d("Already trust");
                                    runOnUiThread(new Thread(new Runnable() {
                                        public void run() {
                                            AlertDialog.Builder alert = new AlertDialog.Builder(getApplicationContext());
                                            alert.setTitle("Friend request");
                                            alert.setMessage("Accept friend request from " + friendRequest.getPendingFriend());
                                            alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    // Accept incoming friend request
                                                    friendRequest.acceptTrusted();

                                                }
                                            });

                                            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    // reject incoming friend request
                                                    friendRequest.reject();
                                                }
                                            });

                                            alert.show();
                                        }
                                    }));
                                }
                            }
                        });
                    }
                });
                navController.navigate(R.id.action_blankFragment_to_mainFragment);
            }
        } else {

            Toast.makeText(context, "Please install NFD which is required for npChat to work.",
                    Toast.LENGTH_LONG).show();

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + nfdAppPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + nfdAppPackageName)));
            }
        }
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        boolean found = true;
        try {
            packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }
        return found;
    }
}

