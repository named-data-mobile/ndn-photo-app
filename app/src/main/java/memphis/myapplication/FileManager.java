package memphis.myapplication;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Base64;
import android.widget.Toast;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;

//import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.io.File;
import java.io.FileWriter;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class FileManager {

    private String m_appRootPath;
    private File m_friendsDir;
    private File m_selfDir;
    private File m_photosDir;
    private File m_filesDir;
    public static boolean dirsCreated = false;

    public FileManager(Context context) {
        m_appRootPath = context.getExternalFilesDir(null).toString();
        Log.d("m_appRootPath", m_appRootPath);
        m_friendsDir = new File(m_appRootPath, "/friends");
        Log.d("m_friendsDir", m_friendsDir.toString());
        m_selfDir = new File(m_appRootPath, "/self");
        Log.d("m_selfDir", m_selfDir.toString());
        m_photosDir = new File(m_appRootPath, "/photos");
        Log.d("m_photosDir", m_photosDir.toString());
        m_filesDir = new File(m_appRootPath, "/files");
        Log.d("m_filesDir", m_filesDir.toString());

        if(!dirsCreated) {
            createDirs();
        }
    }

    public String getAppRootPath() {
        return m_appRootPath;
    }

    public String getFriendsDir() {
        return m_friendsDir.toString();
    }

    public String getSelfDir() {
        return m_selfDir.toString();
    }

    public String getPhotosDir() {
        return m_photosDir.toString();
    }

    public String getFilesDir() {
        return m_filesDir.toString();
    }

    /**
     * Creates the necessary directories for app usage. This includes a friends directory where
     * each of the user's friends' public key and photos will be stored. It also includes a keys
     * directory to store the keypair of the user. And lastly there is a photos directory which
     * stores the user's own photos. This method is intended to be called once upon successful
     * signup for an account.
     *
     * @return Returns a boolean value indicating whether all directories were created or not.
     */
    private boolean createDirs() {

        Log.d("createDirs", m_friendsDir.toString());
        boolean madeFriends = (m_friendsDir).mkdir();
        boolean madeSelf = (m_selfDir).mkdir();
        boolean madePhotos = (m_photosDir).mkdir();
        boolean madeFiles = (m_filesDir).mkdir();
        String s = ("" + madeFriends + " " + madeSelf + " " + madePhotos + " " + madeFiles);
        Log.d("createDirs", s);
        dirsCreated = true;

        return (madeFriends && madeSelf && madePhotos && madeFiles);
    }

    /**
     * Creates a new RSA key pair. This is needed for encrypting/decrypting advertised names.
     * @return returns RSA key pair if successful; returns false if an error occurs
     */
    /*private KeyPair generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        }
        catch (NoSuchAlgorithmException e) {
            Log.d("QR", "RSA algorithm not found. Keys were not generated.");
            return null;
        }
    }*/

    /**
     * Saves the generated RSA key pair to file. This is intended to only be called once upon
     * a successful sign up, so our keys are not overwritten.
     * @return returns true if the keys were successfully written to file; false if anything else
     */
    // you might need to provide an easy way for someone to create new key pairs say in the case that
    // they deleted or lost their previous pair, or they uninstall/reinstall the app. Their friends
    // will need to know.
    /*protected boolean saveKeys() {
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

            File privateKeyFile = new File(m_selfDir, "/id_rsa");
            File pubKeyFile = new File(m_selfDir, "/id_rsa.pub");

            try {
                FileWriter writer = new FileWriter(privateKeyFile);
                writer.write("----BEGIN PRIVATE KEY----\n");
                writer.write(privateKey);
                writer.close();

                writer = new FileWriter(pubKeyFile);
                writer.write("----BEGIN PUBLIC KEY----\n");
                writer.write(pubKey);
                writer.close();
                return true;
            }
            catch (IOException e) {
                Log.d("saveKeys", "IOException: " + e.toString());
            }
        }
        return false;
    }*/

    /**
     * Saves your username to file so we can retrieve it later.
     * @param username is the chosen name of the user
     * @return whether the file operation was successful or not.
     */
    protected boolean saveUsername(String username) {
        File user = new File(m_selfDir + "/username");
        if (!user.exists()) {
            try {
                user.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter writer = new FileWriter(user);
            writer.write(username);
            writer.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves your username from file. Needed for namespacing.
     * @return String of username
     */
    public String getUsername() {
        File user = new File(m_selfDir + "/username");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(user));
            String username = reader.readLine();
            reader.close();
            return username;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves your personal QR code to a png file.
     * @param myInfo is the bitmap of our QR code we are saving.
     */
    public void saveMyQRCode(Bitmap myInfo) {
        File myQRFile = new File(getMyQRPath());
        try {
            FileOutputStream fostream = new FileOutputStream(myQRFile);
            myInfo.compress(Bitmap.CompressFormat.PNG, 90, fostream);
            fostream.close();
        } catch (FileNotFoundException e) {
            Log.d("saveYourself", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("saveYourself", "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Gives the path to your personal QR code.
     * @return String path to QR code
     */
    protected String getMyQRPath() {
        return (m_selfDir + "/myQR");
    }

    /**
     * Reads the public rsa key file and extracts the public key.
     * @return user's public key in string format
     */
    // public String getPubKey() {
    public net.named_data.jndn.security.certificate.PublicKey getPubKey() {
        //try {
            // may change implementation to work with file stored jndn security stuff
            // Globals.privateKeyStorage.getPublicKey
        // note to self!!!!!!!!!!!!!!!!!finish QR stuff now for add friend
        Name keyName = new Name("/ndn-snapchat/" + getUsername() + "/KEY");
        try {
            return Globals.privateKeyStorage.getPublicKey(new Name(keyName));
        }
        catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
            /* StringBuilder key = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(m_selfDir + "/id_rsa.pub"));
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
        }*/
    }

    public String getPrivateKey() {
        try {
            StringBuilder key = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(m_selfDir + "/id_rsa"));
            // first line of the public key file is ----BEGIN PRIVATE KEY---- so we need to skip it
            String line = reader.readLine();
            if (line != null) {
                line = reader.readLine();
            }

            while(line != null) {

                // and read the rest (assuming the keys are stored in separate files and the
                // first line is just ----BEGIN PRIVATE KEY----
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
    public int saveFriend(String friendContent) {
        if (friendContent.length() > 0) {
            int index = friendContent.indexOf(" ");
            String username = friendContent.substring(0, index);
            // A friend's filename will be their username. Another reason why we must ensure uniqueness
            File friendFile = new File(m_friendsDir + "/" + username);
            if (!friendFile.exists()) {
                return 1;
            }
            String pubKey = friendContent.substring(index + 1);
            try {
                boolean wasCreated = friendFile.createNewFile();
                if(wasCreated) {
                    FileWriter writer = new FileWriter(friendFile);
                    writer.write(username);
                    writer.write(pubKey);
                    writer.close();
                    return 0;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * Saves data we retrieved from SegmentFetcher to file.
     * @param content The blob of content we received upon Interest.
     * @param path The path of the file we will save it to.
     * @return whether the file operation was successful or not.
     */
    public boolean saveContentToFile(Blob content, String path) {
        String filename = path.substring(path.lastIndexOf("/")+1);
        File dir = new File(m_appRootPath + "/received_files");
        dir.mkdirs();
        File file = new File(m_appRootPath + "/received_files/" + filename);
        if(file.exists()) {
            boolean exists = true;
            int copyNum = 1;
            int fileTypeIndex = filename.lastIndexOf(".");
            while(exists) {
                file = new File(m_appRootPath + "/received_files/" +
                        filename.substring(0, fileTypeIndex) + "(" + copyNum + ")" +
                        filename.substring(fileTypeIndex));
                copyNum++;
                if(!file.exists()) {
                    exists = false;
                }
            }
        }
        byte[] byteContent = content.getImmutableArray();
        try {
            FileOutputStream fostream = new FileOutputStream(file);
            fostream.write(byteContent);
            fostream.close();
            return true;
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean saveFileQR(Bitmap bitmap, String path) {
        // do file operations here to remove .txt or .pdf or whatever and append .png
        int dotIndex = path.lastIndexOf(".");
        String filename = path.substring(path.lastIndexOf("/")+1, dotIndex) + ".png";
        File fileQR = new File(m_filesDir + "/" + filename);
        if (!fileQR.exists()) {
            try {
                fileQR.createNewFile();
                FileOutputStream fostream = new FileOutputStream(fileQR);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fostream);
                fostream.close();
                return true;
            } catch (FileNotFoundException e) {
                Log.d("saveFileQR", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("saveFileQR", "Error accessing file: " + e.getMessage());
            }
            return false;
        }
        return false;
    }

    // need to map files to one another; start off with full path where we append /ndn-snapchat/<username>
    // or remove /ndn-snapchat/<username>, depending on which way we are going. Then, incorporate a hash.

    // ok; this shows us the full path now. That was easy. Now let's do some hashing for security purposes.
    public String findFile(String providedPath) {
        // remove /ndn-snapchat/<username>, gives /path/to/file
        String trimmed = providedPath;
        int index = 0;
        try {
            for (int i = 0; i < 2; i++) {
                // we already know "/" is at index 0, but we need to find the 3rd slash
                index = trimmed.indexOf("/", index + 1);
            }
        }
        catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            Log.d("findFile", "IndexOutOfBoundsException. The provided path does not follow file naming convention.");
        }

        trimmed = trimmed.substring(index);
        return trimmed;

        // de-hash trimmed
        // return de-hashed trimmed

        // or we could serve from the same directory every time, so any file we are serving has either
        // a symbolic or direct link to the actual file location; how about we add a link to the
        // full path each time we need to publish the data?

        // maybe we hash and then we make a symbolic link based on the hash; this way we obscure
        // the path location, but there are no collisions in the common directory

    }

    /**
     * prepends "/ndn-snapchat/<username>" to file path; temporarily static for testing purposes
     * @param path the provided absolute path of the file
     * @return string of the form /ndn-snapchat/<username>/path/to/file
     */
    public String addAppPrefix(String path) {
        // we could also allow the user to state their own name which will attach to the end of
        // /ndn-snapchat/<username>/
        String username = this.getUsername();
        if (username != null) {
            // check that path already comes with "/" prepended
            if(path.charAt(0) == '/') {
                return "/ndn-snapchat/" + username + path;
            }
            else {
                return "/ndn-snapchat/" + username + "/" + path;
            }
        }
        else {
            return null;
        }
    }

    /**
     * removes the prefix "/ndn-snapchat/<username>" so we can find the file
     * @param fullname : "ndn-snapchat/<username>/path/to/file"
     * @return String of file path
     */
    public static String removeAppPrefix(String fullname) {
        int fileIndex = 0;
        String temp = fullname.substring(fileIndex);
        // name is of the form /ndn-snapchat/username/full-file-path; find third instance of "/"
        for(int i = 0; i < 3; i++) {
            fileIndex = temp.indexOf("/");
            temp = temp.substring(fileIndex + 1);
        }
        return temp;
    }

    // we need to hash to some common directory or we will need to have a table that contain links to the files
}
