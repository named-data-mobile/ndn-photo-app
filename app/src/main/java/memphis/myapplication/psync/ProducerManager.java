package memphis.myapplication.psync;
import android.util.Base64;
import android.util.Log;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.PSync;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ProducerManager {
    public static PSync.PartialProducer m_producer;
    private static Map<Long, String> m_seqToFileName = new HashMap<Long, String>();
    private static String producerPrefix;

    public ProducerManager(String p) {
        producerPrefix = p;
        m_producer = new PSync.PartialProducer(80, producerPrefix, producerPrefix + "/data", 500, 1000);
    }

    public void setDataSeqMap(String syncData) {
        m_seqToFileName.put(m_producer.getSeqNo(producerPrefix + "/data") + 1, syncData);
    }



    public String getSeqMap(long seq) {
        return m_seqToFileName.get(seq);
    }

    public static final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Timber.d("Called OnInterestCallback with Interest: " + interest.getName().toUri());
            try {
                Data data = new Data(interest.getName());
                Blob content = new Blob(Base64.encode(m_seqToFileName.get(m_producer.getSeqNo(producerPrefix + "/data")).getBytes(), 0));
                data.setContent(new Blob(content));
                face.putData(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}
