//package memphis.myapplication;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.support.test.InstrumentationRegistry;
//
//import org.junit.Test;
//import org.mockito.Mockito;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//import static org.junit.Assert.assertTrue;
//import static org.mockito.Mockito.when;
//
//public class FriendsTests {
//    @Test
//    public void FriendsTests() {
//        final SharedPreferences sharedPrefs = Mockito.mock(SharedPreferences.class);
//        final SharedPreferences.Editor editor = Mockito.mock(SharedPreferences.Editor.class);
//        ArrayList<String> testFriends = new ArrayList<>(Arrays.asList("test"));
//        when(InstrumentationRegistry.getTargetContext().getString(anyInt())).thenReturn("test-string");
//        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
//        when(prefs.edit()).thenReturn(editor);
//        when(editor.commit()).thenReturn(true);
//
//        sharedPrefs.addFriend("test");
//        ArrayList<String> friends = sharedPrefs.getFriendsList();
//        assertTrue(friends.equals(testFriends));
//    }
//}
