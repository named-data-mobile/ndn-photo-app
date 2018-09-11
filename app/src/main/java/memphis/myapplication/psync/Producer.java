package memphis.myapplication.psync;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.*;

import memphis.myapplication.psync.State;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.tpm.TpmBackEnd.Error;
import net.named_data.jndn.util.Blob;


// Producer(/npChat, ownUserName, keyChain)
//  --> Register /npChat, parse for hello and sync
//  --> OnHelloInterest, send the latest sequence number back
//  --> OnSyncInterest Ignore if not /npChat/sync/ownUserName/<seq>
//      --> Store in pending sync interest if latest seq
//      --> Else look up seqno -> filename,friends
//  --> Publish(fileName, friendList)
//      --> Store in seqNo -> fileName, friends
//      --> Satisfy pending interests
public class Producer {
    private class
    PendingInterestInfo {
      public final ScheduledExecutorService expiryEvent = Executors.newScheduledThreadPool(1);
    }

    private Map<Name, PendingInterestInfo> m_pendingEntries = new HashMap<Name, PendingInterestInfo>();
    private Face m_face;
    private KeyChain m_keyChain;
    private Name m_userPrefix;
    private double m_syncReplyFreshness, m_helloReplyFreshness;

    private Long m_seq = new Long(0);

    // Need to cleanup periodically
    private Map<Long, Name> m_seqToFileName = new HashMap<Long, Name>();

	public Producer(Face face, Name syncPrefix, Name userPrefix,
                    double syncReplyFreshness, double helloReplyFreshness, KeyChain keyChain)
    {
        m_face = face;
        m_userPrefix = userPrefix;
        m_syncReplyFreshness = syncReplyFreshness;
        m_helloReplyFreshness = helloReplyFreshness;
        m_keyChain = keyChain;

		try {
		    Log.d("psync", "registering prefix " + syncPrefix);
			m_face.registerPrefix(syncPrefix, onInterest, onRegisterFailed);
			m_face.setInterestFilter(new InterestFilter(new Name(syncPrefix).append("hello")), onHelloInterest);
			m_face.setInterestFilter(new InterestFilter(new Name(syncPrefix).append("sync")), onSyncInterest);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private final OnInterestCallback onInterest = new OnInterestCallback() {
		public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
				InterestFilter filter) {
			System.out.print("Received interest: " + interest.toUri());
		}
	};

	private final OnInterestCallback onHelloInterest = new OnInterestCallback() {
		public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
							   InterestFilter filterData) {
			  System.out.println("Hello Interest Received, nonce: " + interest.toUri());
			  final Name interestName = interest.getName();

			  Log.d("producer", interestName.get(interestName.size()-1).toEscapedString());
              Log.d("producer", m_userPrefix.get(m_userPrefix.size()-1).toEscapedString());
			  if (!interestName.get(interestName.size()-1).equals(m_userPrefix.get(m_userPrefix.size()-1))) {
			      Log.d("producer", "Interest not for us");
			      return;
              }

			  Name helloDataName = interestName;
			  helloDataName.append(Component.fromNumber(m_seq));

			  Data data = new Data();
			  data.setName(helloDataName);
			  MetaInfo metaInfo = new MetaInfo();
              metaInfo.setFreshnessPeriod(m_helloReplyFreshness);
              data.setMetaInfo(metaInfo);

			  try {
				  m_keyChain.sign(data);
			  } catch (SecurityException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  } catch (Error e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  } catch (net.named_data.jndn.security.pib.PibImpl.Error e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  } catch (net.named_data.jndn.security.KeyChain.Error e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }

			  try {
				  m_face.putData(data);
			  } catch (IOException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }
		}
	};

    private final OnInterestCallback onSyncInterest;

    {
        onSyncInterest = new OnInterestCallback() {
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                                   InterestFilter filterData) {
                System.out.println("Sync Interest Received " + interest.toUri());
                final Name interestName = interest.getName();

                if (!interestName.get(interestName.size() - 2).equals(m_userPrefix.get(m_userPrefix.size()-1))) {
                    Log.d("Producer", "Sync interest not for us!");
                    return;
                }

                Long seq = interestName.get(interestName.size() - 1).toNumber();
                Log.d("producer", "Seq rcvd in interest: " + seq);
                Log.d("producer", "Our seq " + m_seq);

                State state = new State();
                if (seq < m_seq) {
                    while (seq < m_seq) {
                        Name friendsAndFile = m_seqToFileName.get(seq);
                        if (friendsAndFile == null) {
                            continue;
                        }
                        state.addContent(friendsAndFile);
                        seq++;
                    }
                }

                if (!state.getContent().isEmpty()) {
                    // send back data
                    Name syncDataName = interestName;
                    syncDataName.append(Component.fromNumber(m_seq));

                    Data data = new Data(syncDataName);
                    MetaInfo metaInfo = new MetaInfo();
                    metaInfo.setFreshnessPeriod(m_syncReplyFreshness);
                    data.setMetaInfo(metaInfo);
                    data.setContent(state.wireEncode());

                    try {
                        m_keyChain.sign(data);
                    } catch (SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Error e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (net.named_data.jndn.security.pib.PibImpl.Error e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (KeyChain.Error e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.out.println("Sending sync data");
                    try {
                        m_face.putData(data);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }

                Log.d("Producer", "Save interest!");
                PendingInterestInfo entry = m_pendingEntries.get(interestName);

                if (entry == null) {
                    entry = new PendingInterestInfo();
                    m_pendingEntries.put(interestName, entry);
                }

                entry.expiryEvent.schedule(new Runnable() {
                                               public void run() {
                                                   System.out.println("Deleting pending interest");
                                                   m_pendingEntries.remove(interestName);
                                               }
                                           },
                                           (long) interest.getInterestLifetimeMilliseconds(),
                                           MILLISECONDS);
            }
        };
    }

    public void
	publishName(final Name fileName, ArrayList<String> friendList) {
		m_seq = m_seq + 1;

		Log.d("Producer", "Going over pending interests");
		for (Name interestName: m_pendingEntries.keySet()) {

            Name encodedName = new Name(fileName);
            encodedName.append(new Name("friends"));
            for (String friend : friendList) {
                encodedName.append(new Name(friend));
            }

            State state = new State();
            state.addContent(encodedName);

            // Need to cleanup this map
            m_seqToFileName.put(m_seq, encodedName);

            Name syncDataName = interestName;
            syncDataName.append(Component.fromNumber(m_seq));

            Data data = new Data(syncDataName);
            MetaInfo metaInfo = new MetaInfo();
            metaInfo.setFreshnessPeriod(m_syncReplyFreshness);
            data.setMetaInfo(metaInfo);
            data.setContent(state.wireEncode());

            try {
              m_keyChain.sign(data);
            } catch (SecurityException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            } catch (Error e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            } catch (net.named_data.jndn.security.pib.PibImpl.Error e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            } catch (net.named_data.jndn.security.KeyChain.Error e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }

            System.out.println("Sending sync data " + data);

            try {
              m_face.putData(data);
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            m_pendingEntries.remove(interestName);
		}
	}

    protected final OnRegisterFailed onRegisterFailed = new OnRegisterFailed() {
        public void onRegisterFailed(Name arg0) {
        System.out.println("Register failed for: " + arg0);
        }
    };
}
