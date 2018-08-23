package memphis.myapplication.tasks;

import android.os.AsyncTask;
import android.util.Log;

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

    private FilesActivity m_parentActivity;
    private Face m_face;
    private Blob m_content;
    private ArrayList<Data> m_tempContent;
    private boolean m_shouldReturn;
    private boolean m_received;
    private String m_resultMsg;
    private Interest m_baseInterest;
    private String m_user;
    private PublicKey m_pubKey;
    private FileManager m_manager;
    private Data m_data;
    private String m_appPrefix;
    private int m_numRetries = 5;

    public FetchingTask(FilesActivity activity) {
        m_parentActivity = activity;
        m_appPrefix = "/" + activity.getApplication().getString(R.string.app_name);
        m_face = new Face();
        Log.d("Face Check", "m_face: " + m_face.toString() + " globals: " + Globals.face);
        m_manager = new FileManager(activity.getApplicationContext());
        m_tempContent = new ArrayList();
    }

    /*@Override
    protected void onPreExecute() {

    }*/

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
        m_received = false;
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
                        m_data = data;
                        SignedBlob encoding = data.wireEncode(WireFormat.getDefaultWireFormat());
                        boolean isVerified = verifySignature
                                (encoding.signedBuf(), data.getSignature().getSignature().getImmutableArray(), m_pubKey,
                                        DigestAlgorithm.SHA256);
                        if(isVerified) {
                            m_tempContent.add(data);
                        }
                        return isVerified;
                        //return VerificationHelpers.verifyDataSignature(data, m_pubKey, DigestAlgorithm.SHA256);
                    }
                },
                new SegmentFetcher.OnComplete() {
                    // we have added in retries, so we must obtain content from m_tempContent if we
                    // used a retry; otherwise, we will be missing segments since each fetch must
                    // use a new Segment Fetcher
                    @Override
                    public void onComplete(Blob content) {
                        if(m_numRetries < 5) {
                            // This is from the SegmentFetcher; most everything in the class is private
                            // including its constructors; this is how it keeps track of content
                            int totalSize = 0;
                            for (int i = 0; i < m_tempContent.size(); ++i)
                                totalSize += ((Blob)m_tempContent.get(i).getContent()).size();
                            ByteBuffer tempBuffer = ByteBuffer.allocate(totalSize);
                            for (int i = 0; i < m_tempContent.size(); ++i)
                                tempBuffer.put(((Blob)m_tempContent.get(i).getContent()).buf());
                            tempBuffer.flip();
                            m_content = new Blob(tempBuffer, false);
                        }
                        else {
                            m_content = content;
                        }
                        m_received = true;
                        m_shouldReturn = true;
                    }
                },
                new SegmentFetcher.OnError() {
                    @Override
                    public void onError(SegmentFetcher.ErrorCode errorCode, String message) {
                        // if there is a timeout, could we just retrigger fetchingtask starting
                        // with the last segment number?
                        if(errorCode == SegmentFetcher.ErrorCode.INTEREST_TIMEOUT) {
                            // get the name we timed out with from message
                            int index = message.lastIndexOf(m_appPrefix);
                            if(index != -1) {
                                // we'll retry by asking for this segment again
                                // we'll also need to keep a temp object since we can't access
                                // the SegmentFetcher's current content
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
        if(s.substring(0, index).equals(m_parentActivity.getString(R.string.app_name))) {
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
        if (m_received) {
            FileManager manager = new FileManager(m_parentActivity.getApplicationContext());
            boolean wasSaved = manager.saveContentToFile(m_content, m_baseInterest.getName().toUri());
            if (wasSaved) {
                m_resultMsg = "We got content.";
                m_parentActivity.runOnUiThread(m_parentActivity.makeToast(m_resultMsg));
            } else {
                m_resultMsg = "Failed to save retrieved content";
                m_parentActivity.runOnUiThread(m_parentActivity.makeToast(m_resultMsg));
            }
        }
        else {
            Log.d("fetch_data onError", m_resultMsg);
            m_parentActivity.runOnUiThread(m_parentActivity.makeToast(m_resultMsg));
        }
    }
}
