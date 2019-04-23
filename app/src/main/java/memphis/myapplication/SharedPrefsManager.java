package memphis.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.util.Base64;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;

import javax.crypto.SecretKey;

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
        mFriendsList = mSharedPreferences.getStringSet(KEY_FRIENDS_LIST, new HashSet<String>());

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
        Set<String> friendsList = mFriendsList;

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
        editor.putString(friend + "key", key);
        editor.apply();
    }

    public String getFriendKey(String friend) {
        return mSharedPreferences.getString(friend+"key", null);
    }

    public void storeFriendCert(String friend, CertificateV2 cert) {
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
        tlvEncodedDataContent.writeBuffer(cert.wireEncode().buf());
        byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
        String certString = Base64.encodeToString(finalDataContentByteArray, 0);
        editor.putString(friend, certString);
        editor.apply();

    }

    public CertificateV2 getFriendCert(String friend) {
        String certString = mSharedPreferences.getString(friend, null);
        byte[] certBytes = Base64.decode(certString, 0);
        CertificateV2 certificateV2 = null;
        try {
            certificateV2 = new CertificateV2();
            certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
            return certificateV2;
        } catch (EncodingException e) {
            e.printStackTrace();
        }
        return null;

    }

    public void saveSymKey(SecretKey secretKey, String filename) {
        byte[] keyBytes = secretKey.getEncoded();
        String keyString = Base64.encodeToString(keyBytes, Base64.NO_WRAP);
        SharedPreferences.Editor editor =  mSharedPreferences.edit();
        editor.putString(filename, keyString);
        System.out.println("Saving symkey: " + keyString);
        editor.apply();
    }

    public String getSymKey(String filename) {
        if (mSharedPreferences.contains(filename)) {
            return  mSharedPreferences.getString(filename, null);
        }
        else return null;
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
