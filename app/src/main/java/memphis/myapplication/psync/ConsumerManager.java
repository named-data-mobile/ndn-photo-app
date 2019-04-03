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
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
            String interestData = data.getContent().toString();
            Log.d("onData", interestData);
            String[] recipientList = interestData.split(":");
            System.out.print(recipientList[1]);
//            for (String friend : recipientList) {
//                if (friend.equals(manager.getUsername())) {
//                    new FetchingTask(activity).execute(new Interest(new Name(recipientList[0])));
//                }
//                else {
//                    Log.d("OnData", "Not for me, for " + friend);
//                }
//            }
            // /npChat/<username>
            Name appAndUsername = new Name("/npChat/" + manager.getUsername());

            try {
                Blob keyBlob = Globals.tpm.exportKey(new Name(appAndUsername), null);
                byte[] blobAsBytes = keyBlob.getImmutableArray();
                PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new X509EncodedKeySpec(blobAsBytes));
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                byte[] decryptedText = cipher.doFinal(recipientList[1].getBytes("UTF-8"));
                System.out.println("Decrypted text " + decryptedText);
                String text = new String(decryptedText, "UTF-8");
                System.out.println("String " + text);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
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

    public static void createConsumer(String prefix) {
        consumer = new PSync.Consumer(prefix, helloDataCallBack, syncDataCallBack, 40, 0.001);
        consumers.add(consumer);
        consumer.sendHelloInterest();
    }
}