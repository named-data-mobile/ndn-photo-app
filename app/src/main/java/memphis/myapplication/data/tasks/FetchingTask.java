package memphis.myapplication.data.tasks;

import android.content.Context;
import android.os.AsyncTask;

import memphis.myapplication.data.Common;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.data.RealmRepository;
import memphis.myapplication.utilities.SharedPrefsManager;
import timber.log.Timber;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.DigestAlgorithm;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.UnrecognizedKeyFormatException;
import net.named_data.jndn.security.certificate.PublicKey;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;
import net.named_data.jndn.util.SignedBlob;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import memphis.myapplication.utilities.Decrypter;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.Globals;

import static java.lang.Thread.sleep;

/**
 * AsyncTask to fetch the file on the background thread
 */
public class FetchingTask extends AsyncTask<FetchingTaskParams, Void, Boolean> {

    private final MutableLiveData<String> toastData;
    private Context m_currContext;
    private Face m_face;
    private Blob m_content;
    private boolean m_shouldReturn;
    private boolean m_received;
    private String m_resultMsg;
    private Interest m_baseInterest;
    private String m_user;
    private PublicKey m_pubKey;
    private FileManager m_manager;
    private Data m_data;
    private String m_appPrefix;
    private int m_numRetries = 50;
    private String m_fileKeyDigest;
    private boolean m_feed;

    private SecretKey m_secretKey;

    public FetchingTask(Context applicationContext, MutableLiveData<String> toastData) {
        m_currContext = applicationContext;
        this.toastData = toastData;
        SharedPrefsManager sharedPrefsManager = SharedPrefsManager.getInstance(applicationContext);
        m_appPrefix = sharedPrefsManager.getDomain();
        m_face = new Face();
        Timber.d("Face Check: %s", "m_face: " + m_face.toString() + " globals: " + Globals.face);
        m_manager = new FileManager(m_currContext);
        m_received = false;
    }

    // actual process; we are using the SegmentFetcher to retrieve data
    @Override
    protected Boolean doInBackground(FetchingTaskParams... params) {
        m_baseInterest = params[0].interest;
        m_secretKey = params[0].secretKey;
        m_feed = params[0].feed;
        m_fileKeyDigest = m_baseInterest.getName().get(-1).toEscapedString();
        Timber.d(m_baseInterest.toUri());
        fetch(m_baseInterest, m_secretKey);
        // added this in since we are using a new face for fetching and don't need it afterwards
        m_face.shutdown();

        return m_received;
    }

