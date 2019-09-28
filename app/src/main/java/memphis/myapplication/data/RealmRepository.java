package memphis.myapplication.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmException;
import memphis.myapplication.Globals;
import memphis.myapplication.data.RealmObjects.FilesInfo;
import memphis.myapplication.data.RealmObjects.FilesInfoRealm;
import memphis.myapplication.data.RealmObjects.PublishedContent;
import memphis.myapplication.data.RealmObjects.PublishedContentRealm;
import memphis.myapplication.data.RealmObjects.SavedSyncDataRealm;
import memphis.myapplication.data.RealmObjects.SelfCertificate;
import memphis.myapplication.data.RealmObjects.SelfCertificateRealm;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.data.RealmObjects.UserRealm;
import memphis.myapplication.utilities.Decrypter;
import timber.log.Timber;

/**
 * RealmRepository handles all realm database related stuff for friends, certificates and files data
 */
public class RealmRepository {

    private static RealmRepository instance;
    public Realm realm;
    private static MutableLiveData<List<String>> friends;
    private static MutableLiveData<String> toastData;

    private RealmRepository() {
        realm = Realm.getDefaultInstance();
        if (toastData == null)
            toastData = new MutableLiveData<>();
    }
    /**
    * Create or get a single common instance of RealmRepository
     * @return The RealmRepository instance
     */
    public static RealmRepository getInstance() {
        if (instance == null) {
            instance = new RealmRepository();

        }
        return instance;
    }

    /**
     * Get a new RealmRepository instance for non UI threads. Be sure to call the close method
     * after interacting with the Database
     * @return A new RealmRepository instance
     */
    public static RealmRepository getInstanceForNonUI() {
        return new RealmRepository();
    }

    /**
     * Create the common instance to be used later
     */
    public void createInstance() {
        if (realm.isClosed()) {
            realm = Realm.getDefaultInstance();
        }
    }

    /**
     * Set friendship with a user
     * @param friendName
     * @return the friend User object
     */
    public User setFriendship(String friendName) {
        realm.beginTransaction();
        UserRealm friend = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        friend.setFriend(true);
        User user = userRealmToUser(friend);
        realm.commitTransaction();
        return user;
    }

    /**
     * Check the friendship status of a user
     * @param user
     */
    public int checkFriendship(String user) {
        realm.beginTransaction();
        UserRealm friend = realm.where(UserRealm.class).equalTo("username", user).findFirst();
        if (friend == null) {
            realm.cancelTransaction();
            return -1;
        } else if (friend.isFriend()) {
            realm.cancelTransaction();
            return 1;
        } else if (friend.haveTrust()) {
            realm.cancelTransaction();
            return 3;
        }
        realm.cancelTransaction();
        return -1;
    }

    public User saveNewFriend(String friendName, String friendDomain, CertificateV2 certificateV2) {
        realm.beginTransaction();
        UserRealm friend = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        if (friend == null)
            friend = realm.createObject(UserRealm.class, friendName);

        if (friendDomain != null)
            friend.setDomain(friendDomain);
        if (certificateV2 != null)
            friend.setCert(certificateV2);

        User user = userRealmToUser(friend);
        realm.commitTransaction();
        return user;
    }

    public User saveNewFriend(String friendName, boolean trust, CertificateV2 certificateV2) {
        realm.beginTransaction();
        UserRealm friend = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        if (friend == null)
            friend = realm.createObject(UserRealm.class, friendName);

        if (certificateV2 != null)
            friend.setCert(certificateV2);
        friend.setFriend(true);
        friend.setTrust(trust);

        User user = userRealmToUser(friend);

        if (friends != null && friends.getValue() != null) {
            friends.getValue().add(user.getUsername());
            friends.postValue(friends.getValue());
        }

        realm.commitTransaction();
        return user;
    }

    /**
     * Get the list of all friends of the user
     */
    public ArrayList<User> getAllFriends() {
        ArrayList<User> users = new ArrayList<>();
        realm.beginTransaction();
        RealmResults<UserRealm> userRealm = realm.where(UserRealm.class).equalTo("friend", true).findAll();
        for (UserRealm u : userRealm) {
            users.add(userRealmToUser(u));
        }
        realm.commitTransaction();
        return users;
    }

