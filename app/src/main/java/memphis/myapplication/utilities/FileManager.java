package memphis.myapplication.utilities;

import android.content.Context;
import android.graphics.Bitmap;

import memphis.myapplication.Globals;
import memphis.myapplication.R;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import memphis.myapplication.data.RealmObjects.SelfCertificate;
import memphis.myapplication.data.RealmObjects.User;

public class FileManager {

    private String m_appName;
    private String m_appRootPath;
    private static File m_friendsDir;
    private File m_selfDir;
    private File m_photosDir;
    private File m_profilePhoto;
    private File m_filesDir;
    private File m_rcvdFilesDir;
    private File m_rcvdPhotosDir;
    private static Context mContext;
    public static boolean dirsCreated = false;
    private static Realm mRealm;

    public FileManager(Context context) {
        /* Eventually, we will set m_appRootPath to getFilesDir(). This is the internal storage of
           the phone. It is inaccessible by other applications. getExternalFilesDir() is on the
           SD card, which allows us to browse these directories with a File Explorer. This is better
           for testing, since you can verify file activity results have occurred. In the Release,
           though, we will not want people to view files outside our app given the Snapchat philosophy
           of destroying content after viewing.
         */
        // m_appRootPath = context.getFilesDir().toString();
        mContext = context;
        m_appRootPath = context.getExternalFilesDir(null).toString();
        m_friendsDir = new File(m_appRootPath, "/friends");
        m_selfDir = new File(m_appRootPath, "/self");
        m_photosDir = new File(m_appRootPath, "/photos");
        m_filesDir = new File(m_appRootPath, "/files");
        m_rcvdFilesDir = new File(m_appRootPath, "/received_files");
        m_rcvdPhotosDir = new File(m_appRootPath, "/received_photos");

        m_appName = context.getString(R.string.app_name);

        if(!dirsCreated) {
            createDirs();
        }

        m_profilePhoto = new File(m_photosDir, "profilePhoto.jpg");
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

    public File getProfilePhoto() {return m_profilePhoto; }

    public String getFilesDir() {
        return m_filesDir.toString();
    }

    public String getRcvdFilesDir() {return m_rcvdFilesDir.toString(); }

    public String getRcvdPhotosDir() {return m_rcvdPhotosDir.toString(); }

    public void setProfilePhoto(Bitmap bitmap) {
        try{
            FileOutputStream stream = new FileOutputStream(m_profilePhoto);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.flush();
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
    private void createDirs() {

        boolean madeFriends = (m_friendsDir).mkdir();
        boolean madeSelf = (m_selfDir).mkdir();
        boolean madePhotos = (m_photosDir).mkdir();
        boolean madeFiles = (m_filesDir).mkdir();
        boolean madeRcvdFiles = (m_rcvdFilesDir).mkdir();
        boolean madeRcvdPhotos = (m_rcvdPhotosDir).mkdir();
        String s = ("" + madeFriends + " " + madeSelf + " " + madePhotos + " " + madeFiles + " " + madeRcvdFiles
                    + " " + madeRcvdPhotos);
        Timber.d(s);
        dirsCreated = true;
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
            Timber.d("saveYourself: %s", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Timber.d("saveYourself: %s", "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * Gives the path to your personal QR code.
     * @return String path to QR code
     */
    public String getMyQRPath() {
        return (m_selfDir + "/myQR.png");
    }


    /**
     * Saves data we retrieved from SegmentFetcher to file. This handles photos and other file types
     * slightly differently. Photos are saved to their own special directory, so we can query them
     * separately later for viewing and subsequent destruction.
     * @param content The blob of content we received upon Interest.
     * @param path The path of the file we will save it to.
     * @return whether the file operation was successful or not.
     */

    public boolean saveContentToFile(Blob content, Name path) {
        String filename = path.getSubName(-1).toUri().substring(1);
        Timber.d( "Saving " + filename);
        File dir;
        File file;
        int fileTypeIndex = filename.lastIndexOf(".");
        // if the data name has the .png file extension, save it in the received photos directory
        if(filename.substring(fileTypeIndex).equals(".jpg") || filename.substring(fileTypeIndex).equals(".png")) {
            dir = m_rcvdPhotosDir;
            int npChatComp = 0;
            for (int i = 0; i <= path.size(); i++) {
                if (path.getSubName(i, 1).toUri().equals("/npChat")){
                    npChatComp = i;
                    break;
                }
            }
            String friend = path.getSubName(npChatComp + 1, 1).toUri().substring(1);
            file = new File(m_rcvdPhotosDir + "/" + friend + "_" + filename);
        }
        // else save to the received files directory
        else {
            dir = m_rcvdFilesDir;
            file = new File(m_rcvdFilesDir + "/" + filename);
        }

        // check if the file exists. If so, save a copy and indicate it is a copy in the name by
        // using a number. Example: filename(1).txt
        if (file.exists()) {
            boolean exists = true;
            int copyNum = 1;
            while (exists) {
                file = new File(dir + "/" +
                        filename.substring(0, fileTypeIndex) + "(" + copyNum + ")" +
                        filename.substring(fileTypeIndex));
                copyNum++;
                if (!file.exists()) {
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
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Browses the received files directory and looks for photos and adds them to an ArrayList
     * specific to each user who sent content. Each ArrayList is added to a ConcurrentHashMap with the
     * username as the key.
     * @return the ConcurrentHashMap of photos sent by friends
     */
    public synchronized ConcurrentHashMap<String, ArrayList<String>> getReceivedPhotos() {
        File[] files = m_rcvdPhotosDir.listFiles();
        // let me change how I save these files first and then we'll know how to parse to get friend
        // wait a second. just use the file extension (.png)
        ConcurrentHashMap<String, ArrayList<String>> map = new ConcurrentHashMap<>();
        for(File file : files) {
            String filename = file.getName();
            Timber.d("getReceivedPhotos: %s", filename);
            String key = filename.substring(0, filename.indexOf('_'));
            ArrayList<String> list = map.get(key);

            if(list == null || list.isEmpty()) {
                list = new ArrayList<>();
                list.add(file.getAbsolutePath());
                map.put(key, list);
            }
            else {
                list.add(file.getAbsolutePath());
            }
        }
        return map;
    }

    /**
     * Saves the Name of your file in a QR code. You can present this code to a friend to scan for
     * easy retrieval.
     * @param bitmap provided bitmap to form png of QR code
     * @param path provided filepath; we will use this to name the file after changing its type to png
     * @return true or false depending on success
     */
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
                Timber.d("File not found: " + e.getMessage());
            } catch (IOException e) {
                Timber.d("Error accessing file: " + e.getMessage());
            }
            return false;
        }
        return false;
    }

    // need to map files to one another; start off with full path where we prepend /npChat/<username>
    // or remove /npChat/<username>, depending on which way we are going. Then, incorporate a hash.

    // ok; this shows us the full path now. That was easy. Now let's do some hashing for security purposes.
    public String findFile(String providedPath) {
        // remove /npChat/<username>, gives /path/to/file
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
            Timber.d("IndexOutOfBoundsException. The provided path does not follow file naming convention.");
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
     * prepends "/npChat/<username>" to file path; temporarily static for testing purposes
     * @param path the provided absolute path of the file
     * @return string of the form /npChat/<username>/path/to/file
     */
    public String addAppPrefix(String path) {
        // we could also allow the user to state their own name which will attach to the end of
        // /npChat/<username>/
        String username = SharedPrefsManager.getInstance(mContext).getUsername();
        if (username != null) {
            // check that path already comes with "/" prepended
            if(path.charAt(0) == '/') {
                return "/" + m_appName + "/" + username + path;
            }
            else {
                return "/" + m_appName + "/" + username + "/" + path;
            }
        }
        else {
            return null;
        }
    }

    /**
     * removes the prefix "/npChat/<username>" so we can find the file
     * @param fullname : "npChat/<username>/path/to/file"
     * @return String of file path
     */
    public static String removeAppPrefix(String fullname) throws UnsupportedEncodingException {
        int fileIndex = 0;
        String temp = fullname.substring(fileIndex);
        // name is of the form /npChat/username/full-file-path; find third instance of "/"
        for(int i = 0; i < 3; i++) {
            fileIndex = temp.indexOf("/");
            temp = temp.substring(fileIndex + 1);
        }
        temp = URLDecoder.decode(temp, "UTF-8");
        return temp;
    }

    private String parsePathForFriend(String path) {
        int fileIndex = 1;
        String temp = path.substring(fileIndex);
        // path is of the form /npChat/username/full-file-path; extract username
        int firstIndex = temp.indexOf("/") + 1;
        temp = temp.substring(firstIndex);
        int lastIndex = temp.indexOf("/");
        return temp.substring(0, lastIndex);
    }

    public static final OnInterestCallback onCertInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            mRealm = Realm.getDefaultInstance();
            Timber.d("Called onCertInterest with Interest: %s", interest.getName().toUri());
            String issuer = interest.getName().getSubName(-5, 1).toString().substring(1);
            String signer = interest.getName().getSubName(-2, 1).toString().substring(1);
            Timber.d("Signer: %s", signer);
            Timber.d("Issuer: %s", issuer);

            // Interest for our self-signed cert
            if ((signer.equals(SharedPrefsManager.getInstance(mContext).getUsername()) && issuer.equals(signer))) {
                Timber.d("Asking for our self-signed cert");
                try {
                    CertificateV2 cert = Globals.pibIdentity.getDefaultKey().getDefaultCertificate();
                    Timber.d(cert.getName().toUri());

                    Data data = new Data(interest.getName());
                    TlvEncoder tlvEncodedDataContent = new TlvEncoder();
                    tlvEncodedDataContent.writeBuffer(cert.wireEncode().buf());
                    byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
                    Blob d = new Blob(finalDataContentByteArray);
                    data.setContent(d);
                    data.setMetaInfo(new MetaInfo());
                    data.getMetaInfo().setFreshnessPeriod(31536000000.0);

                    face.putData(data);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // Interest for their cert signed by us
            // <ourPrefix>/cert/<theirPrefix>/KEY/<keyID>/<our_username>/<version>
            else if (signer.equals(SharedPrefsManager.getInstance(mContext).getUsername())) {
                Timber.d("Asking for their own cert");
                String friend = (interest.getName().getSubName(-5, 1)).toString().substring(1);
                Timber.d("Friend name %s", friend);
                CertificateV2 friendCert = null;
                try {
                    friendCert = mRealm.where(User.class).equalTo("username", friend).findFirst().getCert();
                    Data data = new Data(interest.getName());
                    TlvEncoder tlvEncodedDataContent = new TlvEncoder();
                    tlvEncodedDataContent.writeBuffer(friendCert.wireEncode().buf());
                    byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
                    Blob d = new Blob(finalDataContentByteArray);
                    data.setContent(d);
                    data.setMetaInfo(new MetaInfo());
                    data.getMetaInfo().setFreshnessPeriod(31536000000.0);
                    Timber.d("Their cert name signed by us %s", friendCert.getName().toUri());
                    face.putData(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                catch (EncodingException e) {
                    e.printStackTrace();
                }

                // Interest for our cert signed by mutual friend
                //  <ourPrefix>/cert/<ourPrefix>/KEY/<keyID>/<mutual_friend_username>/<version>
            }else {
                try {
                    Timber.d("Asking for our cert signed by mutual friend");
                    int friendNameComp = 0;
                    for (int i = 0; i<=interest.getName().size(); i++) {
                        if (interest.getName().getSubName(i, 1).toUri().equals("/KEY")) {
                            friendNameComp = i+2;
                            break;
                        }
                    }
                    String friend = interest.getName().getSubName(-2, 1).toString().substring(1);
                    CertificateV2 cert = mRealm.where(SelfCertificate.class).equalTo("username", friend).findFirst().getCert();
                    Timber.d(cert.getName().toUri());

                    Data data = new Data(interest.getName());
                    TlvEncoder tlvEncodedDataContent = new TlvEncoder();
                    tlvEncodedDataContent.writeBuffer(cert.wireEncode().buf());
                    byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
                    Blob d = new Blob(finalDataContentByteArray);
                    data.setContent(d);
                    data.setMetaInfo(new MetaInfo());
                    data.getMetaInfo().setFreshnessPeriod(31536000000.0);

                    face.putData(data);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    };



            // we need to hash to some common directory or we will need to have a table that contain links to the files
}