    /**
     * Fetch the data for the interest
     * @param interest The interest to fetch
     * @param secretKey
     */
    private void fetch(Interest interest, SecretKey secretKey) {
        m_shouldReturn = false;
        interest.setInterestLifetimeMilliseconds(15000);

        int npChatComp = 0;
        for (int i = 0; i<=m_baseInterest.getName().size(); i++) {
            if (m_baseInterest.getName().getSubName(i, 1).toUri().equals("/npChat")){
                npChatComp = i;
                break;
            }
        }
        final Name username = m_baseInterest.getName().getSubName(npChatComp + 1, 1);
        getUserInfo(username.toUri().substring(1));
        Timber.d("KeyType: %s", m_pubKey.getKeyType().toString());
        Timber.d("interest: %s", interest.getName());

        SegmentFetcher.fetch(
                m_face,
                interest,
                new SegmentFetcher.VerifySegment() {
                    @Override
                    public boolean verifySegment(Data data) {
                        //m_data = data;
                        Timber.d( "verifying segment");
                        SignedBlob encoding = data.wireEncode(WireFormat.getDefaultWireFormat());
                        boolean isVerified = verifySignature
                                (encoding.signedBuf(), data.getSignature().getSignature().getImmutableArray(), m_pubKey,
                                        DigestAlgorithm.SHA256);
                        return isVerified;
                    }
                },
                new SegmentFetcher.OnComplete() {
                    @Override
                    public void onComplete(Blob content) {
                        Timber.i("Completed");
                        m_content = content;
                        m_received = true;
                        m_shouldReturn = true;
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        if(errorCode == SegmentFetcher.ErrorCode.INTEREST_TIMEOUT) {
                            Timber.d( "timed out");
                            if(m_numRetries > 0) {
                                m_numRetries--;
                                fetch(m_baseInterest, m_secretKey);

                            }
                        }
                        m_resultMsg = message;
                        m_shouldReturn = true;
                    }
                });
        while(!m_shouldReturn) {
            try {
                m_face.processEvents();
                sleep(10);
            }
            catch(IOException | EncodingException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extracts the username from the Data and sets m_user. It then checks if the username matches any
     * friend in the friends directory.
     * @param user
     */
    private void getUserInfo(String user) {
        m_user = user;
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User m_user = realmRepository.getFriend(user);
        realmRepository.close();
        // we have the user, check if we're friends. If so, retrieve their key from file.
        Timber.d("username&PubKey: %s", "user: " + m_user.getUsername());
        if(m_user.isFriend()) {
            try {
                m_pubKey = new PublicKey(m_user.getCert().getPublicKey());
            }
            catch(UnrecognizedKeyFormatException e) {
                e.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (CertificateV2.Error error) {
                error.printStackTrace();
            }
        }
    }

    /**
     * Verify the received data buffer
     * @param buffer received signed buffer
     * @param signature sender's signature
     * @param publicKey sender's public key
     * @param digestAlgorithm
     * @return True if the data is actually from the sender
     */
    public static boolean verifySignature(ByteBuffer buffer, byte[] signature, PublicKey publicKey,
                                          DigestAlgorithm digestAlgorithm) {
        if(digestAlgorithm == DigestAlgorithm.SHA256) {
            if (publicKey.getKeyType() == KeyType.RSA) {
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    java.security.PublicKey securityPublicKey = keyFactory.generatePublic
                            (new X509EncodedKeySpec(publicKey.getKeyDer().getImmutableArray()));

                    java.security.Signature rsaSignature =
                            java.security.Signature.getInstance("SHA256withRSA");
                    rsaSignature.initVerify(securityPublicKey);
                    rsaSignature.update(buffer);
                    Timber.d("We've made it to verify(signature)");
                    return rsaSignature.verify(signature);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    // What we do when doInBackground finishes; show result in Toast message
    @Override
    protected void onPostExecute(Boolean wasReceived) {
        Timber.d("Calling onPostExecute");
        if (!m_received) {
            Timber.d("onPostExecute: not received");
        }
        if (m_received) {
            // FileManager manager = new FileManager(applicationContext);
            Timber.d("m_content size; " + m_content.size());

            // Check symkey if for feed
            if (m_feed) {
                try {
                    checkKey(m_fileKeyDigest);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            // else decrypt with key received in sync data
            else {
                succeed();
            }

        }
        else {
            Timber.d(" onError: %s", m_resultMsg);
            toastData.postValue(m_resultMsg);
        }
    }

    public void checkKey(String keyDigest) throws NoSuchAlgorithmException {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();

        if (keyDigest.equals(Common.getKeyDigest(realmRepository.getSymKey(m_user))))
            succeed();
        else
            fetchKey();
    }

    /**
     * Extract the data from successfully received and verified file
     */
    public void succeed() {
        boolean wasSaved;
        // Get IV
        byte[] content = m_content.getImmutableArray();
        byte[] iv = Arrays.copyOfRange(content, 0, 16);
        byte[] data = Arrays.copyOfRange(content, iv.length, content.length);

        // Decrypt content
        Decrypter decrypter = new Decrypter(m_currContext);
        Blob decryptedContent = null;
        try {
            Timber.d(Arrays.toString(m_secretKey.getEncoded()));
            decryptedContent = decrypter.decrypt(m_secretKey, iv, new Blob(data));
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
        wasSaved = m_manager.saveContentToFile(decryptedContent, m_baseInterest.getName());


        if (wasSaved) {
            m_resultMsg = "We got content.";
            Timber.d( "Data saved");
            toastData.postValue(m_resultMsg);
        } else {
            m_resultMsg = "Failed to save retrieved content";
            toastData.postValue(m_resultMsg);
        }

    }

    /**
     * Request symmetric key for the data from the friend
     */
    public void fetchKey() {
       requestSymKey(RealmRepository.getInstanceForNonUI().getFriend(m_user).getNamespace(), m_fileKeyDigest, SharedPrefsManager.getInstance(m_currContext).getUsername());
    }

    /**
     * Request symmetric key for data
     * @param friendNameSpace
     * @param keyName
     * @param username user requesting the key
     */
    public void requestSymKey(final String friendNameSpace, String keyName, String username) {
        Interest symKeyInterest = new Interest(new Name(friendNameSpace));
        symKeyInterest.getName().append("keys");
        symKeyInterest.getName().append(keyName);
        symKeyInterest.getName().append(username);
        Timber.d("Requesting their sym key");
        try {
            Globals.face.expressInterest(symKeyInterest, new OnData() {
                @Override
                public void onData(Interest interest, Data data) {
                    // Store friend's symmetric key
                    RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                    Timber.d("Saving key of " + friendNameSpace.substring(friendNameSpace.lastIndexOf("/")+1));
                    realmRepository.setSymKey(friendNameSpace.substring(friendNameSpace.lastIndexOf("/")+1), data.getContent().getImmutableArray());
                    realmRepository.close();
                    m_secretKey = RealmRepository.getInstanceForNonUI().getSymKey(m_user);
                    try {
                        checkKey(Common.getKeyDigest(m_secretKey));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            }, onSymKeyTimeOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    OnTimeout onSymKeyTimeOut = new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
            m_resultMsg = "Failed to retrieve correct key from " + m_user;
            fail();
        }
    };

    public void fail() {
        toastData.postValue(m_resultMsg);
    }
}
