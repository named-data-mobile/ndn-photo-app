package memphis.myapplication.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.VerificationHelpers;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import memphis.myapplication.Globals;
import memphis.myapplication.data.RealmObjects.PublishedContent;
import memphis.myapplication.data.RealmObjects.User;
import memphis.myapplication.utilities.BloomFilter;
import memphis.myapplication.utilities.Encrypter;
import memphis.myapplication.utilities.FileManager;
import memphis.myapplication.utilities.FriendRequest;
import memphis.myapplication.utilities.Metadata;
import memphis.myapplication.utilities.QRExchange;
import memphis.myapplication.utilities.SharedPrefsManager;
import memphis.myapplication.viewmodels.RealmViewModel;
import timber.log.Timber;

import static memphis.myapplication.Globals.consumerManager;
import static memphis.myapplication.Globals.producerManager;


public class Common {


    /**
     * Starts a new thread to publish the file/photo data.
     *
     * @param blob   Blob of content
     * @param prefix Name of the file (currently absolute path)
     */
    public static void publishData(final Blob blob, final Name prefix) {
        Thread publishingThread = new Thread(new Runnable() {
            public void run() {
                try {

                    ArrayList<Data> fileData = new ArrayList<>();
                    ArrayList<Data> packets = packetize(blob, prefix);
                    // it would be null if this file is already in our cache so we do not packetize
                    if (packets != null) {
                        Timber.d("Publishing with prefix: " + prefix);
                        for (Data data : packets) {
                            Globals.keyChain.sign(data);
                            fileData.add(data);
                        }
                    }
                    Globals.memoryCache.putInCache(fileData);
                } catch (PibImpl.Error | SecurityException | TpmBackEnd.Error |
                        KeyChain.Error e) {
                    e.printStackTrace();

                }
            }
        });
        publishingThread.start();
    }

