package memphis.myapplication.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.SigningInfo;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Observable;

import memphis.myapplication.Globals;
import memphis.myapplication.R;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.data.RealmRepository;
import timber.log.Timber;

public class FriendRequest extends Observable {
    private Context context;
    private String m_newFriend;
    private String m_mutualFriend;
    private Interest m_signedInterest;
    private SharedPrefsManager sharedPrefsManager;

    protected int updateCode;

    final int UPDATE_NEW = 1;
    final int UPDATE_FAILED = 2;
    final int UPDATE_FRIEND = 3;
    final int UPDATE_TRUST = 4;

    // Outgoing friend request
    public FriendRequest(String nF, String mF, Context _context) {
        context = _context;
        m_newFriend = nF;
        m_mutualFriend = mF;
    }

    public void send() {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User user = realmRepository.saveNewFriend(m_newFriend, null, null);
        if (!user.haveTrust()) {
            Timber.d("new friend");
            String userPrefix = user.getNamespace();
            if (Globals.useMulticast) {
                try {
                    Nfdc.register(Globals.face, Globals.multicastFaceID, new Name(user.getNamespace()), 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }
            } else {
                Globals.nsdHelper.registerUser(user);
            }
            try {
                CertificateV2 certificate = realmRepository.getFriendCert(m_mutualFriend).getCert();
                Name name = new Name(userPrefix + "/friend-request/mutual-friend/");
                Timber.d("KeyName: " + Globals.pubKeyName +
                        ", CertName: " + certificate.getName());
                name.append(certificate.getName());

                Interest interest = new Interest(name);
                interest.setInterestLifetimeMilliseconds(600000);
                Globals.keyChain.sign(interest, new SigningInfo(SigningInfo.SignerType.KEY, Globals.pubKeyName));
                Timber.d("Express signed interest %s", interest.toUri());


                Globals.face.expressInterest(interest, onCertData, onFriendRequestTimeOut);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            } catch (PibImpl.Error error) {
                error.printStackTrace();
            } catch (KeyChain.Error error) {
                error.printStackTrace();
            }
        } else if (user.isFriend()) {
            // Already friends. Do nothing
            Timber.d("already friends");
        }
        realmRepository.close();
    }

    OnData onCertData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Timber.d("Got data packet with name %s", data.getName().toUri());
            final RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
            try {
                Blob interestData = data.getContent();
                if (data.getContent().toString().equals("Rejected")) {
                    Timber.d("Rejected");

                } else if (data.getContent().toString().equals("Friends")) {
                    Timber.d("Already friends");
                } else {
                    byte[] certBytes = interestData.getImmutableArray();

                    CertificateV2 certificateV2 = new CertificateV2();
                    certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                    Validator validator = new Validator(certificateV2, m_mutualFriend);
                    // If valid, then save their cert and get our cert signed by them
                    if (validator.valid()) {
                        // Save their cert
                        User user = realmRepository.saveNewFriend(m_newFriend, true, certificateV2);

                        // Get name for new cert
                        Name name = new Name(user.getNamespace());
                        name.append(context.getString(R.string.certificate_prefix));
                        Name certName = Globals.keyChain.getDefaultCertificateName();
                        Name newCertName = new Name();
                        newCertName.append(new Name(SharedPrefsManager.getInstance(context).getNamespace()));
                        newCertName.append(certName.getSubName(-4, 2));
                        newCertName.append(m_newFriend);
                        newCertName.append(certName.getSubName(-1, 1));
                        name.append(newCertName);
                        Interest certInterest = new Interest(name);

                        Globals.face.expressInterest(certInterest, new OnData() {
                            @Override
                            public void onData(Interest interest, Data data) {
                                // Got our cert signed by new friend, save and add them as a friend
                                RealmRepository innerRealmRepository = RealmRepository.getInstanceForNonUI();
                                Blob interestData = data.getContent();
                                byte[] certBytes = interestData.getImmutableArray();

                                CertificateV2 certificateV2 = new CertificateV2();
                                try {
                                    certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                                } catch (EncodingException e) {
                                    e.printStackTrace();
                                }

                                innerRealmRepository.setFriendCertificate(m_newFriend, certificateV2);

                                User user = innerRealmRepository.setFriendship(m_newFriend);
                                Globals.consumerManager.createConsumer(user.getNamespace());

                                Globals.producerManager.updateFriendsList();
                                innerRealmRepository.close();

                            }
                        }, onCertTimeOut);

                        requestSymKey(user.getNamespace());
                    }

                }

            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            realmRepository.close();

        }

    };

