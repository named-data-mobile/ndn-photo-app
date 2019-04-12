package memphis.myapplication;

import android.content.Context;

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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.tasks.FetchingTaskParams;

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
    public Blob decrypt(SecretKey secretKey, byte[] iv, Blob content) {
        try {
            ivspec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new Blob(cipher.doFinal(content.getImmutableArray()), true);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
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

    /**
     * Decodes TLV encoded sync data
     * @param interestData encoded sync data
     * @return FetchingTaskParams object containing the interest for the file, the symmetric key, and the initialization vector
     */
    public FetchingTaskParams decodeSyncData(Blob interestData) {
        FileManager manager = new FileManager(context);

        Blob filename = null;
        Blob recipient = null;
        Blob symmetricKey = null;
        byte[] iv = null;

        TlvDecoder decoder = new TlvDecoder(interestData.buf());
        int endOffset = 0;
        try {
            endOffset = decoder.readNestedTlvsStart(syncDataType);
            while (decoder.getOffset() < endOffset) {
                if (decoder.peekType(filenameType, endOffset)) {
                    filename = new Blob(decoder.readBlobTlv(filenameType), true);
                }
                else if (decoder.peekType(nameAndKeyType, endOffset)) {
                    int friendOffsetEnd = decoder.readNestedTlvsStart(nameAndKeyType);
                    while (decoder.getOffset() < friendOffsetEnd) {
                        if (decoder.peekType(keyType, friendOffsetEnd)) {
                        }
                        if (decoder.peekType(friendNameType, friendOffsetEnd)) {
                            recipient = new Blob(decoder.readBlobTlv(friendNameType), true);
                            if (recipient.toString().equals(manager.getUsername())) {
                                iv = new Blob(decoder.readBlobTlv(ivType), true).getImmutableArray();
                                symmetricKey = new Blob(decoder.readBlobTlv(keyType), true);
                                decoder.finishNestedTlvs(friendOffsetEnd);
                            }
                            else {
                                decoder.skipTlv(ivType);
                                decoder.skipTlv(keyType);
                            }
                        }
                    }
                    decoder.finishNestedTlvs(friendOffsetEnd);
                }
            }

            if (recipient.toString().equals(manager.getUsername())) {
                // Decrypt symmetric key
                TpmBackEndFile m_tpm = Globals.tpm;
                TpmKeyHandle privateKey = m_tpm.getKeyHandle(Globals.pubKeyName);
                Blob encryptedKeyBob = privateKey.decrypt(symmetricKey.buf());
                byte[] encryptedKey = encryptedKeyBob.getImmutableArray();
                SecretKey secretKey = new SecretKeySpec(encryptedKey, 0, encryptedKey.length, "AES");
                System.out.println("Filename : " + filename);
                return new FetchingTaskParams(new Interest(new Name(filename.toString())), secretKey, iv);
            }

            decoder.finishNestedTlvs(endOffset);
        } catch (EncodingException e) {
            e.printStackTrace();
        } catch (TpmBackEnd.Error error) {
            error.printStackTrace();
        }
        return null;
    }
}
