package memphis.myapplication.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class SharedPrefsManager {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGIN_STATUS = "login_status";
    private static final String KEY_DOMAIN = "domain";

    private String mUsername;
    private String mPassword;
    private Boolean mLogInStatus;
    private String mDomain;

    private SharedPreferences mSharedPreferences;
    private static SharedPrefsManager sharedPrefsManager;

    public static SharedPrefsManager getInstance(Context context) {
        if (sharedPrefsManager == null) {
            sharedPrefsManager = new SharedPrefsManager(context);
        }
        return sharedPrefsManager;
    }
    private SharedPrefsManager(Context context){
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mUsername = mSharedPreferences.getString(KEY_USERNAME, null);
        mPassword = mSharedPreferences.getString(KEY_PASSWORD, null);
        mDomain = mSharedPreferences.getString(KEY_DOMAIN, null);
        mLogInStatus = mSharedPreferences.getBoolean(KEY_LOGIN_STATUS, false);

    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }


    public String getDomain() { return mDomain; }
  
    public String getNamespace() { return mDomain + "/npChat/" + mUsername; }
    public Boolean getLogInStatus() {
        return mLogInStatus;
    }

    public void setCredentials(String username, String password, String domain) {
        mLogInStatus = true;
        mUsername = username;
        mPassword = password;
        mDomain = domain;
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putString(KEY_DOMAIN, domain);
        editor.putBoolean(KEY_LOGIN_STATUS, mLogInStatus);
        editor.apply();
    }

    public boolean contains(String key) {
        if (mSharedPreferences.contains(key)) {
            return true;
        }
        return false;
    }

    public void removeCredentials(){
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

}
