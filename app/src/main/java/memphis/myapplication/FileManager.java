package memphis.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.util.Base64;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyPair;
import java.io.File;
import java.io.FileWriter;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static java.security.AccessController.getContext;

public class FileManager {

    private String m_appRootPath;
    private File m_friendsDir;
    private File m_keysDir;
    private File m_photosDir;

    /*public FileManager() {

    }*/

    public FileManager(View view) {
        //try {
            // m_appRootPath = view.getContext().getFilesDir().getPath();
            // this just worked and when I tried it one more time, it failed????? talking about createDirs below with these paths
            m_appRootPath = view.getContext().getExternalFilesDir(null).toString();
            Log.d("m_appRootPath", m_appRootPath);
            m_friendsDir = new File(m_appRootPath, "/friends");
            Log.d("m_friendsDir", m_friendsDir.toString());
            m_keysDir = new File(m_appRootPath, "/keys");
            Log.d("m_keysDir", m_keysDir.toString());
            m_photosDir = new File(m_appRootPath, "/photos");
            Log.d("m_photosDir", m_photosDir.toString());
        //}
        /*catch(IOException e) {
            Log.d("FileManager Constructor", e.toString());
        }*/
    }

    /**
     * Creates the necessary directories for app usage. This includes a friends directory where
     * each of the user's friends' public key and photos will be stored. It also includes a keys
     * directory to store the keypair of the user. And lastly there is a photos directory which
     * stores the user's own photos. This method is intended to be called once upon successful
     * signup for an account.
     *
     * @return Returns a boolean value indicating whether all 3 directories were created or not.
     */
    private boolean createDirs() {

        Log.d("createDirs", m_friendsDir.toString());
        boolean madeFriends = (m_friendsDir).mkdir();
        boolean madeKeys = (m_keysDir).mkdir();
        boolean madePhotos = (m_photosDir).mkdir();
        String s = ("" + madeFriends + " " + madeKeys + " " + madePhotos);
        Log.d("createDirs", s);

        return (madeFriends && madeKeys && madePhotos);
    }

    /**
     * Creates a new RSA key pair. This is needed for encrypting/decrypting advertised names.
     * @return returns RSA key pair if successful; returns false if an error occurs
     */
    private KeyPair generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        }
        catch (NoSuchAlgorithmException e) {
            Log.d("QR", "RSA algorithm not found. Keys were not generated.");
            return null;
        }
    }

    /**
     * Saves the generated RSA key pair to file. This is intended to only be called once upon
     * a successful sign up, so our keys are not overwritten.
     * @return returns true if the keys were successfully written to file; false if anything else
     */
    protected boolean saveKeys() {
        boolean wasCreated = createDirs();
        if (!wasCreated) {
            return false;
        }
        KeyPair keypair = generateKeys();
        if (keypair != null) {
            byte[] encodedPrivateKey = keypair.getPrivate().getEncoded();
            byte[] encodedPubKey = keypair.getPublic().getEncoded();

            // want to use string format of keys when I save to file
            String privateKey = Base64.encodeToString(encodedPrivateKey, 0);
            String pubKey = Base64.encodeToString(encodedPubKey, 0);

            File privateKeyFile = new File(m_keysDir, "/id_rsa");
            File pubKeyFile = new File(m_keysDir, "/id_rsa.pub");

            try {
                FileWriter writer = new FileWriter(privateKeyFile, true);
                writer.append("----BEGIN PRIVATE KEY----\n");
                writer.append(privateKey);
                writer.close();

                writer = new FileWriter(pubKeyFile, true);
                writer.append("----BEGIN PUBLIC KEY----\n");
                writer.append(pubKey);
                writer.close();
                return true;
            }
            catch (IOException e) {
                Log.d("saveKeys", "IOException: " + e.toString());
            }
        }
        return false;
    }

    protected boolean saveUsername() {
        return false;
    }

    protected String getUsername() {
        return "thisIsMe";
    }

    protected void saveYourself(Bitmap myInfo) {
        File myQR = new File(m_appRootPath + "/myQR");
        try {
            FileOutputStream fostream = new FileOutputStream(myQR);
            myInfo.compress(Bitmap.CompressFormat.PNG, 90, fostream);
            fostream.close();
        }
        catch (FileNotFoundException e) {
            Log.d("saveYourself", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("saveYourself", "Error accessing file: " + e.getMessage());
        }
    }

    protected String getYourself() {
        return (m_appRootPath + "/myQR");
    }
    /**
     * Reads the public rsa key file and extracts the public key.
     * @return user's public key in string format
     */
    public String getPubKey() {
        try {
            StringBuilder key = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(m_keysDir + "/id_rsa.pub"));
            // first line of the public key file is ----BEGIN PUBLIC KEY---- so we need to skip it
            String line = reader.readLine();
            if (line != null) {
                line = reader.readLine();
            }

            while(line != null) {

                // and read the rest (assuming the keys are stored in separate files and the
                // first line is just ----Begin Public Key----
                key.append(line);
                line = reader.readLine();
            }
            reader.close();
            // need to check this is not empty elsewhere
            return key.toString();
        }
        catch(IOException e) {
            Log.d("getPubKey", "IOException: " + e.toString());
            return null;
        }
    }

    // save friends
    public boolean saveFriend() {
        return false;
    }
}
