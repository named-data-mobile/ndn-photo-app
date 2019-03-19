package memphis.myapplication.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.DigestAlgorithm;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.UnrecognizedKeyFormatException;
import net.named_data.jndn.security.VerificationHelpers;
import net.named_data.jndn.security.certificate.PublicKey;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;
import net.named_data.jndn.util.SignedBlob;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import memphis.myapplication.FileManager;
import memphis.myapplication.FilesActivity;
import memphis.myapplication.Globals;
import memphis.myapplication.R;

import static java.lang.Thread.sleep;

// revisit params
public class FetchingTask extends AsyncTask<Interest, Void, Boolean> {

    private Activity m_parentActivity;
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
    private int m_numRetries = 10;

    public FetchingTask(Activity activity) {
        m_parentActivity = activity;
        m_currContext = activity.getApplicationContext();
        m_appPrefix = "/" + m_currContext.getResources().getString(R.string.app_name);
        m_face = new Face();
        Log.d("Face Check", "m_face: " + m_face.toString() + " globals: " + Globals.face);
        m_manager = new FileManager(m_currContext);
        m_received = false;
    }

    // actual process; we are using the SegmentFetcher to retrieve data
    @Override
    protected Boolean doInBackground(Interest... interests) {
        m_baseInterest = interests[0];
        fetch(m_baseInterest);
        // added this in since we are using a new face for fetching and don't need it afterwards
        m_face.shutdown();

        return m_received;
    }

    private void fetch(Interest interest) {
        m_shouldReturn = false;

        interest.setInterestLifetimeMilliseconds(35000);
        final Name appAndUsername = m_baseInterest.getName().getPrefix(2);
        Log.d("BeforeVerify", "appAndUsername:" + appAndUsername.toUri());
        getUserInfo(m_baseInterest);
        Log.d("KeyType", m_pubKey.getKeyType().toString());

        SegmentFetcher.fetch(
                m_face,
                interest,
                new SegmentFetcher.VerifySegment() {
                    @Override
                    public boolean verifySegment(Data data) {
                        //m_data = data;
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
                        m_content = content;
                        m_received = true;
                        m_shouldReturn = true;
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        if(errorCode == SegmentFetcher.ErrorCode.INTEREST_TIMEOUT) {
                            Log.d("Segment fetcher", "timed out");
                             //get the name we timed out with from message
                            int index = message.lastIndexOf(m_appPrefix);
                            if(index != -1) {
                                Interest interest = new Interest(new Name(message.substring(index)));
                                if(m_numRetries > 0) {
                                    m_numRetries--;
                                    fetch(interest);

                                }
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
     * @param interest
     */
    private void getUserInfo(Interest interest) {
        String s = interest.getName().getPrefix(2).toUri();
        s = s.substring(1);
        int index = s.indexOf("/");
        if(s.substring(0, index).equals(m_appPrefix.substring(1))) {
            m_user = s.substring(index+1, s.length());
            // we have the user, check if we're friends. If so, retrieve their key from file.
            ArrayList<String> friendsList = m_manager.getFriendsList();
            Log.d("username&PubKey", "user: " + m_user);
            if(friendsList.contains(m_user)) {
                try {
                    m_pubKey = new PublicKey(m_manager.getFriendKey(m_user));
                }
                catch(UnrecognizedKeyFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
                    Log.d("verifySignature", "We've made it to verify(signature)");
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
        Log.d("FetchingTask", "Calling onPostExecute");
        if (!m_received) {
            Log.d("FetchingTask", "onPostExecute: not received");
        }
        if (m_received) {
            // FileManager manager = new FileManager(m_parentActivity.getApplicationContext());
            Log.d("onPostExecute", "m_content size; " + m_content.size());
            boolean wasSaved = m_manager.saveContentToFile(m_content, m_baseInterest.getName().toUri());
            if (wasSaved) {
                m_resultMsg = "We got content.";
                Log.d("FetchingTask: ", "Data saved");
                m_parentActivity.runOnUiThread(makeToast(m_resultMsg));
            } else {
                m_resultMsg = "Failed to save retrieved content";
                m_parentActivity.runOnUiThread(makeToast(m_resultMsg));
            }
        }
        else {
            Log.d("fetch_data onError", m_resultMsg);
            m_parentActivity.runOnUiThread(makeToast(m_resultMsg));
        }
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public Runnable makeToast(final String s) {
        return new Runnable() {
            public void run() {
                Toast.makeText(m_currContext, s, Toast.LENGTH_LONG).show();
            }
        };
    }
}
