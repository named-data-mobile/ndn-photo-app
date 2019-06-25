package memphis.myapplication;

import androidx.test.InstrumentationRegistry;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.SecretKey;

import memphis.myapplication.data.tasks.FetchingTaskParams;
import memphis.myapplication.utilities.Decrypter;
import memphis.myapplication.utilities.Encrypter;
import memphis.myapplication.utilities.FileManager;

import static org.junit.Assert.assertEquals;

public class TLVEncodeDecodeTest {
    @Test
    public void ecodingDecoding_returnsTrue() {
        String filename = "/test/filename.jpg";
        String[] users = {"mw"};
        ArrayList<String> recipients = new ArrayList<String>(Arrays.asList(users));
        Encrypter encrypter = new Encrypter(InstrumentationRegistry.getTargetContext());
        Decrypter decrypter = new Decrypter(InstrumentationRegistry.getTargetContext());
        SecretKey secretKey = encrypter.generateKey();
        byte[] iv = encrypter.generateIV();
        Blob fakeKey = null;
        FileManager manager = Mockito.mock(FileManager.class);

        Mockito.when(manager.getFriendKey(users[0])).thenReturn(fakeKey);

        // This currently throw an exception as it tries to get the recipient's public key, which returns null
        Blob syncData = encrypter.encodeSyncData(recipients, filename, secretKey, iv);

        FetchingTaskParams input = new FetchingTaskParams(new Interest(new Name(filename)), secretKey, iv);
        assertEquals(decrypter.decodeSyncData(syncData), input);
    }
}
