package memphis.myapplication.psync;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.NetworkNack;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnNetworkNack;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import memphis.myapplication.R;
import memphis.myapplication.psync.State;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

// Create a new consumer when a new friend is added
// Consumer(/npChat, friendsUserName)
//  --> Send hello interest to /npChat/hello/friendsUserName to discover the latest sequence number
//  --> Send periodic interest to /npChat/sync/friendsUserName/<seq-no>
//  --> onSyncData -> let application know that it needs to fetch data by calling fetch data
public class Consumer {

    private Name m_syncPrefix, m_userName, m_friendsUserName;
    private Face m_face;
    private ReceiveSyncCallback m_onReceivedSyncData;
    private Long m_seq;
    private long m_outstandingInterestId = 0;

    private Name m_helloInterestName, m_syncInterestName;

    private final ScheduledExecutorService m_expiryEvent = Executors.newScheduledThreadPool(1);

    public Consumer(Name syncPrefix,
                    Name userName,
                    Name friendsUserName,
                    Face face,
                    ReceiveSyncCallback onReceivedSyncData)
    {
    	m_syncPrefix = syncPrefix;
        m_userName = userName;
        m_friendsUserName = friendsUserName;

        m_face = face;
        m_onReceivedSyncData = onReceivedSyncData;

        m_helloInterestName = new Name(m_syncPrefix);
        m_helloInterestName.append("hello");
        m_helloInterestName.append(m_friendsUserName);

        m_syncInterestName = new Name(m_syncPrefix);
        m_syncInterestName.append("sync");
        m_syncInterestName.append(m_friendsUserName);
        sendHelloInterest();
    }

    public interface ReceiveSyncCallback {
        void onReceivedSyncData(Name fileName);
    }

    public void sendHelloInterest() {
        Interest helloInterest = new Interest(m_helloInterestName);
        helloInterest.setInterestLifetimeMilliseconds(60000);
        helloInterest.setMustBeFresh(true);

        System.out.println("Send hello interest " + helloInterest.toUri());

        try {
            m_face.expressInterest(helloInterest,
                                   onHelloDataCallback,
                                   onHelloTimeout,
                                   onHelloNack);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private OnData onHelloDataCallback = new OnData() {
        public void onData(Interest interest, Data data) {
            Name helloDataName = data.getName();

            m_seq =  helloDataName.get(helloDataName.size()-1).toNumber();

            sendSyncInterest();
        }
    };

    private OnTimeout onHelloTimeout = new OnTimeout() {
        public void onTimeout(Interest interest) {
            System.out.println("Timeout for interest " + interest.getName().toString());
            sendHelloInterest();
        }
    };

    private OnNetworkNack onHelloNack = new OnNetworkNack() {
		public void onNetworkNack(Interest interest, NetworkNack networkNack) {
            System.out.println("Nack for interest " + interest.getName().toUri());
            m_expiryEvent.schedule(new Runnable() {
                public void run() {
                    sendHelloInterest();
                }
            }, (long) 60000,  MILLISECONDS);
		}
    };

    private void sendSyncInterest() {
    	// Sync interest format for partial: /<sync-prefix>/sync/<user-name>
    	Name syncInterestName = new Name(m_syncInterestName);
        syncInterestName.append(Name.Component.fromNumber(m_seq));

        Interest syncInterest = new Interest(syncInterestName);
        syncInterest.setInterestLifetimeMilliseconds(60000);
        syncInterest.setMustBeFresh(true);

        System.out.println("Send sync interest " + syncInterestName.toUri());

        try {
        	if (m_outstandingInterestId != 0) {
        		m_face.removePendingInterest(m_outstandingInterestId);
        	}

        	m_outstandingInterestId = m_face.expressInterest(syncInterest,
				                                             onSyncDataCallback,
				                                             onSyncTimeout,
				                                             onSyncNack);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private OnData onSyncDataCallback = new OnData() {
        public void onData(Interest interest, Data data) {
            Log.d("Consumer", "Received sync data " + data.getName().toUri());
        	Name syncDataName = data.getName();
        	m_seq = syncDataName.get(syncDataName.size()-1).toNumber();

        	State state = new State();
        	try {
				state.wireDecode(data.getContent().buf());
			} catch (EncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        	for (Name content : state.getContent()) {
                // <fileName>/friends/<username>/<username>/...
        	    Log.d("Consumer ", content.toUri());

        	    int friendPos = -1;
        	    for (int i = 0; i < content.size(); ++i) {
                    if (content.get(i).equals(new Name.Component("friends"))) {
                        friendPos = i;
                        break;
                    }
                }

                Name fileName = content.getSubName(0, friendPos);
        	    Name userNames = content.getSubName(friendPos + 1);

                boolean shouldFetch = false;
        	    for (int i = 0; i < userNames.size(); ++i) {
        	        if (userNames.get(i).equals(new Name.Component(m_userName.get(m_userName.size()-1)))) {
                        shouldFetch = true;
                        break;
                    }
                }

                if (shouldFetch) {
                    m_onReceivedSyncData.onReceivedSyncData(fileName);
                }
        	}
        	sendSyncInterest();
        }
    };

    private OnTimeout onSyncTimeout = new OnTimeout() {
        public void onTimeout(Interest interest) {
            System.out.println("Timeout for interest " + interest.getName().toString());
            sendSyncInterest();
        }
    };
            
    private OnNetworkNack onSyncNack = new OnNetworkNack() {
		public void onNetworkNack(Interest interest, NetworkNack networkNack) {
			System.out.println("Nack for interest " + interest.getName().toUri());
            m_expiryEvent.schedule(new Runnable() {
                public void run() {
                    sendSyncInterest();
                }
            }, (long) 60000,  MILLISECONDS);
		}
    };
}