    /**
     * Get the dynamic list of all the friends of the users
     * @return Livedata for the list
     */
    public LiveData<List<String>> observeAllFriends() {
        if (friends == null) {
            friends = new MutableLiveData<>();
            List<String> friendsList = new ArrayList<>();
            for (User f : getAllFriends())
                friendsList.add(f.getUsername());
            friends.setValue(friendsList);
        }
        return friends;
    }

    /**
     * Get the list of all the users discovered but not added as friend
     */
    public ArrayList<User> getPotentialFriends() {
        ArrayList<User> users = new ArrayList<>();
        realm.beginTransaction();
        RealmResults<UserRealm> potentialFriends = realm.where(UserRealm.class).equalTo("friend", false).findAll();
        for (UserRealm u : potentialFriends) {
            users.add(userRealmToUser(u));
        }
        realm.commitTransaction();
        return users;
    }

    /**
     * Get the list of all the friends of a friend
     */
    public ArrayList<String> getFriendsofFriend(String friendName) {
        realm.beginTransaction();
        UserRealm newFriend = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        User user = userRealmToUser(newFriend);
        realm.commitTransaction();
        return user.getFriends();
    }

    /**
     * Get the list of all the trusted friends of the user
     * @return
     */
    public ArrayList<User> getTrustedFriends() {
        realm.beginTransaction();
        RealmResults<UserRealm> trustedFriends = realm.where(UserRealm.class).equalTo("trust", true).findAll();
        ArrayList<User> users = new ArrayList<>();
        for (UserRealm u : trustedFriends) {
            users.add(userRealmToUser(u));
        }
        realm.commitTransaction();
        return users;
    }

    /**
     * Delete friendship with a friend
     */
    public User deleteFriendship(String friend) {
        realm.beginTransaction();
        UserRealm userRealm = realm.where(UserRealm.class).equalTo("username", friend).findFirst();
        userRealm.setFriend(false);
        User user = userRealmToUser(userRealm);
        if (friends != null && friends.getValue() != null && friends.getValue().contains(user.getUsername())) {
            friends.getValue().remove(user.getUsername());
            friends.setValue(friends.getValue());
        }
        realm.commitTransaction();
        return user;
    }

    /**
     * Get the details of a friend
     */
    public User getFriend(String friendName) {
        realm.beginTransaction();
        User user = userRealmToUser(realm.where(UserRealm.class).equalTo("username", friendName).findFirst());
        realm.commitTransaction();
        return user;
    }

    /**
     * Get our certificate signed by a friend
     * @param friendName
     */
    public SelfCertificate getFriendCert(String friendName) {
        realm.beginTransaction();
        SelfCertificate selfCertificate = selfCertificateRealmToSelfCertificate(realm.where(SelfCertificateRealm.class).equalTo("username", friendName).findFirst());
        realm.commitTransaction();
        return selfCertificate;
    }

    /**
     * Set symmetric mutual keys for a friend
     */
    public void setSymKey(String friendName, byte[] key) {
        realm.beginTransaction();
        UserRealm friend = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        friend.setSymKey(key);
        realm.commitTransaction();
    }

    /**
     * Get symmetric mutual keys for a friend
     * @param friendName
     * @return
     */
    public SecretKey getSymKey(String friendName) {
        try {
            realm.beginTransaction();
            User user = userRealmToUser(realm.where(UserRealm.class).equalTo("username", friendName).findFirst());
            byte[] encryptedKey = user.getSymKey();
            realm.commitTransaction();
            return Decrypter.decryptSymKey(encryptedKey, Globals.tpm.getKeyHandle(Globals.pubKeyName));
        } catch (TpmBackEnd.Error error) {
            realm.commitTransaction();
            error.printStackTrace();
            return null;
        }
    }


    /**
     * Save our certificate signed by a friend
     */
    public void setFriendCert(String friendName, CertificateV2 certificateV2) {
        realm.beginTransaction();
        SelfCertificateRealm realmCertificate = realm.where(SelfCertificateRealm.class).equalTo("username", friendName).findFirst();
        if (realmCertificate == null) {
            realmCertificate = realm.createObject(SelfCertificateRealm.class, friendName);
        }
        realmCertificate.setCert(certificateV2);

        realm.commitTransaction();
    }

