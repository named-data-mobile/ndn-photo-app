package memphis.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Session's intended use is to maintain login status and username.
 */
public class Session {

    private SharedPreferences preferences;

    public Session(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setLoginStatus() {
        preferences.edit().putBoolean("isLoggedIn", true).apply();
    }

    public boolean getLoginStatus() {
        // second param is default
        return preferences.getBoolean("isLoggedIn", false);
    }

    public void setUsername(String username) {
        preferences.edit().putString("username", username).apply();
    }

    public String getUsername() {
        // second param is default
        return preferences.getString("username", "");
    }
}
