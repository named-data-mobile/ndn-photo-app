package memphis.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.util.Base64;
import android.util.Log;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

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

//    public ArrayList<String> getFriendsList() {
//        ArrayList<String> friends = new ArrayList<>();
//        for (String friend : mFriendsList) {
//            friends.add(friend);
//        }
//        return friends;
//    }
//
//    public boolean addFriend(String friend) {
//        Set<String> friendsList = mFriendsList;
//
//        if (friendsList.contains(friend)) {
//            return false;
//        }
//        friendsList.add(friend);
//
//        SharedPreferences.Editor editor =  mSharedPreferences.edit();
//        editor.putStringSet(KEY_FRIENDS_LIST, friendsList);
//        editor.apply();
//        return true;
//    }
//
//    public boolean checkFriend(String friend) {
//        return mFriendsList.contains(friend);
//    }
//
//    public Blob getFriendKey(String friend) throws CertificateV2.Error, EncodingException {
//        CertificateV2 cert = getFriendCert(friend);
//        Blob key = cert.getPublicKey();
//        return key;
//    }
//
//    public void storeFriendCert(String friend, CertificateV2 cert) {
//        SharedPreferences.Editor editor =  mSharedPreferences.edit();
//        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
//        tlvEncodedDataContent.writeBuffer(cert.wireEncode().buf());
//        byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
//        String certString = Base64.encodeToString(finalDataContentByteArray, 0);
//        editor.putString(friend, certString);
//        editor.apply();
//
//    }
//
//    public CertificateV2 getFriendCert(String friend) throws EncodingException {
//        String certString = mSharedPreferences.getString(friend, null);
//        byte[] certBytes = Base64.decode(certString, 0);
//        CertificateV2 certificateV2 = null;
//        certificateV2 = new CertificateV2();
//        certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
//        return certificateV2;
//
//    }
//
//    public void saveSymKey(SecretKey secretKey, String filename) {
//        byte[] keyBytes = secretKey.getEncoded();
//        String keyString = Base64.encodeToString(keyBytes, Base64.NO_WRAP);
//        SharedPreferences.Editor editor =  mSharedPreferences.edit();
//        editor.putString(filename, keyString);
//        editor.apply();
//    }
//
//    public String getSymKey(String filename) {
//        if (mSharedPreferences.contains(filename)) {
//            return  mSharedPreferences.getString(filename, null);
//        }
//        else return null;
//    }
//
//    public void saveSelfCert(CertificateV2 cert) {
//        Name certName = cert.getName();
//        String friend = certName.getSubName(4, 1).toString().substring(1) + "_self";
//        storeFriendCert(friend, cert);
//        Log.d("SharedPrefsManager", "Saved our own cert signed by " + friend);
//    }
//
//    public CertificateV2 getSelfCert(String friend) throws EncodingException {
//        friend = friend + "_self";
//        return getFriendCert(friend);
//    }
//
//    public boolean removeFriend(String friend) {
//        Set<String> friendsList = mFriendsList;
//
//        if (!friendsList.contains(friend)) {
//            return false;
//        }
//        friendsList.remove(friend);
//
//        SharedPreferences.Editor editor =  mSharedPreferences.edit();
//        editor.putStringSet(KEY_FRIENDS_LIST, friendsList);
//        editor.apply();
//        return true;
//    }

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
