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
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.tpm.TpmBackEndFile;
import net.named_data.jndn.security.tpm.TpmKeyHandle;
import net.named_data.jndn.util.Blob;
import net.named_data.jni.psync.MissingDataInfo;
import net.named_data.jni.psync.PSync;


import java.io.IOException;
import java.util.ArrayList;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.FileManager;
import memphis.myapplication.Globals;
import memphis.myapplication.tasks.FetchingTask;
import memphis.myapplication.tasks.FetchingTaskParams;

public class ConsumerManager {

    static PSync.Consumer consumer;
    static ArrayList<PSync.Consumer> consumers = new ArrayList<>();
    static Activity activity;
    static Context context;
    static Face face;
    final static int filenameType = 100;
    final static int friendNameType = 101;
    final static int keyType = 102;
    final static int syncDataType = 999;
    final static int nameAndKeyType = 104;
    final static int ivType = 105;


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
            Blob interestData = data.getContent();
            Blob filename = null;
            Blob recipient = null;
            Blob symmetricKey = null;
            byte[] iv = null;

            TlvDecoder decoder = new TlvDecoder(interestData.buf());
            int endOffset = 0;
            try {
                endOffset = decoder.readNestedTlvsStart(syncDataType);
                while (decoder.getOffset() < endOffset) {
                    if (decoder.peekType(filenameType, endOffset)) {
                        filename = new Blob(decoder.readBlobTlv(filenameType), true);
                    }
                    else if (decoder.peekType(nameAndKeyType, endOffset)) {
                        int friendOffsetEnd = decoder.readNestedTlvsStart(nameAndKeyType);
                        while (decoder.getOffset() < friendOffsetEnd) {
                            if (decoder.peekType(keyType, friendOffsetEnd)) {
                            }
                            if (decoder.peekType(friendNameType, friendOffsetEnd)) {
                                recipient = new Blob(decoder.readBlobTlv(friendNameType), true);
                                if (recipient.toString().equals(manager.getUsername())) {
                                    iv = new Blob(decoder.readBlobTlv(ivType), true).getImmutableArray();
                                    symmetricKey = new Blob(decoder.readBlobTlv(keyType), true);
                                    decoder.finishNestedTlvs(friendOffsetEnd);
                                }
                                else {
                                    decoder.skipTlv(ivType);
                                    decoder.skipTlv(keyType);
                                }
                            }
                        }
                        decoder.finishNestedTlvs(friendOffsetEnd);
                    }
                }

                if (recipient.toString().equals(manager.getUsername())) {
                    // Decrypt symmetric key
                    TpmBackEndFile m_tpm = Globals.tpm;
                    TpmKeyHandle privateKey = m_tpm.getKeyHandle(Globals.pubKeyName);
                    Blob encryptedKeyBob = privateKey.decrypt(symmetricKey.buf());
                    byte[] encryptedKey = encryptedKeyBob.getImmutableArray();
                    SecretKey secretKey = new SecretKeySpec(encryptedKey, 0, encryptedKey.length, "AES");

                    new FetchingTask(activity).execute(new FetchingTaskParams(new Interest(new Name(filename.toString())), secretKey, iv));
                }

                decoder.finishNestedTlvs(endOffset);
            } catch (EncodingException e) {
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