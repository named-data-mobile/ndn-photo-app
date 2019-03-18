package memphis.myapplication;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import memphis.myapplication.MainActivity;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Face;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Rule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static net.named_data.jndn.encoding.tlv.Tlv.Interest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*class LooperThread extends Thread {
    public Handler mHandler;

    public void run() {
        Looper.prepare();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                // process incoming messages here
            }
        };

        Looper.loop();
    }

    public void quit() {
        try {
            Looper.myLooper().quit();
        }
        catch(NullPointerException e)
        {
            Log.d("Test Looper", "Failed to quit Looper");
        }
    }
}*/

@RunWith(AndroidJUnit4.class)
public class PublisherAndFetcherTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    /*@Test
    public void checkCacheContent() {
        String stuff = "This is a test string to insert into the cache.";
        byte[] bytes = stuff.getBytes(StandardCharsets.UTF_8);
        Blob blob = new Blob(bytes, true);
        Name prefix = new Name("/test/stuff");

        onView(withId(R.id.editText)).check(matches((isDisplayed())));
        onView(withId(R.id.editText)).perform(clearText());
        onView(withId(R.id.editText)).perform(typeText("/test/stuff"));
        onView(withId(R.id.button3)).check(matches((isDisplayed())));
        onView(withId(R.id.button3)).perform(click());

        mActivityRule.getActivity().publishData(blob, prefix);
        Data[] cache = faceProxy.getCache();
        for(int i = 0; i < cache.length; i++) {
            if(cache[i] != null) {
                Log.d("checkCacheContentTest", "Index " + i + ": " + cache[i].getContent().toString());
            }
        }
    }*/

    @Test
    public void pubfetchSimpleString() {
        String shortContent = "This is a string of test content which we will publish and later fetch.";
        // byte[] bytes = shortContent.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = shortContent.getBytes();
        Log.d("pubfetchSimpleString", "size of bytes for shortContent: " + bytes.length);
        Blob blob = new Blob(bytes, true);
        Name prefix = new Name("/test/shortContent");
        // mActivityRule.getActivity().setup_security();

        //fetch_data(View view); "editText" is the input bar; "button3" is "Register with NFD" button;
        // "button" is Fetch Data button
        onView(withId(R.id.editText)).check(matches((isDisplayed())));
        onView(withId(R.id.editText)).perform(clearText());
        onView(withId(R.id.editText)).perform(typeText("/test/shortContent"));
        onView(withId(R.id.registerButton)).check(matches((isDisplayed())));
        onView(withId(R.id.registerButton)).perform(click());

        try {
            Thread.sleep(2000);
        }
        catch(java.lang.InterruptedException e) {
            Log.d("PublisherAndFetcherTest", "Refused to sleep thread.");
        }
        // onView(withId(R.id.button)).perform(click());
        mActivityRule.getActivity().publishData(blob, prefix);

        try {
            Thread.sleep(2000);
        }
        catch(java.lang.InterruptedException e) {
            Log.d("PublisherAndFetcherTest", "Refused to sleep thread.");
        }

        onView(withId(R.id.fetchButton)).check(matches((isDisplayed())));
        onView(withId(R.id.fetchButton)).perform(click());
        try {
            Thread.sleep(2000);
        }
        catch(java.lang.InterruptedException e) {
            Log.d("PublisherAndFetcherTest", "Refused to sleep thread.");
        }

        Assert.assertEquals(shortContent, mActivityRule.getActivity().retrieved_data);
    }

    // I don't think our method is working currently without a file; let's try to do the same thing
    // for a file that we know we have on our device
    // Well, I don't see any indication that the fetcher is even working, so the file is most likely
    // the issue. The cache should be checked anyway and there is no reason for stuff to be overwritten
    // given we are working with such small data packets.

}
