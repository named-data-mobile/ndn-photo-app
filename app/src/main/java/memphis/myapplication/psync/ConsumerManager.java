package memphis.myapplication.psync;

import android.app.Activity;
import android.content.Context;

import android.util.Base64;

import memphis.myapplication.FriendsList;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.Globals;
import memphis.myapplication.SharedPrefsManager;
import memphis.myapplication.SyncData;
import memphis.myapplication.tasks.FetchingTask;
import memphis.myapplication.tasks.FetchingTaskParams;

public class ConsumerManager {

    static PSync.Consumer consumer;
    static ArrayList<PSync.Consumer> consumers = new ArrayList<>();
    static Activity activity;
    static Context context;
    static Face face;



    public ConsumerManager(Activity _activity, Context _context) {
        this.activity = _activity;
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
                    new FetchingTask(activity).execute(new FetchingTaskParams(new Interest(new Name(filename)), null));
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
                            new FetchingTask(activity).execute(new FetchingTaskParams(new Interest(new Name(filename)), secretKey));
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
        consumers.add(consumer);
        consumer.sendHelloInterest();
    }

    public void removeConsumer(String friend) {
        Timber.d("Removing " + friend + " as consumer");
        // Does nothing yet. Need to add.
        }

}