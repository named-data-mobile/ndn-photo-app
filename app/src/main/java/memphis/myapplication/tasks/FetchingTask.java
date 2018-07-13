package memphis.myapplication.tasks;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.KeyLocator;
import net.named_data.jndn.Name;
import net.named_data.jndn.Signature;
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
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import memphis.myapplication.FileManager;
import memphis.myapplication.Globals;
import memphis.myapplication.MainActivity;
import memphis.myapplication.R;

import static java.lang.Thread.sleep;

// revisit params
public class FetchingTask extends AsyncTask<Interest, Void, Boolean> {

    private MainActivity m_mainActivity;
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

    public FetchingTask(MainActivity activity) {
        m_mainActivity = activity;
        // m_face = activity.face;
        m_face = new Face();
        Log.d("Face Check", "m_face: " + m_face.toString() + " globals: " + Globals.face);
        m_manager = new FileManager(activity.getApplicationContext());
    }

    /*@Override
    protected void onPreExecute() {

    }*/

    // actual process; we are using the SegmentFetcher to retrieve data
    @Override
    protected Boolean doInBackground(Interest... interests) {
        m_baseInterest = interests[0];
        m_shouldReturn = false;
        m_received = false;
        final Name appAndUsername = m_baseInterest.getName().getPrefix(2);
        Log.d("BeforeVerify", "appAndUsername:" + appAndUsername.toUri());
        getUserInfo(m_baseInterest);
        Log.d("KeyType", m_pubKey.getKeyType().toString());

            SegmentFetcher.fetch(
                    m_face,
                    m_baseInterest,
                    new SegmentFetcher.VerifySegment() {
                        @Override
                        public boolean verifySegment(Data data) {
                            m_data = data;
                            SignedBlob encoding = data.wireEncode(WireFormat.getDefaultWireFormat());
                            return verifySignature
                                    (encoding.signedBuf(), data.getSignature().getSignature().getImmutableArray(), m_pubKey,
                                            DigestAlgorithm.SHA256);
                            // return VerificationHelpers.verifyDataSignature(data, m_pubKey, DigestAlgorithm.SHA256);
                            // return true;
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
                            try {
                                m_resultMsg = message + " " + m_data.getFullName().toUri();
                            }
                            catch(EncodingException e) {
                                e.printStackTrace();
                                m_resultMsg = message;
                            }
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
        // added this in since we are using a new face for fetching
        m_face.shutdown();
        return m_received;
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
        Log.d("appName", "From Substring: " + s.substring(0, index));
        Log.d("appName", "From Resource: " + m_mainActivity.getString(R.string.app_name));
        if(s.substring(0, index).equals(m_mainActivity.getString(R.string.app_name))) {
            m_user = s.substring(index+1, s.length());
            // we have the user, check if we're friends. If so, retrieve their key from file.
            ArrayList<String> friendsList = m_manager.getFriendsList();
            Log.d("username&PubKey", "user: " + m_user);
            if(friendsList.contains(m_user)) {
                try {
                    m_pubKey = new PublicKey(m_manager.getFriendKey(m_user));
                    Log.d("fetchingTask", "m_pubkey der: " + m_pubKey.getKeyDer().toString());
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
                    Log.d("rsaSignature", rsaSignature.toString());
                    Log.d("verifySignature", "We've made it to verify(signature)");
                    Log.d("verifySignature", "Signature size: " + signature.length);
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
        if (m_received) {
            FileManager manager = new FileManager(m_mainActivity.getApplicationContext());
            boolean wasSaved = manager.saveContentToFile(m_content, m_baseInterest.getName().toUri());
            if (wasSaved) {
                m_resultMsg = "We got content.";
                m_mainActivity.runOnUiThread(m_mainActivity.makeToast(m_resultMsg));
            } else {
                m_resultMsg = "Failed to save retrieved content";
                m_mainActivity.runOnUiThread(m_mainActivity.makeToast(m_resultMsg));
            }
        }
        else {
            Log.d("fetch_data onError", m_resultMsg);
            m_mainActivity.runOnUiThread(m_mainActivity.makeToast(m_resultMsg));
        }
    }
}