    /**
     * Get PublishedContent info for a file
     */
    public PublishedContent getPublishedContent(String filename) {
        realm.beginTransaction();
        PublishedContent publishedContent = publishedContentRealmTopublishedContent(realm.where(PublishedContentRealm.class).equalTo("filename", filename).findFirst());
        realm.commitTransaction();
        return publishedContent;
    }

    /**
     * Add secret key for a shared file
     */
    public void addKey(String path, SecretKey secretKey) {
        realm.beginTransaction();
        PublishedContentRealm contentKey = realm.createObject(PublishedContentRealm.class, path);
        contentKey.addKey(secretKey);
        realm.commitTransaction();
    }

    /**
     * Check if a file with the path was shared before
     */
    public PublishedContent checkIfShared(String path) {
        realm.beginTransaction();
        PublishedContentRealm contentKey = realm.where(PublishedContentRealm.class).equalTo("filename", path).findFirst();
        realm.commitTransaction();

        if (contentKey != null) {
            return publishedContentRealmTopublishedContent(contentKey);
        } else {
            return null;
        }
    }

    /**
     * Add a friend of friend
     * @param friend the friend
     * @param friendsFriend the friend of friend
     * @return friend's user data
     */
    public User addFriendToUser(String friend, String friendsFriend) {
        realm.beginTransaction();
        UserRealm sharingUser = realm.where(UserRealm.class).equalTo("username", friend).findFirst();
        sharingUser.addFriend(friendsFriend);

        User user = userRealmToUser(sharingUser);
        realm.commitTransaction();

        return user;
    }

    /**
     * Save meta information of the received files
     * @param filename The name of the file
     * @param isFeed true if the file is a public feed, false if shared to particular group of users
     * @param location true if the file contains location information
     * @param isFile true if the data is a file, false when its a story-picture
     * @param producer the name of the producer
     */
    public void saveNewFile(String filename, boolean isFeed, boolean location, boolean isFile, String producer) {
        realm.beginTransaction();
        FilesInfoRealm filesInfoRealm = realm.where(FilesInfoRealm.class).equalTo("filename", filename).findFirst();
        if (filesInfoRealm == null) {
            filesInfoRealm = realm.createObject(FilesInfoRealm.class, filename);
            filesInfoRealm.setProducer(producer);

            filesInfoRealm.setFeed(isFeed);
            filesInfoRealm.setLocation(location);
            filesInfoRealm.setFile(isFile);
        } else {
            filesInfoRealm.setProducer(producer);

            filesInfoRealm.setFeed(isFeed);
            filesInfoRealm.setLocation(location);
            filesInfoRealm.setFile(isFile);
        }

        realm.commitTransaction();
    }


    /**
     * Save meta information of the received files
     */
    public void saveNewFile(FilesInfo filesInfo) {
        realm.beginTransaction();
        FilesInfoRealm filesInfoRealm = realm.where(FilesInfoRealm.class).equalTo("filename", filesInfo.filename).findFirst();
        if (filesInfoRealm == null) {
            filesInfoRealm = realm.createObject(FilesInfoRealm.class, filesInfo.filename);
        }

        filesInfoRealm.setProducer(filesInfo.producer);
        filesInfoRealm.setFilePath(filesInfo.filePath);

        filesInfoRealm.setFeed(filesInfo.feed);
        filesInfoRealm.setLocation(filesInfo.location);

        realm.commitTransaction();
    }

    /**
     * Get the meta information of a file from the file name
     */
    public FilesInfo getFileInfo(String filename) {
        realm.beginTransaction();
        FilesInfoRealm filesInfoRealm = realm.where(FilesInfoRealm.class).equalTo("filename", filename).findFirst();
        FilesInfo filesInfo = null;
        if (filesInfoRealm != null) {
            filesInfo = fileInfoRealmToFileInfo(filesInfoRealm);
        }

        realm.commitTransaction();
        return filesInfo;
    }

    /**
     * Get the meta information of a file from the file path
     */
    public FilesInfo getFileInfoFromPath(String filePath) {
        realm.beginTransaction();
        FilesInfoRealm filesInfoRealm = realm.where(FilesInfoRealm.class).equalTo("filePath", filePath).findFirst();
        FilesInfo filesInfo = null;
        if (filesInfoRealm != null) {
            filesInfo = fileInfoRealmToFileInfo(filesInfoRealm);
        }

        realm.commitTransaction();
        return filesInfo;
    }

