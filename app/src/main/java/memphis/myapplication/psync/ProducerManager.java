package memphis.myapplication.psync;
import android.util.Base64;

import memphis.myapplication.data.FriendsList;
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
    public  PSync.PartialProducer m_producer;
    private  Map<Long, String> m_seqToFileName = new HashMap<Long, String>();
    private  String producerPrefix;
    private  String dataPrefix;
    private  String friendsPrefix;

    public ProducerManager(String p) {
        producerPrefix = p;
        dataPrefix = producerPrefix + "/data";
        friendsPrefix = producerPrefix + "/friends";
        m_producer = new PSync.PartialProducer(80, producerPrefix, dataPrefix, 500, 1000);
        m_producer.addUserNode(friendsPrefix);
    }

    public void setDataSeqMap(String syncData) {
        m_seqToFileName.put(m_producer.getSeqNo(dataPrefix) + 1, syncData);
    }

    public void updateFriendsList() {
        m_producer.publishName(friendsPrefix);

    }

    public void publishFile(String name) {
        m_producer.publishName(name);

    }




    public String getSeqMap(long seq) {
        return m_seqToFileName.get(seq);
    }

    public final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Timber.d("Called OnInterestCallback with Interest: %s", interest.getName().toUri());
            try {
                Data data = new Data(interest.getName());
                Blob content = new Blob(Base64.encode(m_seqToFileName.get(m_producer.getSeqNo(dataPrefix)).getBytes(), 0));
                data.setContent(new Blob(content));
                face.putData(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public OnInterestCallback onFriendsListInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            Timber.d("Called OnInterestCallback with Interest: %s", interest.getName().toUri());
            try {
                Data data = new Data(interest.getName());
                FriendsList friendsList = new FriendsList();
                String friends = friendsList.stringify();
                Blob content = new Blob(Base64.encode(friends.getBytes(), 0));
                data.setContent(new Blob(content));
                face.putData(data);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
