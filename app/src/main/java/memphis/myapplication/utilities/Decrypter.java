package memphis.myapplication.utilities;

import android.content.Context;

import memphis.myapplication.Globals;
import timber.log.Timber;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.tpm.TpmKeyHandle;
import net.named_data.jndn.util.Blob;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.data.tasks.FetchingTaskParams;

public class Decrypter {
    IvParameterSpec ivspec;
    Context context;
    final int filenameType = 100;
    final int friendNameType = 101;
    final int keyType = 102;
    final int syncDataType = 999;
    final int nameAndKeyType = 104;
    final int ivType = 105;

    public Decrypter (Context context) {
        this.context = context;
    }

    /**
     * Decrypts file data
     * @param secretKey symmetric key to decrypt the data
     * @param iv initialization vector
     * @param content the encrypted file data
     * @return Blob of decrypted file data
     */
    public Blob decrypt(SecretKey secretKey, byte[] iv, Blob content) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
                                                                            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Timber.d( "Decrypting file");
        ivspec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
        return new Blob(cipher.doFinal(content.getImmutableArray()), true);
    }

    public static SecretKey decryptSymKey(byte[] symKeyBytes, TpmKeyHandle privateKey) {
        try {
            Blob encryptedKeyBob = privateKey.decrypt(new Blob(symKeyBytes).buf());
            byte[] encryptedKey = encryptedKeyBob.getImmutableArray();
            return new SecretKeySpec(encryptedKey, 0, encryptedKey.length, "AES");
        } catch (TpmBackEnd.Error error) {
            error.printStackTrace();
            return null;
        }
    }
}
