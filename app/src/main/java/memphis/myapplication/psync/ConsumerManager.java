package memphis.myapplication.psync;

import android.content.Context;

import android.util.Base64;

import androidx.lifecycle.MutableLiveData;

import memphis.myapplication.data.FriendsList;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.tpm.TpmKeyHandle;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.Globals;
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.utilities.SyncData;
import memphis.myapplication.data.tasks.FetchingTask;
import memphis.myapplication.data.tasks.FetchingTaskParams;

public class ConsumerManager {

    static PSync.Consumer consumer;
    static HashMap<String, PSync.Consumer> consumers = new HashMap<>();
    static MutableLiveData<String> toastData;
    static Context context;
    static Face face;


    public ConsumerManager(Context _context, MutableLiveData<String> toastData) {
        this.toastData = toastData;
        this.context = _context;
    }

    static PSync.OnHelloDataCallBack helloDataCallBack = new PSync.OnHelloDataCallBack() {
        @Override
        public void onHelloDataCallBack(ArrayList<String> names, PSync.Consumer callbackConsumer) {
            for (String name : names) {
                callbackConsumer.addSubscription(name);
                Timber.d("Subscription added for %s", name);

            }
            callbackConsumer.sendSyncInterest();
        }
    };

    private OnData onFileData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Timber.d( "Got sync data for /data");
            try {
                String interestData = new String(Base64.decode(data.getContent().getImmutableArray(), 0));
                Timber.d(interestData);



                SyncData syncData = new SyncData(interestData);
                String filename = syncData.getFilename();
                Timber.d("Filename: " + filename);


                if (syncData.isFeed()) {
                    Timber.d("For feed");
                    new FetchingTask(context, toastData).execute(new FetchingTaskParams(new Interest(new Name(filename)), null));
                } else {
                    if (syncData.forMe(SharedPrefsManager.getInstance(context).getUsername())) {
                        Timber.d("For me");
                        try {
                            Blob symmetricKey = new Blob(syncData.getFriendKey(SharedPrefsManager.getInstance(context).getUsername()), false);
                            TpmBackEndFile m_tpm = Globals.tpm;
                            TpmKeyHandle privateKey = m_tpm.getKeyHandle(Globals.pubKeyName);
                            Blob encryptedKeyBob = privateKey.decrypt(symmetricKey.buf());
                            byte[] encryptedKey = encryptedKeyBob.getImmutableArray();
                            SecretKey secretKey = new SecretKeySpec(encryptedKey, 0, encryptedKey.length, "AES");
                            Timber.d("Filename : " + filename);
                            new FetchingTask(context, toastData).execute(new FetchingTaskParams(new Interest(new Name(filename)), secretKey));
                        } catch (TpmBackEnd.Error error) {
                            error.printStackTrace();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }
    };

    private OnData onFriendsData = new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
            try {
            Timber.d( "Got sync data for /friends");
            String interestData = new String(Base64.decode(data.getContent().getImmutableArray(), 0));
            String friendName = interest.getName().getSubName(-3, 1).toUri().substring(1);
            Timber.d(interestData);
            Timber.d(friendName);

            FriendsList friendsList = new FriendsList(interestData);
            FriendsList myFriendsList = new FriendsList();
            myFriendsList.addNew(friendsList, friendName, SharedPrefsManager.getInstance(context).getNamespace());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    PSync.OnSyncDataCallBack syncDataCallBack = new PSync.OnSyncDataCallBack() {
        @Override
        public void onSyncDataCallBack(ArrayList<MissingDataInfo> updates) {

            Timber.d("Got sync callback");
            for (MissingDataInfo update : updates) {

                Name name = new Name(update.prefix);
                name.appendSequenceNumber(update.highSeq);
                face = Globals.face;
                Timber.d(name.getSubName(-2,1).toUri());
                if (name.getSubName(-2,1).toUri().equals("/friends")) {
                    try {
                        Timber.d("Expressing interest for friends list");
                        face.expressInterest(name,  onFriendsData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (name.getSubName(-2,1).toUri().equals("/data")) {
                    try {
                        Timber.d("Expressing interest for published file");
                        face.expressInterest(name, onFileData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }
    };

    /**
     * Creates a new consumer for each friend and adds it to the ArrayList of consumers.
     * @param prefix is the String "/npChat/friendName"
     */
    public void createConsumer(String prefix) {

        Timber.d("Adding friend " + prefix + "as consumer");
        consumer = new PSync.Consumer(prefix, helloDataCallBack, syncDataCallBack, 40, 0.001);
        consumers.put(prefix, consumer);
        consumer.sendHelloInterest();
    }

    public void removeConsumer(String friend) {
        Timber.d("Removing " + friend + " as consumer");
        PSync.Consumer removedConsumer = consumers.get(friend);
        consumers.remove(friend);
        removedConsumer.stop();
        removedConsumer = null;
    }
}