    /**
     * Delete file's meta information
     */
    public void deleteFileInfo(String filename) {
        realm.beginTransaction();
        RealmResults<FilesInfoRealm> filesInfoRealms = realm.where(FilesInfoRealm.class).equalTo("filename", filename).findAll();
        if (filesInfoRealms != null) {
            filesInfoRealms.deleteAllFromRealm();
        }

        realm.commitTransaction();
    }

    /**
     * Convert UserRealm object to User object
     */
    public static User userRealmToUser(UserRealm userRealm) {
        User user = new User();
        if (userRealm == null) return user;
        user.setUsername(userRealm.getUsername());
        user.setTrust(userRealm.haveTrust());
        user.setDomain(userRealm.getDomain());
        user.setCert(userRealm.getCertByreArray());
        user.setFriend(userRealm.isFriend());
        user.setSymKey(userRealm.getSymKey());
        user.setFriends(userRealm.getFriends());
        user.setSeqNo(userRealm.getSeqNo());
        return user;
    }

    /**
     * Convert selfCertificateRealm object to SelfCertificate object
     */
    public static SelfCertificate selfCertificateRealmToSelfCertificate(SelfCertificateRealm selfCertificateRealm) {
        SelfCertificate selfCertificate = new SelfCertificate();
        if (selfCertificateRealm == null) return selfCertificate;

        selfCertificate.setCert(selfCertificateRealm.getCertInByte());
        return selfCertificate;
    }

    /**
     * Convert publishedContentRealm object to PublishedContent object
     */
    public static PublishedContent publishedContentRealmTopublishedContent(PublishedContentRealm publishedContentRealm) {
        PublishedContent publishedContent = new PublishedContent();
        if (publishedContentRealm == null) return publishedContent;

        publishedContent.setKey(publishedContentRealm.getKey());
        publishedContent.setFilename(publishedContentRealm.getFilename());

        return publishedContent;
    }

    /**
     * Convert FilesInfoRealm object to FilesInfo object
     */
    public static FilesInfo fileInfoRealmToFileInfo(FilesInfoRealm filesInfoRealm) {
        FilesInfo filesInfo = new FilesInfo();
        filesInfo.filename = filesInfoRealm.getFilename();
        filesInfo.filePath = filesInfoRealm.getFilePath();
        filesInfo.producer = filesInfoRealm.getProducer();
        filesInfo.feed = filesInfoRealm.isFeed();
        filesInfo.location = filesInfoRealm.isLocation();
        filesInfo.isFile = filesInfoRealm.isFile();

        return filesInfo;
    }

    /**
     * Save the syncData for a seq
     */
    public void saveSyncData(long seq, String syncData) {
        realm.beginTransaction();
        SavedSyncDataRealm savedSyncDataRealm = realm.where(SavedSyncDataRealm.class).equalTo("seqNum", seq).findFirst();
        if (savedSyncDataRealm == null) {
            savedSyncDataRealm = realm.createObject(SavedSyncDataRealm.class, seq);
        }
        savedSyncDataRealm.setSyncData(syncData);
        realm.commitTransaction();
    }

    /**
     * Get the syncData for the seq
     */
    public String getSyncData(long seq) {
        realm.beginTransaction();
        SavedSyncDataRealm savedSyncDataRealm = realm.where(SavedSyncDataRealm.class).equalTo("seqNum", seq).findFirst();
        realm.commitTransaction();
        if (savedSyncDataRealm == null)
            throw new RealmException("No seqNum");
        return savedSyncDataRealm.getSyncData();
    }

    public long getSeqNo(String friendName) {
        realm.beginTransaction();
        User user = userRealmToUser(realm.where(UserRealm.class).equalTo("username", friendName).findFirst());
        realm.commitTransaction();
        return user.getSeqNo();
    }

    public void setSeqNo(String friendName, long seq) {
        realm.beginTransaction();
        Timber.d("Setting seq no: " + seq);
        UserRealm user = realm.where(UserRealm.class).equalTo("username", friendName).findFirst();
        user.setSeqNo(seq);
        realm.commitTransaction();
    }

    /**
     * End a Realm instance
     */
    public void close() {
        realm.close();
    }

    public MutableLiveData<String> toast() {
        return toastData;
    }
}
