package memphis.myapplication.psync;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;

import java.io.IOException;
import java.util.ArrayList;

import memphis.myapplication.Decrypter;
import memphis.myapplication.Globals;
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
            Blob interestData = data.getContent();
            System.out.println("interest: " + interest);
            Decrypter decrypter = new Decrypter(context);
            FetchingTaskParams fetchingTaskParams = null;
            try {
                fetchingTaskParams = decrypter.decodeSyncData(interestData);
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            } catch (EncodingException e) {
                e.printStackTrace();
            }
            if (fetchingTaskParams != null) {
                new FetchingTask(activity).execute(fetchingTaskParams);
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
        Log.d("ConsumerManager", "Adding friend " + prefix + "as consumer");
        consumer = new PSync.Consumer(prefix, helloDataCallBack, syncDataCallBack, 40, 0.001);
        consumers.add(consumer);
        consumer.sendHelloInterest();
    }
}