package memphis.myapplication;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.User;
import timber.log.Timber;

public class FriendsList {
    RealmResults<User> friends;
    ArrayList<String> friendsPrefixesList;
    ArrayList<String> friendsNameList;


    public FriendsList() {
        friendsPrefixesList = new ArrayList<>();
        friendsNameList = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        friends = realm.where(User.class).equalTo("friend", true).or().equalTo("trust", true).findAll();

        if (friends == null) {
            Timber.d("Huh");
        }

        for (User f : friends) {
            Timber.d(f.toString());
            friendsPrefixesList.add(f.getNamespace());
            friendsNameList.add(f.getUsername());
        }

        realm.close();
    }

    public FriendsList(String j) throws JSONException {

        JSONArray ja = new JSONArray(j);
        friendsPrefixesList = new ArrayList<>();
        friendsNameList = new ArrayList<>();
        for (int i = 0; i<ja.length(); i++) {
            String friendsPrefix = ja.getString(i);
            Timber.d(friendsPrefix);
            Timber.d(friendsPrefix.substring(friendsPrefix.lastIndexOf("/")+1));
            friendsPrefixesList.add(ja.getString(i));
            friendsNameList.add(friendsPrefix.substring(friendsPrefix.lastIndexOf("/")+1));
        }

    }

    public ArrayList<String> getFriendsNamesList() { return friendsNameList; }

    public ArrayList<String> getFriendsPrefixesList() {
        return friendsPrefixesList;
    }

    public String stringify() {
        JSONArray jsonArray = new JSONArray((friendsPrefixesList));
        return jsonArray.toString();
    }

    public void addNew(FriendsList fl, String myPrefix) {
        ArrayList<String> newFriends = new ArrayList<String>(fl.friendsPrefixesList);
        newFriends.removeAll(friendsPrefixesList);
        newFriends.remove("/" + myPrefix);

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        for (String f : newFriends) {
            String username = f.substring(f.lastIndexOf("/")+1);
            Timber.d(username);
            User user = realm.where(User.class).equalTo("username", username).findFirst();
            if (user == null) {
                user = realm.createObject(User.class, username);
                Timber.d(f.substring(0, f.indexOf("/npChat")));
                user.setDomain(f.substring(0, f.indexOf("/npChat")));
            }
        }
        realm.commitTransaction();
        realm.close();

    }
}
