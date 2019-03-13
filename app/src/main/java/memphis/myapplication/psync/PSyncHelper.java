package memphis.myapplication.psync;

import android.app.Activity;
import android.util.Log;

import net.named_data.jndn.Name;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;


import java.util.ArrayList;

import memphis.myapplication.FileManager;
import memphis.myapplication.R;

public class PSyncHelper {

    public Activity activity;
    private String title;
    PSync.Consumer consumer;
    PSync psync;
    //ArrayList<PSync.Consumer> m_consumers = new ArrayList<PSync.Consumer>();

    public PSyncHelper(Activity _activity) {
        this.activity = _activity;
    }

    public ArrayList<PSync.Consumer> startConsumer(String friend, ArrayList<PSync.Consumer> m_consumers) {
        PSync.OnSyncDataCallBack onSyncDataCallBack = new PSync.OnSyncDataCallBack() {
            @Override
            public void onSyncDataCallBack(ArrayList<MissingDataInfo> updates) {
                Log.d("SyncDataCallBack", "Yeah");
                for (MissingDataInfo i : updates) {
                    System.out.println(i);
                }
//                    Log.d("Consumer", "Will fetch file: " + fileName);
//                    runOnUiThread(makeToast("Fetching: " + fileName));
//                    fetch_data(new Interest(fileName))
            }
        };
        PSync.OnHelloDataCallBack onHelloDataCallBack = new PSync.OnHelloDataCallBack() {
            @Override
            public void onHelloDataCallBack(ArrayList<String> names) {
                for (String name : names) {
                    if (name.contains("npChat")) {
                        Log.d("addSubscription", name);
                        consumer.addSubscription(name);
                    }
                    Log.d("onHelloDataCallBack", name);
                }
                consumer.sendSyncInterest();
                Log.d("MainActivity: startCon","Hello data callback");
            }
        };
//        FileManager manager = new FileManager(activity.getApplicationContext());
//        Name appAndUsername = new Name("/" + activity.getString(R.string.app_name) + "/" + manager.getUsername());
//        Name friendsUserName = new Name(friend);
        //Consumer consumer = new Consumer(new Name(getString(R.string.app_name)), appAndUsername, friendsUserName, face, onSyncData);
        consumer = new PSync.Consumer(activity.getString(R.string.app_name), onHelloDataCallBack, onSyncDataCallBack, 3, 40);
        m_consumers.add(consumer);
        consumer.sendHelloInterest();
        ArrayList<String> subList = consumer.getSubscriptionList();
        for (String s : subList){
            Log.d("Consumer", "subList");
            System.out.println(s);
        }
        //
        Log.d("Consumer", "Added consumer for friend for " + friend);
        return m_consumers;
    }


}
