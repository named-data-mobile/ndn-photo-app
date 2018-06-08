package memphis.myapplication.tasks;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Signature;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;

import java.io.IOException;

import memphis.myapplication.FileManager;
import memphis.myapplication.Globals;
import memphis.myapplication.MainActivity;

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

    public FetchingTask(MainActivity activity) {
        m_mainActivity = activity;
        // m_face = activity.face;
        m_face = new Face();
        Log.d("Face Check", "m_face: " + m_face.toString() + " globals: " + Globals.face);
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Boolean doInBackground(Interest... interests) {
        m_baseInterest = interests[0];
        m_shouldReturn = false;
        m_received = false;

            SegmentFetcher.fetch(
                    m_face,
                    m_baseInterest,
                    new SegmentFetcher.VerifySegment() {
                        @Override
                        public boolean verifySegment(Data data) {
                            /*Log.d("VerifySegment", "We just return true.");
                            return true;*/
                            Signature signature = data.getSignature();
                            Log.d("VerifySegment", signature.toString());
                            return true;
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
        // added this in since we are using a different face
        m_face.shutdown();
        return m_received;
    }

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
