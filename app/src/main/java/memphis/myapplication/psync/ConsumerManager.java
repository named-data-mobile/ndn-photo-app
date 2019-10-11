package memphis.myapplication.psync;

import android.content.Context;
import android.util.Base64;

import androidx.lifecycle.MutableLiveData;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import memphis.myapplication.Globals;
import memphis.myapplication.data.Common;
import memphis.myapplication.data.FriendsList;
import memphis.myapplication.data.RealmRepository;
import memphis.myapplication.data.tasks.FetchingTask;
import memphis.myapplication.data.tasks.FetchingTaskParams;
import memphis.myapplication.utilities.BloomFilter;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.utilities.Metadata;
import memphis.myapplication.utilities.SharedPrefsManager;
import timber.log.Timber;

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
                if (name.substring(name.lastIndexOf("/")).equals("/data")) {
                    RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                    String firstCut = name.substring(0, name.lastIndexOf("/"));
                    String friendName = firstCut.substring(firstCut.lastIndexOf("/")).substring(1);
                    if (realmRepository.getSeqNo(friendName) == 0) {
                        realmRepository.setSeqNo(friendName, callbackConsumer.getSeqNo(name));
                    }
                }
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
                String friendName = Common.interestToUsername(interest);
                if (!RealmRepository.getInstanceForNonUI().getFriend(friendName).isFriend())
                    return;

                Metadata metadata = new Metadata(interestData);
                String filename = metadata.getFilename();
                String fileInfoName = FileManager.getFileName(new Name(metadata.getFilename()), FileManager.FILENAME);

                String producer = metadata.getFilename().substring(0, metadata.getFilename().indexOf("/file"));
                RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                Timber.d("isFile:  " + metadata.isFile());
                realmRepository.saveNewFile(fileInfoName, metadata.isFeed(), metadata.isLocation(), metadata.isFile(), producer);
                realmRepository.close();

                if (metadata.isFeed()) {
                    Timber.d("For feed");
                    new FetchingTask(context, toastData).execute(new FetchingTaskParams(new Interest(new Name(filename)), true));

                } else {
                    BloomFilter bloomFilter = new BloomFilter(metadata.getBloomFilter().get(0).toNumber(), metadata.getBloomFilter().get(-1));
                    if (bloomFilter.contains(SharedPrefsManager.getInstance(context).getUsername())) {
                        Timber.d("For me");
                        new FetchingTask(context, toastData).execute(new FetchingTaskParams(new Interest(new Name(filename)), false));

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();

            } catch (Exception e) {
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
            String friendName = interest.getName().getSubName(-2, 1).toUri().substring(1);
            Timber.d(interestData);
            Timber.d("Friend " + friendName);

            FriendsList friendsList = new FriendsList(interestData);
            FriendsList myFriendsList = new FriendsList();
            myFriendsList.addNew(friendsList, friendName, SharedPrefsManager.getInstance(context).getNamespace());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    private OnData onKeyData = new OnData() {
        @Override
        public void onData(Interest interest, Data data) {

        }
    };

    PSync.OnSyncDataCallBack syncDataCallBack = new PSync.OnSyncDataCallBack() {
        @Override
        public void onSyncDataCallBack(ArrayList<MissingDataInfo> updates) {

            Timber.d("Got sync callback");
            for (MissingDataInfo update : updates) {
                Timber.d("Name: %s",update.prefix);

                Name name = new Name(update.prefix);
                face = Globals.face;
                Timber.d(name.getSubName(-1,1).toUri());
                if (name.getSubName(-1,1).toUri().equals("/friends")) {
                    try {
                        Timber.d("Expressing interest for friends list");

                        face.expressInterest(name,  onFriendsData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (name.getSubName(-1,1).toUri().equals("/data")) {
                    try {
                        Timber.d("Expressing interest for published file(s)");
                        long hiNo = update.highSeq;
                        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
                        String friendName = Common.interestToUsername(new Interest(name));
                        long lowNo = realmRepository.getSeqNo(friendName);
                        RealmRepository.getInstanceForNonUI().setSeqNo(friendName, hiNo);
                        for (long i = lowNo+1; i <= hiNo; i++) {
                            Timber.d("Fetching seqNo: %s", i);
                            face.expressInterest(new Name(name).appendSequenceNumber(i), onFileData);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (EncodingException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (name.getSubName(-1,1).toUri().equals("/keys")) {
                    Timber.d("Expressing interest for friend's key");
                    name.append(SharedPrefsManager.getInstance(context).getUsername());
                    try {
                        face.expressInterest(name, onKeyData);
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
        Timber.d("Adding friend " + prefix + " as consumer");
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
        Globals.producerManager.updateFriendsList();
    }
}