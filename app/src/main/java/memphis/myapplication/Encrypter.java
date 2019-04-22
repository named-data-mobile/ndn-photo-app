package memphis.myapplication;

import android.content.Context;

import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.encrypt.algo.EncryptAlgorithmType;
import net.named_data.jndn.encrypt.algo.EncryptParams;
import net.named_data.jndn.encrypt.algo.RsaAlgorithm;
import net.named_data.jndn.util.Blob;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Encrypter {
    private final int filenameType = 100;
    private final int friendNameType = 101;
    private final int keyType = 102;
    private final int syncDataType = 999;
    private final int nameAndKeyType = 104;
    private final int ivType = 105;
    private Context context;

    public Encrypter (Context context) {
        this.context = context;

    }

    /**
     * Generates a new symmetric key
     * @return symmetric key
     */
    public SecretKey generateKey() {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            int keyBitSize = 256;
            keyGenerator.init(keyBitSize, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return keyGenerator.generateKey();
    }

    /**
     * Generates a new random initialization vector
     * @return initialization vector
     */
    public byte[] generateIV() {
        byte[] iv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * TLV encodes sync data
     * @param recipients the list of recipients
     * @param filename the uri of the file being shared
     * @param secretKey the encrypted symmetric key
     * @return Blob of TLV encoded data
     */
    public Blob encodeSyncData(ArrayList<String> recipients, String filename, SecretKey secretKey){
        TlvEncoder encoder = new TlvEncoder();
        int saveLength;
        FileManager manager = new FileManager(context);

        // Encode filename
        encoder.writeBlobTlv(filenameType, ByteBuffer.wrap(filename.getBytes()));
        saveLength = encoder.getLength();

        for (String friend : recipients) {

            // Get friend's public key
            Blob friendKey = manager.getFriendKey(friend);

            // Encrypt secret key with friend's public key
            Blob encryptedKey = null;
            try {
                encryptedKey = RsaAlgorithm.encrypt
                        (friendKey, new Blob(secretKey.getEncoded()), new EncryptParams(EncryptAlgorithmType.RsaOaep));
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }

            // Encode the symmetric key, iv, and friend's name
            encoder.writeBlobTlv(keyType, encryptedKey.buf());
            encoder.writeBlobTlv(friendNameType, ByteBuffer.wrap(friend.getBytes()));
            encoder.writeTypeAndLength(nameAndKeyType, encoder.getLength() - saveLength);
            saveLength = encoder.getLength();
        }

        encoder.writeTypeAndLength(syncDataType, encoder.getLength());
        return new Blob(encoder.getOutput(), true);
    }

    /**
     * Encrypts file data
     * @param secretKey the symmetric key to be used to encrypt the data
     * @param iv the initialization vector
     * @param data the file data
     * @return Blob of encrypted file data
     */
    public Blob encrypt(SecretKey secretKey, byte[] iv, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            byte[] encryptedData = cipher.doFinal(data);
            byte[] dataPlusIV = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, dataPlusIV, 0, iv.length);
            System.arraycopy(encryptedData, 0, dataPlusIV, iv.length, encryptedData.length);

            return new Blob(dataPlusIV, false);

        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;

    }

//    public boolean saveKey(SecretKey secretKey, byte[] iv, String filename) {
//
//    }

}
