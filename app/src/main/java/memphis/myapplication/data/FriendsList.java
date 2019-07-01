package memphis.myapplication.data;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import memphis.myapplication.data.RealmObjects.User;
import timber.log.Timber;

/**
 * The FriendsList class contains methods for getting the user's friends' friends list, comparing
 * those lists with the user's own, and adding the new users as discovered users
 */

public class FriendsList {
    ArrayList<User> friends;
    ArrayList<String> friendsPrefixesList;
    ArrayList<String> friendsNameList;
    /**
     * The default constructor creates a FriendsList instance with the user's friends/trusted users
     */
    public FriendsList() {
        friendsPrefixesList = new ArrayList<>();
        friendsNameList = new ArrayList<>();

        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        friends = realmRepository.getAllFriends();
        realmRepository.close();

        for (User f : friends) {
            friendsPrefixesList.add(f.getNamespace());
            friendsNameList.add(f.getUsername());
        }

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
        ArrayList<String> newFriends = new ArrayList<String>(fl.friendsPrefixesList);
        newFriends.removeAll(friendsPrefixesList);
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        // Remove our username from the friends list
        if (newFriends.contains("/" + myPrefix)) {
            Timber.d("We're still friends");
            newFriends.remove("/" + myPrefix);
        // If we don't find our name, then that means we are no longer friends with them. So remove
        // them from our friends list
        } else {
            Timber.d("We're not friends. Removing %s from our friends list", friendName);
            realmRepository.deleteFriendship(friendName);
        }

        for (String f : newFriends) {
            String username = f.substring(f.lastIndexOf("/")+1);
            Timber.d("Adding userRealm: %s", username);

            realmRepository.saveNewFriend(username, f.substring(0, f.indexOf("/npChat")), null);
            User sharingUser = realmRepository.addFriendToUser(friendName, f);
            realmRepository.addFriendToUser(username, sharingUser.getNamespace());
            realmRepository.close();
        }

    }
}
