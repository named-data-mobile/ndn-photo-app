package memphis.myapplication.psync;

import android.util.Log;

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
    public static PSync.PartialProducer m_cert_producer;
    private static Map<Long, byte[]> m_seqToFileName = new HashMap<Long, byte[]>();
    private static String producerPrefix;

    public ProducerManager(String p) {
        producerPrefix = p;
        m_producer = new PSync.PartialProducer(80, producerPrefix, producerPrefix + "/data", 500, 1000);
    }

    public void setDataSeqMap(byte[] syncData) {
        m_seqToFileName.put(m_producer.getSeqNo(producerPrefix + "/data"), syncData);
    }



    public byte[] getSeqMap(long seq) {
        return m_seqToFileName.get(seq);
    }

    public static final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Log.d("onDataInterest", "Called OnInterestCallback with Interest: " + interest.getName().toUri());
            try {
                Data data = new Data(interest.getName());
                System.out.print(data.getContent());
                Blob content = new Blob(m_seqToFileName.get(m_producer.getSeqNo(producerPrefix + "/data")), false);
                data.setContent(new Blob(content));
                face.putData(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}
