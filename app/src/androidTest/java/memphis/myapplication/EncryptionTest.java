package memphis.myapplication;

import androidx.test.InstrumentationRegistry;

import net.named_data.jndn.util.Blob;

import org.junit.Test;


import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static org.junit.Assert.assertTrue;

public class EncryptionTest {
    private Blob encryptedBlob;
    private Encrypter encrypter;
    private SecretKey secretKey;
    private byte[] iv;
    byte[] bytes;
    private Decrypter decrypter;
    private byte[] decryptedData;

    @Test
    public void encryption_returnsTrue() {
        bytes = new byte[]
                {
                        0x01, 0x01, 0x06, 0x00, 0x00, 0x00, 0x3d, 0x1d, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x43, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x0b, 0x43, 0x01, 0x43, 0x42, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x63, 0x06,
                        0x53, 0x63, 0x35, 0x01, 0x01, 0x3d, 0x07, 0x01, 0x00, 0x0b, 0x43, 0x01, 0x06, 0x42,
                        0x32, 0x04, 0x00, 0x00, 0x00, 0x00, 0x37, 0x04, 0x01, 0x03, 0x06, 0x2a, 0x06, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                };
        encrypter = new Encrypter(InstrumentationRegistry.getTargetContext());
        secretKey = encrypter.generateKey();
        iv = encrypter.generateIV();
        try {
             encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        decrypter = new Decrypter(InstrumentationRegistry.getTargetContext());
        decryptedData = decrypter.decrypt(secretKey, iv, encryptedBlob).getImmutableArray();
        assertTrue(Arrays.equals(bytes, decryptedData));



    }
}

