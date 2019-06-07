package memphis.myapplication;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import io.realm.Realm;
import io.realm.RealmResults;
import memphis.myapplication.RealmObjects.User;
import timber.log.Timber;

/**
 * The FriendsList class contains methods for getting the user's friends' friends list, comparing
 * those lists with the user's own, and adding the new users as discovered users
 */

public class FriendsList {
    RealmResults<User> friends;
    ArrayList<String> friendsPrefixesList;
    ArrayList<String> friendsNameList;

    /**
     * The default constructor creates a FriendsList instance with the user's friends/trusted users
     */
    public FriendsList() {
        friendsPrefixesList = new ArrayList<>();
        friendsNameList = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        friends = realm.where(User.class).equalTo("friend", true).findAll();

        for (User f : friends) {
            friendsPrefixesList.add(f.getNamespace());
            friendsNameList.add(f.getUsername());
        }

        realm.close();
    }

    /**
     * Constructor takes a String containing a friend's friends list.
     */
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

    /**
     * Outputs JSONArray string of the friends list
     * @return jsonArray.toString(): friends list
     */
    public String stringify() {
        JSONArray jsonArray = new JSONArray((friendsPrefixesList));
        return jsonArray.toString();
    }

    /**
     * Compares the user's friends list with their friend's, adds any new users to discovered list,
     * and updates the local friends list for the friend
     * @param fl: a friend's friends list instance
     * @param friendName: the friend's name
     * @param myPrefix: the user's prefix
     */
    public void addNew(FriendsList fl, String friendName, String myPrefix) {
        Realm realm = Realm.getDefaultInstance();
        ArrayList<String> newFriends = new ArrayList<String>(fl.friendsPrefixesList);
        newFriends.removeAll(friendsPrefixesList);

        // Remove our username from the friends list
        if (newFriends.contains("/" + myPrefix)) {
            Timber.d("We're still friends");
            newFriends.remove("/" + myPrefix);
        // If we don't find our name, then that means we are no longer friends with them. So remove
        // them from our friends list
        } else {
            Timber.d("We're not friends. Removing %s from our friends list", friendName);
            realm.beginTransaction();
            User user = realm.where(User.class).equalTo("username", friendName).findFirst();
            user.setFriend(false);
            realm.commitTransaction();
        }




        for (String f : newFriends) {
            realm.beginTransaction();
            String username = f.substring(f.lastIndexOf("/")+1);
            Timber.d("Adding user: %s", username);
            User user = realm.where(User.class).equalTo("username", username).findFirst();
            if (user == null) {
                user = realm.createObject(User.class, username);
                Timber.d(f.substring(0, f.indexOf("/npChat")));
                user.setDomain(f.substring(0, f.indexOf("/npChat")));
            }
            User sharingUser = realm.where(User.class).equalTo("username", friendName).findFirst();
            user.addFriend(sharingUser.getNamespace());
            sharingUser.addFriend(f);
            realm.commitTransaction();

        }
        realm.close();

    }
}
