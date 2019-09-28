package memphis.myapplication.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import net.named_data.jndn.security.v2.CertificateV2;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import memphis.myapplication.data.RealmObjects.PublishedContent;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.data.RealmRepository;

/**
 * RealmViewModel is a ViewModel to connect RealmRepository database to the UI
 */
public class RealmViewModel extends ViewModel {
    public final RealmRepository repository;

    public LiveData<String> toast() {
        return repository.toast();
    }

    public RealmViewModel() {
        repository = RealmRepository.getInstance();
    }

    public void createInstance(){
        repository.createInstance();
    }

    public User setFriendship(String friendName) {
        return repository.setFriendship(friendName);
    }

    public int checkFriendship(String friendName) {
        return repository.checkFriendship(friendName);
    }

    public void saveNewFriend(String friendName, String friendDomain, CertificateV2 certificateV2) {
        repository.saveNewFriend(friendName, friendDomain, certificateV2);
    }

    public void saveNewFriend(String friendName, boolean trust, CertificateV2 certificateV2) {
        repository.saveNewFriend(friendName, trust, certificateV2);
    }

    public void addFriendToUser(String username, String friend) {
        repository.addFriendToUser(username, friend);
    }

    public ArrayList<User> getAllFriends() {
        return repository.getAllFriends();
    }

    public LiveData<List<String>> observeAllFriends() {
        return repository.observeAllFriends();
    }

    public ArrayList<User> getPotentialFriends() {
        return repository.getPotentialFriends();
    }

    public ArrayList<String> getFriendsofFriend(String friendName) {
        return repository.getFriendsofFriend(friendName);
    }

    public ArrayList<User> getTrustedFriends() {
        return repository.getTrustedFriends();
    }

    public User deleteFriendship(String friendName) {
        return repository.deleteFriendship(friendName);
    }

    public User getFriend(String friendName) {
        return repository.getFriend(friendName);
    }

    public void addKey(String path, SecretKey secretKey) {
        repository.addKey(path, secretKey);
    }

    public PublishedContent checkIfShared(String path) {
        return repository.checkIfShared(path);
    }

    public void close(){
        repository.close();
    }
}
