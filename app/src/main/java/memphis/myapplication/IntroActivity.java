package memphis.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import memphis.myapplication.utilities.SharedPrefsManager;

public class IntroActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        NavController navController = Navigation.findNavController(this, R.id.nav_host);

        if(navController.getCurrentDestination().getId()!=R.id.blankFragment) return;
        // Check if NDN Forwarding Daemon is installed.
        // If installed -> Continue Regular Onboarding
        // If not installed -> Show a Message and request memphis.myapplication.data.RealmObjects.User to install NDN Forwarding Daemon.
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

