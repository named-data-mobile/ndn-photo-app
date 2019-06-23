package memphis.myapplication;

import android.app.Activity;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.SecretKey;

import memphis.myapplication.psync.ConsumerManager;
import memphis.myapplication.tasks.FetchingTaskParams;

import static org.junit.Assert.assertThat;

public class TLVEncodeDecodeTest {
    @Test
    public void ecodingDecoding_returnsTrue() {
        String filename = "/test/filename.jpg";
        byte[] iv = new byte[16];
        String[] users = {"testUser"};
        Activity activity = new MainFragment();
        SecretKey secretKey = activity.;
        ArrayList<String> recipients = new ArrayList<String>(Arrays.asList(users));
        Blob syncData =
        FetchingTaskParams input = new FetchingTaskParams(new Interest(new Name(filename.toString())), secretKey, iv);
        assertThat(ConsumerManager.decodeSyncData(syncData)).isEqual();
    }
}
