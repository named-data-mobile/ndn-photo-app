package memphis.myapplication;

import android.app.slice.Slice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

import io.realm.Realm;
import memphis.myapplication.RealmObjects.SelfCertificate;
import memphis.myapplication.RealmObjects.User;

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
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
        if ((user == null) || !(user.haveTrust())) {
            if (user == null)
                user = realm.createObject(User.class, m_newFriend);
            Log.d("FriendRequest", "new friend");
            String userPrefix = user.getNamespace();
            if (Globals.useMulticast) {
                try {
                    Nfdc.register(Globals.face, Globals.multicastFaceID, new Name(user.getNamespace()), 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }
            } else {
                Globals.nsdHelper.registerUser(m_newFriend);
            }
            realm.commitTransaction();
            realm.close();
            try {
                CertificateV2 certificate = realm.where(SelfCertificate.class).equalTo("username", m_mutualFriend).findFirst().getCert();
                Name name = new Name(userPrefix + "/friend-request/mutual-friend/");
                System.out.println("KeyName: " + Globals.pubKeyName +
                        ", CertName: " + certificate.getName());
                name.append(certificate.getName());

                Interest interest = new Interest(name);
                interest.setInterestLifetimeMilliseconds(600000);
                Globals.keyChain.sign(interest, new SigningInfo(SigningInfo.SignerType.KEY, Globals.pubKeyName));
                System.out.println("Express signed interest " + interest.toUri());


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
            Log.d("FriendRequest", "already friends");
            realm.cancelTransaction();
            realm.close();
        }

    }

    OnData onCertData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Log.d("FriendRequest", "Got data packet with name " + data.getName().toUri());
            Realm realm = Realm.getDefaultInstance();
            try {
                Blob interestData = data.getContent();
                if (data.getContent().toString().equals("Rejected")) {
                    Log.d("FriendRequest", "Rejected");

                } else if (data.getContent().toString().equals("Friends")) {
                    Log.d("FriendRequest", "Already friends");
                } else {
                    byte[] certBytes = interestData.getImmutableArray();

                    CertificateV2 certificateV2 = new CertificateV2();
                    certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                    Log.d("FriendRequest", "Got pending friend's cert " + certificateV2.toString());
                    Log.d("FriendRequest", "Friend name " + m_newFriend);
                    Log.d("FriendRequest", "Mutual friend name " + m_mutualFriend);
                    Validator validator = new Validator(certificateV2, m_mutualFriend, context);
                    // If valid, then save their cert and get our cert signed by them
                    if (validator.valid()) {
                        // Save their cert
                        realm.beginTransaction();
                        User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
                        user.setCert(certificateV2);
                        user.setTrust(true);
                        realm.commitTransaction();

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
                        Log.d("FriendRequest", "Asking for cert " + name.toUri());
                        realm.close();

                        Globals.face.expressInterest(certInterest, new OnData() {
                            @Override
                            public void onData(Interest interest, Data data) {
                                // Got our cert signed by new friend, save and add them as a friend
                                Blob interestData = data.getContent();
                                byte[] certBytes = interestData.getImmutableArray();

                                CertificateV2 certificateV2 = new CertificateV2();
                                try {
                                    certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                                } catch (EncodingException e) {
                                    e.printStackTrace();
                                }
                                Log.d("FriendRequest", "Got our cert back " + certificateV2.getName().toUri());
                                Realm realm = Realm.getDefaultInstance();
                                realm.beginTransaction();
                                SelfCertificate selfCertificate = realm.where(SelfCertificate.class).equalTo("username", m_newFriend).findFirst();
                                if (selfCertificate == null) {
                                    selfCertificate = realm.createObject(SelfCertificate.class, m_newFriend);
                                }
                                selfCertificate.setCert(certificateV2);
                                User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
                                user.setFriend(true);
                                Globals.consumerManager.createConsumer(user.getNamespace());

                                realm.commitTransaction();
                                realm.close();



                            }
                        }, onCertTimeOut);
                    }



                }

            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }



        }

    };

    // Incoming friend request
    public FriendRequest(Interest interest, Context _context) {
        context = _context;
        sharedPrefsManager = SharedPrefsManager.getInstance(context);
        m_signedInterest = interest;
    }

    public void receive() {
        Realm realm = Realm.getDefaultInstance();

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
        Log.d("onFriendRequestInterest", "Pending friend name: " + friend);
        realm.beginTransaction();
        User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
        if (user == null)
            user = realm.createObject(User.class, m_newFriend);

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
                Globals.nsdHelper.registerUser(m_newFriend);
            }

            String userPrefix = user.getNamespace();
            Log.d("FriendRequest", "User prefix " + userPrefix);
            realm.commitTransaction();
            realm.close();

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
            System.out.println("Cert name to fetch: " + certNameToFetch);
            Name newInterest = new Name(userPrefix);
            newInterest.append("cert");
            newInterest.append(certNameToFetch);
            System.out.println("Interest name to fetch: " + newInterest);

            try {
                Globals.face.expressInterest(newInterest, new OnData() {

                    @Override
                    public void onData(Interest interest, Data data) {
                        Log.d("onCertData", "Getting certificate from pending friend");
                        Blob interestData = data.getContent();
                        byte[] certBytes = interestData.getImmutableArray();

                        CertificateV2 certificateV2 = new CertificateV2();
                        try {
                            certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                        } catch (EncodingException e) {
                            e.printStackTrace();
                        }

                        Log.d("onCertData", "Pending friend certificate: " + certificateV2.getName());

                        Validator validator = new Validator(certificateV2, m_mutualFriend, m_signedInterest, context);
                        if (validator.valid()) {
                            Log.d("onCertData", "Everything verified, saving friend's cert");
                            Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
                            user.setCert(certificateV2);
                            user.setTrust(true);
                            realm.commitTransaction();
                            realm.close();

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
        Log.d("onCertData", "Accepting request and sending our cert back");
        Realm realm = Realm.getDefaultInstance();
        CertificateV2 myCert = realm.where(SelfCertificate.class).equalTo("username", m_mutualFriend).findFirst().getCert();
        Log.d("onCertData", myCert.toString());

        Data certData = new Data(m_signedInterest.getName());
        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
        tlvEncodedDataContent.writeBuffer(myCert.wireEncode().buf());
        byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
        Blob d = new Blob(finalDataContentByteArray);
        certData.setContent(d);
        certData.setMetaInfo(new MetaInfo());
        certData.getMetaInfo().setFreshnessPeriod(31536000000.0);
        realm.close();

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
                        // Get name for new cert
                        Realm realm = Realm.getDefaultInstance();
                        User user = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
                        Name name =  new Name(user.getNamespace());
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
                        Log.d("FriendRequest", "Getting our cert back " + name.toUri());


                        try {
                            Globals.face.expressInterest(certInterest, new OnData() {
                                @Override
                                public void onData(Interest interest, Data data) {
                                    // Got our cert signed by new friend, save and add them as a friend
                                    Log.d("FriendRequest", "Got our cert back");
                                    Blob interestData = data.getContent();
                                    byte[] certBytes = interestData.getImmutableArray();

                                    CertificateV2 certificateV2 = new CertificateV2();
                                    try {
                                        certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
                                    } catch (EncodingException e) {
                                        e.printStackTrace();
                                    }
                                    Realm realm = Realm.getDefaultInstance();
                                    realm.beginTransaction();
                                    SelfCertificate realmCertificate = realm.where(SelfCertificate.class).equalTo("username", m_newFriend).findFirst();
                                    if (realmCertificate == null)
                                        realmCertificate = realm.createObject(SelfCertificate.class, m_newFriend);

                                    realmCertificate.setCert(certificateV2);
                                    User friend = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
                                    friend.setFriend(true);
                                    Globals.consumerManager.createConsumer(friend.getNamespace());
                                    realm.commitTransaction();
                                    realm.close();



                                }
                            }, new OnTimeout() {
                                @Override
                                public void onTimeout(Interest interest) {
                                    Log.d("OnTimeout", "Timeout for interest " + interest.toUri());
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        handler.removeCallbacks(this);
                        Looper.myLooper().quit();
                    }
                }, 1500);

                Looper.loop();
            }
        };
        thread.start();
    }

    public void reject()  {
        Log.d("onCertData", "Rejecting request");
        Data data = new Data(m_signedInterest.getName());
        data.setContent(new Blob("Rejected"));
        try {
            Globals.face.putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void acceptTrusted() {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        User friend = realm.where(User.class).equalTo("username", m_newFriend).findFirst();
        friend.setFriend(true);
        Globals.consumerManager.createConsumer(friend.getNamespace());
        realm.commitTransaction();
        realm.close();

    }

    private void setUpdateCode(int c) {
        updateCode = c;
        setChanged();
        notifyObservers(c);
    }


    OnTimeout onCertTimeOut = new OnTimeout() {

        @Override
        public void onTimeout(Interest interest) {
            System.out.println("Time out for interest " + interest.getName().toUri());
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

    public String getPendingFriend(){
        return m_newFriend;
    }

}
