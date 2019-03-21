package memphis.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class IntroActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String nfdAppPackageName = getString(R.string.nfd_package);
        Context context = getApplicationContext();
        Session session = new Session(context);

        // Check if NDN Forwarding Daemon is installed.
        // If installed -> Continue Regular Onboarding
        // If not installed -> Show a Message and request User to install NDN Forwarding Daemon.
        PackageManager pm = context.getPackageManager();
        if (isPackageInstalled(nfdAppPackageName,pm))
        {
            // session checks sharedPreferences where we store our login boolean variable
            // if we're not logged in, start LoginActivity
            if (!session.getLoginStatus()) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
            // we are logged in; take us to MainActivity
            else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
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

