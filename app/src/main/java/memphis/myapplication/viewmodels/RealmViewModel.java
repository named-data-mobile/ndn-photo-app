package memphis.myapplication.viewmodels;

import androidx.lifecycle.ViewModel;

import net.named_data.jndn.security.v2.CertificateV2;

import java.util.ArrayList;

import javax.crypto.SecretKey;

import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.data.RealmRepository;

public class RealmViewModel extends ViewModel {
    public final RealmRepository repository;

    public RealmViewModel() {
        repository = RealmRepository.getInstance();
    }

    public void createInstance(){
        repository.createInstance();
    }

    public User setFriendship(String friendName) {
        return repository.setFriendship(friendName);
    }

    public int saveFriend(String friendName) {
        return repository.saveFriend(friendName);
    }

    public void saveNewFriend(String friendName, String friendDomain, CertificateV2 certificateV2) {
        repository.saveNewFriend(friendName, friendDomain, certificateV2);
    }

    public ArrayList<User> getAllFriends() {
        return repository.getAllFriends();
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
        return repository.deleteFriendship(friendName);
    }

    public void addKey(String path, SecretKey secretKey) {
        repository.addKey(path, secretKey);
    }

    public void close(){
        repository.close();
    }
}