    /**
     * This takes a Blob and divides it into NDN data packets
     *
     * @param raw_blob The full content of data in Blob format
     * @param prefix
     * @return returns an ArrayList of all the data packets
     */
    private static ArrayList<Data> packetize(Blob raw_blob, Name prefix) {
        final int VERSION_NUMBER = 0;
        final int DEFAULT_PACKET_SIZE = 8000;
        int PACKET_SIZE = (DEFAULT_PACKET_SIZE > raw_blob.size()) ? raw_blob.size() : DEFAULT_PACKET_SIZE;
        ArrayList<Data> datas = new ArrayList<>();
        int segment_number = 0;
        ByteBuffer byteBuffer = raw_blob.buf();
        do {
            // need to check for the size of the last segment; if lastSeg < PACKET_SIZE, then we
            // should not send an unnecessarily large packet. Also, if smaller, we need to prevent BufferUnderFlow error
            if (byteBuffer.remaining() < PACKET_SIZE) {
                PACKET_SIZE = byteBuffer.remaining();
            }
            Timber.d("packetizing: %s", "PACKET_SIZE: " + PACKET_SIZE);
            byte[] segment_buffer = new byte[PACKET_SIZE];
            Data data = new Data();
            Name segment_name = new Name(prefix);
            segment_name.appendVersion(VERSION_NUMBER);
            segment_name.appendSegment(segment_number);
            data.setName(segment_name);
            try {
                Timber.d("packetizing: %s", "full data name: " + data.getFullName().toString());
            } catch (EncodingException e) {
                Timber.d("packetizing: %s", "unable to print full name");
            }
            try {
                Timber.d("packetizing: %s", "byteBuffer position: " + byteBuffer.position());
                byteBuffer.get(segment_buffer, 0, PACKET_SIZE);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            data.setContent(new Blob(segment_buffer));
            MetaInfo meta_info = new MetaInfo();
            meta_info.setType(ContentType.BLOB);
            // not sure what is a good freshness period
            meta_info.setFreshnessPeriod(90000);
            datas.add(data);
            if (!byteBuffer.hasRemaining()) {
                // Set the final component to have a final block id.
                Name.Component finalBlockId = Name.Component.fromSegment(segment_number);
                meta_info.setFinalBlockId(finalBlockId);
                datas.get(0).setMetaInfo(meta_info);
                datas.get(datas.size() - 1).setMetaInfo(meta_info);
            }
            segment_number++;
        } while (byteBuffer.hasRemaining());
        return datas;
    }

    /**
     * Generates and expresses interest for our certificate signed by friend
     *
     * @param friend: name of friend who has our certificate
     */
    public static void generateCertificateInterest(String friend) throws SecurityException, IOException {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User user = realmRepository.getFriend(friend);
        realmRepository.close();
        Name name = new Name(user.getNamespace());
        name.append("cert");
        Name certName = Globals.keyChain.getDefaultCertificateName();
        Name newCertName = new Name();
        int end = 0;
        for (int i = 0; i <= certName.size(); i++) {
            if (certName.getSubName(i, 1).toUri().equals("/self")) {
                newCertName.append(certName.getPrefix(i));
                end = i;
                break;
            }
        }
        newCertName.append(friend);
        newCertName.append(certName.getSubName(end + 1));
        name.append(newCertName);
        Interest interest = new Interest(name);
        Timber.d("Expressing interest for our cert %s", name.toUri());
        registerUser(user);
        Globals.face.expressInterest(interest, onCertData, onCertTimeOut);
    }

    public static void registerUser(User friend) {
        if (!Globals.useMulticast) {
            Globals.nsdHelper.registerUser(friend);
        } else {
            try {
                Nfdc.register(Globals.face, Globals.multicastFaceID, new Name(friend.getNamespace()), 0);
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Callback for certificate from friend
     */
    static OnData onCertData = new OnData() {

        @Override
        public void onData(Interest interest, Data data) {
            Timber.d("Getting our certificate back from friend");
            RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();

            String friendName = interest.getName().getSubName(-2, 1).toUri().substring(1);
            String userName = interest.getName().getSubName(-5, 1).toUri().substring(1);
            Blob interestData = data.getContent();
            byte[] certBytes = interestData.getImmutableArray();

            CertificateV2 certificateV2 = new CertificateV2();
            try {
                certificateV2.wireDecode(ByteBuffer.wrap(certBytes));
            } catch (EncodingException e) {
                e.printStackTrace();
            }

            User friend = realmRepository.saveNewFriend(friendName, true, null);
            realmRepository.setFriendCert(friendName, certificateV2);

            VerificationHelpers verificationHelpers = new VerificationHelpers();
            try {
                boolean verified = verificationHelpers.verifyDataSignature(certificateV2, realmRepository.getFriend(friendName).getCert());
            } catch (EncodingException e) {
                e.printStackTrace();
            }

            Timber.d("Saved our certificate back signed by friend and adding them as a consumer");
            consumerManager.createConsumer(friend.getNamespace());
            realmRepository.toast().postValue("Successfully added " + friendName);

            FriendRequest.requestSymKey(friend.getNamespace(), "default", userName);

            // Share friend's list
            producerManager.updateFriendsList();

            if (!Globals.useMulticast) {
                Globals.nsdHelper.registerUser(friend);
            } else {
                User user = realmRepository.getFriend(friendName);
                try {
                    Nfdc.register(Globals.face, Globals.multicastFaceID, new Name(user.getNamespace()), 0);
                } catch (ManagementException e) {
                    e.printStackTrace();
                }
            }
            realmRepository.close();
        }
    };

    /**
     * Callback for timeout of interest for certificate from friend
     */
    static OnTimeout onCertTimeOut = new OnTimeout() {

        @Override
        public void onTimeout(Interest interest) {
            Timber.d("Timeout for interest " + interest.toUri());
            String friend = interest.getName().getSubName(-2, 1).toString().substring(1);
            Timber.d("Resending interest to " + friend);
            try {
                generateCertificateInterest(friend);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * encodes sync data, encrypts photo, and publishes filename and symmetric keys
     *
     * @param resultData: intent with filename and recipients list
     */
    public static void encryptAndPublish(Intent resultData, Context context, boolean isFile) {
        RealmViewModel databaseViewModel = ViewModelProviders.of((FragmentActivity) context).get(RealmViewModel.class);
        try {
            final String path = resultData.getStringExtra("photo");
            final File photo = new File(path);
            boolean location = false;
            double latitude;
            double longitude;
            if (resultData.getExtras().getBoolean("location")) {
                location = true;
                Bundle params = resultData.getExtras();
                latitude = params.getDouble("latitude");
                longitude = params.getDouble("longitude");
                Timber.d("Adding location: " + latitude + " : " + longitude);

                ExifInterface exif;
                try {
                    exif = new ExifInterface(photo.getAbsolutePath());
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convert(latitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude < 0.0d ? "S" : "N");
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convert(longitude));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude < 0.0d ? "W" : "E");
                    exif.saveAttributes();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error in sending location", Toast.LENGTH_SHORT).show();
                }
            }
            Timber.d("File size: " + photo.length());
            final Uri uri = FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() +
                            ".fileProvider", photo);
            final Encrypter encrypter = new Encrypter();

            ArrayList<String> recipients;
            try {
                recipients = resultData.getStringArrayListExtra("recipients");
                SharedPrefsManager sharedPrefsManager = SharedPrefsManager.getInstance(context);

                String name = sharedPrefsManager.getNamespace() + "/data";
                String filename = sharedPrefsManager.getNamespace() + "/file" + path;

                // Generate symmetric key
                final SecretKey secretKey;

                final byte[] iv = encrypter.generateIV();

                // Encode sync data
                Metadata metadata = new Metadata();
                metadata.addLocation(location);
                metadata.setIsFile(isFile);

                final boolean feed = (recipients == null);
                if (feed) {
                    secretKey = sharedPrefsManager.getKey();
                    Timber.d("For feed");
                    metadata.setFeed(true);
                } else {
                    secretKey = encrypter.generateKey();

                    metadata.setFeed(false);
                    Timber.d("For friends");
                    BloomFilter bloomFilter = new BloomFilter(recipients.size());
                    for (String friend : recipients)
                        bloomFilter.insert(friend);

                    Name bloomName = new Name(bloomFilter.appendToName(new Name()));
                    Timber.d("Size: " + bloomName.get(0).toNumber() + " Prob: " + bloomName.get(1).toNumber());
                    metadata.setBloomFilter(bloomName);

                }
                String keyDigest = Common.getKeyDigest(secretKey);
                PublishedContent publishedContent = databaseViewModel.checkIfShared(keyDigest);
                filename = filename + "/" + keyDigest;
                Timber.d("Filename: " + filename);
                metadata.setFilename(filename);
                Timber.d("Publishing file: %s", filename);

                byte[] bytes;
                try {
                    InputStream is = context.getContentResolver().openInputStream(uri);
                    bytes = IOUtils.toByteArray(is);
                } catch (IOException e) {
                    Timber.d("onItemClick: failed to byte");
                    e.printStackTrace();
                    bytes = new byte[0];
                }
                Timber.d("file selection result: %s", "file path: " + filename);
                try {
                    String prefixApp = sharedPrefsManager.getNamespace();

                    Timber.d(filename);
                        Timber.d("Publishing to friend(s)");
                        if (publishedContent == null) {
                            databaseViewModel.addKey(keyDigest, secretKey);
                        }

                    Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
                    Timber.d(metadata.stringify());
                    Timber.d("m_content size: " + encryptedBlob.size());
                    Common.publishData(encryptedBlob, new Name(filename));
                    final FileManager manager = new FileManager(context.getApplicationContext());
                    Bitmap bitmap = QRExchange.makeQRCode(filename);
                    manager.saveFileQR(bitmap, filename);
                    ((AppCompatActivity) context).runOnUiThread(makeToast("Sending photo", context));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
                producerManager.publishFile(name, metadata.stringify());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ((AppCompatActivity) context).runOnUiThread(makeToast("Something went wrong with sending photo. Try resending", context));
        }
    }

    public static final String convert(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 1000.0d);

        StringBuilder sb = new StringBuilder();

        sb.setLength(0);
        sb.append(degree);
        sb.append("/1,");
        sb.append(minute);
        sb.append("/1,");
        sb.append(second);
        sb.append("/1000");
        return sb.toString();
    }

    /**
     * Parses interest to get the username
     *
     * @param interest
     */
    public static String interestToUsername(Interest interest) throws Exception {
        for (int i = 0; i<interest.getName().size(); i++) {
            if (interest.getName().get(i).toEscapedString().equals("npChat")) {
                return interest.getName().get(i+1).toEscapedString();
            }
        }
        throw new Exception();
    }

    /**
     * Get symmetric key digest
     *
     * @param secretKey
     * @returns String of the key's digest
     */
    public static String getKeyDigest(SecretKey secretKey) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(secretKey.getEncoded());
        byte[] digest = md.digest();
        String digestString = "";
        for (int b:digest)
            digestString += b;
        return digestString;
    }

    /**
     * Unfriend a user
     *
     * @param friend
     */
    public static void unfriend(String friend, SharedPrefsManager sharedPrefsManager) {
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        User user = realmRepository.deleteFriendship(friend);
        Globals.consumerManager.removeConsumer(user.getNamespace());
        Globals.producerManager.updateFriendsList();
        sharedPrefsManager.generateNewKey();
    }

    /**
     * Android is very particular about UI processes running on a separate thread. This function
     * creates and returns a Runnable thread object that will display a Toast message.
     */
    public static Runnable makeToast(final String s, final Context context) {
        return new Runnable() {
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }
        };
    }

    public static Name setComponent(Name name, int i, String component) throws ArrayIndexOutOfBoundsException {
        if (i > name.size() || i < 0) throw new ArrayIndexOutOfBoundsException("Out of bounds index");
        Name modifiedName = new Name();
        modifiedName.append(name.getPrefix(i));
        modifiedName.append(component);
        modifiedName.append(name.getSubName(i+1));
        return modifiedName;
    }

    public static int discoverComponent(Name name, String component) {
        for (int i = 0; i<name.size(); i++)
            if (name.get(i).toEscapedString().equals(component)) return i;
        return -1;
    }

}
