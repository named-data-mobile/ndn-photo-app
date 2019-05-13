package memphis.myapplication.psync;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.tpm.TpmKeyHandle;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;

import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.Decrypter;
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
                Log.d("onHelloDataCallBack", "Subscription added for " + name);

            }
            callbackConsumer.sendSyncInterest();
        }
    };

    static OnData onData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Log.d("ConsumerManager", "Got sync data");
            try {
                String interestData = new String(Base64.decode(data.getContent().getImmutableArray(), 0));
                Log.d("ConsumerManager", interestData);

                SyncData syncData = new SyncData(interestData);
                String filename = syncData.getFilename();
                Log.d("ConsumerManager", "Filename: " + filename);


                if (syncData.isFeed()) {
                    System.out.println("For feed");
                    new FetchingTask(activity).execute(new FetchingTaskParams(new Interest(new Name(filename)), null));
                } else {
                    if (syncData.forMe(SharedPrefsManager.getInstance(context).getUsername())) {
                        System.out.println("For me");
                        try {
                            Blob symmetricKey = new Blob(syncData.getFriendKey(SharedPrefsManager.getInstance(context).getUsername()), false);
                            TpmBackEndFile m_tpm = Globals.tpm;
                            TpmKeyHandle privateKey = m_tpm.getKeyHandle(Globals.pubKeyName);
                            Blob encryptedKeyBob = privateKey.decrypt(symmetricKey.buf());
                            byte[] encryptedKey = encryptedKeyBob.getImmutableArray();
                            SecretKey secretKey = new SecretKeySpec(encryptedKey, 0, encryptedKey.length, "AES");
                            System.out.println("Filename : " + filename);
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

    static PSync.OnSyncDataCallBack syncDataCallBack = new PSync.OnSyncDataCallBack() {
        @Override
        public void onSyncDataCallBack(ArrayList<MissingDataInfo> updates) {

            Log.d("OnSyncDataCallBack", "Got sync callback");
            for (MissingDataInfo update : updates) {

                Name name = new Name(update.prefix);
                name.appendSequenceNumber(update.highSeq);
                System.out.println(name);
                face = Globals.face;
                try {
                    System.out.println("Expressing interest for " + name);
                    face.expressInterest(name,  onData);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    };

    /**
     * Creates a new consumer for each friend and adds it to the ArrayList of consumers.
     * @param prefix is the String "/npChat/friendName"
     */
    public static void createConsumer(String prefix) {
        Log.d("ConsumerManager", "Adding friend " + prefix + " as consumer");
        consumer = new PSync.Consumer(prefix, helloDataCallBack, syncDataCallBack, 40, 0.001);
        consumers.add(consumer);
        consumer.sendHelloInterest();
    }

    public static void removeConsumer(String friend) {
        Log.d("ConsumerManager", "Removing " + friend + " as consumer");
        // Does nothing yet. Need to add.
        }

}