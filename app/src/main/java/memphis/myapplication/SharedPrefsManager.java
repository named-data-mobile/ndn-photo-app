package memphis.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SharedPrefsManager {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGIN_STATUS = "login_status";
    private static final String KEY_FRIENDS_LIST = "friends_list";

    private String mUsername;
    private String mPassword;
    private Boolean mLogInStatus;
    private Set<String> mFriendsList;

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
        mLogInStatus = mSharedPreferences.getBoolean(KEY_LOGIN_STATUS, false);
        mFriendsList = mSharedPreferences.getStringSet(KEY_FRIENDS_LIST, null);
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public ArrayList<String> getFriendsList() {
        ArrayList<String> friends = new ArrayList<>();
        for (String friend : mFriendsList) {
            friends.add(friend);
        }
        return friends;
    }

    public boolean addFriend(String friend) {
        Set<String> friendsList = new HashSet<String>(mFriendsList);
        if (friendsList.contains(friend)) {
            return false;
        }
        friendsList.add(friend);
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.putStringSet(KEY_FRIENDS_LIST, friendsList);
        editor.apply();
        return true;
    }

    public void storeFriendKey(String friend, String key) {
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.putString(friend, key);
        editor.apply();
    }

    public String getFriendKey(String friend) {
        return mSharedPreferences.getString(friend, null);
    }

    public Boolean getLogInStatus() {
        return mLogInStatus;
    }

    public void setCredentials(String username, String password) {
        mLogInStatus = true;
        mUsername = username;
        mPassword = password;
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_LOGIN_STATUS, mLogInStatus);
        editor.apply();
    }

    public void removeCredentials(){
        mLogInStatus = false;
        mPassword = null;
        mUsername = null;
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_PASSWORD);
        editor.apply();
    }

}
