package memphis.myapplication.psync;
import android.content.Context;
import android.util.Base64;

import io.realm.Realm;
import memphis.myapplication.data.FriendsList;
import memphis.myapplication.data.RealmRepository;
import memphis.myapplication.utilities.SharedPrefsManager;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encrypt.algo.EncryptAlgorithmType;
import net.named_data.jndn.encrypt.algo.EncryptParams;
import net.named_data.jndn.encrypt.algo.RsaAlgorithm;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.PSync;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class ProducerManager {
    public  PSync.PartialProducer m_producer;
    private  String producerPrefix;
    private  String dataPrefix;
    private  String friendsPrefix;
    private String keysPrefix;
    private Context context;
    SharedPrefsManager sharedPrefsManager;

    public ProducerManager(String p, Context c) {
        context = c;
        sharedPrefsManager = SharedPrefsManager.getInstance(context);
        producerPrefix = p;
        dataPrefix = producerPrefix + "/data";
        friendsPrefix = producerPrefix + "/friends";
        keysPrefix = producerPrefix + "/keys";

        m_producer = new PSync.PartialProducer(80, producerPrefix, dataPrefix, 500, 1000);
        m_producer.addUserNode(friendsPrefix);
        m_producer.addUserNode(keysPrefix);
    }

    private void setDataSeqMap(String syncData, long seq) {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        realmRepository.saveSyncData(seq, syncData);
        realmRepository.close();
    }

    public void updateFriendsList() {
        if (sharedPrefsManager.getSharing())
            m_producer.publishName(friendsPrefix);
    }

    public void updateKey() {
        m_producer.publishName(keysPrefix);
    }

    public void publishFile(String name, String syncData) {
        long seq = sharedPrefsManager.getSeqNum() + 1;
        setDataSeqMap(syncData, seq);

        Timber.d("Publishing seqNo: " + seq);
        m_producer.publishName(name, seq);
        sharedPrefsManager.setSeqNum(seq);
    }

    public final OnInterestCallback onDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                               InterestFilter filterData) {
            Timber.d("Called OnInterestCallback with Interest: %s", interest.getName().toUri());
            try {
                Data data = new Data(interest.getName());
                long seqNo = interest.getName().get(-1).toSequenceNumber();
                Timber.d("SeqNo: " + seqNo);
                // Get string from DB here
                RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                Blob content = new Blob(Base64.encode(realmRepository.getSyncData(seqNo).getBytes(), 0));
                data.setContent(new Blob(content));
                face.putData(data);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (EncodingException e) {
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

    public OnInterestCallback onKeyInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            try {
                // Currently only saving our most recent key
                Timber.d("On interest for our key " + interest.getName());
                Blob friendKey = RealmRepository.getInstanceForNonUI().getFriend(interest.getName().getSubName(-1).toUri().substring(1))
                        .getCert().getPublicKey();
                SecretKey secretKey = SharedPrefsManager.getInstance(context).getKey();

                byte[] encryptedKey = RsaAlgorithm.encrypt
                        (friendKey, new Blob(secretKey.getEncoded()), new EncryptParams(EncryptAlgorithmType.RsaOaep)).getImmutableArray();
                Data data = new Data();
                data.setContent(new Blob(encryptedKey));
                data.setName(interest.getName());
                face.putData(data);
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            } catch (CertificateV2.Error error) {
                error.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
