package memphis.myapplication.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import net.named_data.jndn.encrypt.algo.EncryptAlgorithmType;
import net.named_data.jndn.encrypt.algo.EncryptParams;
import net.named_data.jndn.encrypt.algo.RsaAlgorithm;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import memphis.myapplication.Globals;
import timber.log.Timber;


public class SharedPrefsManager {
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGIN_STATUS = "login_status";
    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_KEY = "key";
    private static final String KEY_SEQ = "sequence";
    private static final String KEY_SHARE = "share";

    private String mUsername;
    private String mPassword;
    private Boolean mLogInStatus;
    private String mDomain;
    private String mKey;
    private Long mSeqNum;
    private Boolean mShareFriendsList;

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
        mKey = mSharedPreferences.getString(KEY_KEY, null);
        mSeqNum = mSharedPreferences.getLong(KEY_SEQ, 0);
        mShareFriendsList = mSharedPreferences.getBoolean(KEY_SHARE, true);
        mLogInStatus = mSharedPreferences.getBoolean(KEY_LOGIN_STATUS, false);
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }

    public SecretKey getKey() throws TpmBackEnd.Error {
        return Decrypter.decryptSymKey(Base64.decode(mKey, 0), Globals.tpm.getKeyHandle(Globals.pubKeyName));
    }


    public String getDomain() { return mDomain; }
  
    public String getNamespace() { return mDomain + "/npChat/" + mUsername; }
    public Boolean getLogInStatus() {
        return mLogInStatus;
    }

    public void generateKey() {
        if (mKey == null) {
            Encrypter encrypter = new Encrypter();
            SecretKey secretKey = encrypter.generateKey();
            try {
                byte[] encryptedKey = RsaAlgorithm.encrypt
                            (Globals.pubKeyBlob, new Blob(secretKey.getEncoded()), new EncryptParams(EncryptAlgorithmType.RsaOaep)).getImmutableArray();
                mKey = Base64.encodeToString(encryptedKey, 0);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putString(KEY_KEY, mKey);
                editor.apply();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateNewKey() {
        mKey = null;
        generateKey();
        Globals.producerManager.updateKey();
    }

    public long getSeqNum() {
        return mSeqNum;
    }

    public void setSeqNum(long s) {
        Timber.d("Setting seq num: " + s);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        mSeqNum = s;
        editor.putLong(KEY_SEQ, s);
        editor.apply();
    }

    public void shareFriendsList(boolean b) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        mShareFriendsList = b;
        editor.putBoolean(KEY_SHARE, b);
        Timber.d("Sharing friends");
        editor.apply();
    }

    public boolean getSharing() {
        return mShareFriendsList;
    }

    public void setCredentials(String username, String password, String domain) {
        mLogInStatus = true;
        mUsername = username;
        mPassword = password;
        mDomain = domain;
        mShareFriendsList = true;
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
