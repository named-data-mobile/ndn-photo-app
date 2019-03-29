package memphis.myapplication.psync;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;


import java.io.IOException;
import java.util.ArrayList;

import memphis.myapplication.FileManager;
import memphis.myapplication.Globals;
import memphis.myapplication.MainActivity;
import memphis.myapplication.R;
import memphis.myapplication.tasks.FetchingTask;

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
            FileManager manager = new FileManager(context);
            String filename = data.getContent().toString();
            Log.d("onData", interest.toUri());
            Log.d("onData", filename);
            new FetchingTask(activity).execute(new Interest(new Name(filename)));
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

    public static void createConsumer(String prefix) {
        consumer = new PSync.Consumer(prefix, helloDataCallBack, syncDataCallBack, 40, 0.001);
        consumers.add(consumer);
        consumer.sendHelloInterest();
    }
}