    // Incoming friend request
    public FriendRequest(Interest interest, Context _context) {
        context = _context;
        sharedPrefsManager = SharedPrefsManager.getInstance(context);
        m_signedInterest = interest;
    }

    public void receive() {
        Name interestName = m_signedInterest.getName();
        int friendComp = 0;
        for (int i = 0; i < interestName.size(); ++i) {
            if (interestName.getSubName(i, 1).toUri().contentEquals("/KEY")) {
                friendComp = i - 1;
            }
        }
        final String friend = interestName.getSubName(friendComp, 1).toUri().substring(1);
        m_newFriend = friend;

        final String mutual_friend = interestName.getSubName(friendComp + 3, 1).toUri().substring(1);
        m_mutualFriend = mutual_friend;
        Timber.d("Pending friend name: %s", friend);
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User user = realmRepository.saveNewFriend(m_newFriend, null, null);
        realmRepository.close();

        if (user.isFriend()) {
            setUpdateCode(UPDATE_FRIEND);
        } else if (user.haveTrust()) {
            setUpdateCode(UPDATE_TRUST);
        } else {

            if (Globals.useMulticast) {
                try {
                    Nfdc.register(Globals.face, Globals.multicastFaceID, new Name(user.getNamespace()), 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }
            } else {
                Globals.nsdHelper.registerUser(user);
            }

            String userPrefix = user.getNamespace();

            int start = 0;
            int end = 0;
            for (int i = 0; i < interestName.size(); ++i) {
                if (interestName.getSubName(i, 1).toUri().contentEquals("/mutual-friend")) {
                    start = i + 1;
                }

                if (interestName.getSubName(i, 1).toUri().contentEquals("/KEY")) {
                    end = i + 4;
                }
            }

            Name certNameToFetch = interestName.getSubName(start, end - start);
            Timber.d("Cert name to fetch: %s", certNameToFetch);
            Name newInterest = new Name(userPrefix);
            newInterest.append("cert");
            newInterest.append(certNameToFetch);
            Timber.d("Interest name to fetch: %s", newInterest);

            try {
                Globals.face.expressInterest(newInterest, new OnData() {

                    @Override
                    public void onData(Interest interest, Data data) {
                        Timber.d("Getting certificate from pending friend");
                        Blob interestData = data.getContent();
                        byte[] certBytes = interestData.getImmutableArray();

                        CertificateV2 certificateV2 = new CertificateV2();
                        try {
                            certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                        } catch (EncodingException e) {
                            e.printStackTrace();
                        }

                        Timber.d("Pending friend certificate: %s", certificateV2.getName().toUri());

                        Validator validator = new Validator(certificateV2, m_mutualFriend, m_signedInterest);
                        if (validator.valid()) {
                            Timber.d("Everything verified, saving friend's cert");

                            RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                            realmRepository.saveNewFriend(m_newFriend, true, certificateV2);
                            realmRepository.close();
                            // Send friend request response and our cert signed by mutual friend
                            setUpdateCode(UPDATE_NEW);
                        } else {
                            setUpdateCode(UPDATE_FAILED);

                        }

                    }
                });
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void accept() throws EncodingException {
        Timber.d("Accepting request and sending our cert back");
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        CertificateV2 myCert = realmRepository.getFriendCert(m_mutualFriend).getCert();
        realmRepository.close();
        Timber.d(myCert.toString());

        Data certData = new Data(m_signedInterest.getName());
        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
        tlvEncodedDataContent.writeBuffer(myCert.wireEncode().buf());
        byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
        Blob d = new Blob(finalDataContentByteArray);
        certData.setContent(d);
        certData.setMetaInfo(new MetaInfo());
        certData.getMetaInfo().setFreshnessPeriod(0);

        try {
            Globals.face.putData(certData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Now wait and ask for our cert back
        Thread thread = new Thread() {
            public void run() {
                Looper.prepare();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                        // Get name for new cert
                        User user = realmRepository.getFriend(m_newFriend);
                        Name name = new Name(user.getNamespace());
                        name.append(context.getString(R.string.certificate_prefix));
                        Name certName = null;
                        try {
                            certName = Globals.keyChain.getDefaultCertificateName();
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                        Name newCertName = new Name(SharedPrefsManager.getInstance(context).getNamespace());
                        newCertName.append(certName.getSubName(-4, 2));
                        newCertName.append(m_newFriend);
                        newCertName.append(certName.getSubName(-1, 1));
                        name.append(newCertName);
                        Interest certInterest = new Interest(name);
                        Timber.d("Getting our cert back " + name.toUri());


                        try {
                            Globals.face.expressInterest(certInterest, new OnData() {
                                @Override
                                public void onData(Interest interest, Data data) {
                                    // Got our cert signed by new friend, save and add them as a friend
                                    Timber.d("Got our cert back");
                                    Blob interestData = data.getContent();
                                    byte[] certBytes = interestData.getImmutableArray();

                                    CertificateV2 certificateV2 = new CertificateV2();
                                    try {
                                        certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                                    } catch (EncodingException e) {
                                        e.printStackTrace();
                                    }
                                    RealmRepository.getInstanceForNonUI().setFriendCertificate(m_newFriend, certificateV2);

                                    User friend = RealmRepository.getInstanceForNonUI().setFriendship(m_newFriend);
                                    Globals.consumerManager.createConsumer(friend.getNamespace());


                                }
                            }, new OnTimeout() {
                                @Override
                                public void onTimeout(Interest interest) {
                                    Timber.d("Timeout for interest %s", interest.toUri());
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        realmRepository.close();
                        requestSymKey(user.getNamespace());
                        handler.removeCallbacks(this);
                        Looper.myLooper().quit();
                    }
                }, 1500);

                Looper.loop();
            }
        };
        thread.start();
    }

    public void reject() {
        Timber.d("Rejecting request");
        Data data = new Data(m_signedInterest.getName());
        data.setContent(new Blob("Rejected"));
        try {
            Globals.face.putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptTrusted() {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User friend = realmRepository.setFriendship(m_newFriend);
        realmRepository.close();
        Globals.consumerManager.createConsumer(friend.getNamespace());
    }

    private void setUpdateCode(int c) {
        updateCode = c;
        setChanged();
        notifyObservers(c);
    }

    private void requestSymKey(String friendNamespace) {
        requestSymKey(friendNamespace, "default", SharedPrefsManager.getInstance(context).getUsername());
    }

    public static void requestSymKey(final String friendNameSpace, String keyName, String username) {
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
                }
            }, onSymKeyTimeOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    OnTimeout onCertTimeOut = new OnTimeout() {

        @Override
        public void onTimeout(Interest interest) {
            Timber.d("Time out for interest %s", interest.getName().toUri());
        }
    };

    OnTimeout onFriendRequestTimeOut = new OnTimeout() {

        @Override
        public void onTimeout(Interest interest) {
            try {
                Globals.face.expressInterest(interest, onCertData, onFriendRequestTimeOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    static OnTimeout onSymKeyTimeOut = new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {

        }
    };

    public String getPendingFriend() {
        return m_newFriend;
    }

}
