package memphis.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.MemoryContentCache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import io.realm.Realm;
import memphis.myapplication.RealmObjects.PublishedContent;

/**
 * MemoryCache is a class that works with MemoryContentCache class from jndn library that caches
 * data to be served by the face upon future requests. If the data is no longer present in the
 * cache, MemoryContentCache will call the onInterest callback to initialize publishing of the
 * requested data if present. See the API docs for more detail at
 * http://named-data.net/doc/ndn-ccl-api/memory-content-cache.html .
 */

public class MemoryCache {

    private MemoryContentCache mCache;
    private Face face;
    private static Context m_currContext;

    private final int INTENT_FILTER = 1;


    MemoryCache(Face face, Context context) {
        mCache = new MemoryContentCache(face, 1000);
        mCache.setMinimumCacheLifetime(1000);
        this.face = face;
        m_currContext = context; }

    MemoryContentCache getmCache() {
        return mCache;
    }

    /**
     * This checks if the corresponding Data to the incoming Interest is in the cache. If so, put
     * it in the face. If not, publish the file, which will result in its placement in the cache.
     * @param filename
     */
    public void process(String filename) {
        SharedPrefsManager sharedPrefsManager = SharedPrefsManager.getInstance(m_currContext);
        Realm realm = Realm.getDefaultInstance();
        SecretKey secretKey = realm.where(PublishedContent.class).equalTo("filename", filename).findFirst().getKey();
        Name iName = new Name(filename);

        Log.d("process", "Called process in MemoryCache");

        File file;
        try {
            file = new File(FileManager.removeAppPrefix(filename));
        } catch (UnsupportedEncodingException e) {
            Log.e("MemoryCache Process" , "URI decode error.");
            e.printStackTrace();
            return;
        }
        if(!file.exists()) {
            // malformed interest; let's change it to "/" and check for that so we do not attempt to publish
            iName = new Name("/");
        }

        byte[] bytes;
        try {
            InputStream is = FileUtils.openInputStream(new File(filename));
            bytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            Log.e("MemoryCache process", "failed to byte");
            e.printStackTrace();
            bytes = new byte[0];
        }
        FileManager manager = new FileManager(m_currContext);
        String prefixApp = "/" + sharedPrefsManager.getNamespace();
        String prefix = prefixApp + "/file" + filename;

        Encrypter encrypter = new Encrypter(m_currContext);
        if (secretKey != null) {
            byte[] iv = encrypter.generateIV();
            try {
                Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
                Common.publishData(encryptedBlob, new Name(prefix));

            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
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
        } else {
            Blob unencryptedBlob = new Blob(bytes);
            Common.publishData(unencryptedBlob, new Name(prefix));
        }



//        Bitmap bitmap = QRExchange.makeQRCode(filename);
//        Log.d("publishData", "filename: " + filename + " bitmap: " + (bitmap == null));
//        manager.saveFileQR(bitmap, filename);

    }

    // need to put the newly published data segment in the cache
    void putInCache(ArrayList<Data> fileData) {
        for (Data data : fileData) {
            mCache.add(data);
        }
    }

    // Return the minimum lifetime before removing stale content from the cache.
    double time() {
        return mCache.getMinimumCacheLifetime();
    }

    public final OnInterestCallback onNoDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            Log.d("onNoDataInterest", "Called OnInterestCallback with Interest: " + interest.getName().toUri());
            int start = 0;
            for (int i = 0; i<=interest.getName().size(); i++) {
                if ((interest.getName().getSubName(i, 1).toUri().equals("/npChat"))
                        && (interest.getName().getSubName(i+1, 1).toUri().equals("/" + SharedPrefsManager.getInstance(m_currContext).getUsername()))
                        && (interest.getName().getSubName(i+2, 1).toUri().equals("/file"))) {
                    start = i + 3;
                }
            }
            String filename = interest.getName().getSubName(start).toUri();
            System.out.println("What is the filename? " + filename);

            // If file has been previously published, republish it
            Realm realm = Realm.getDefaultInstance();
            if (realm.where(PublishedContent.class).equalTo("filename", filename).findFirst() != null) {
                process(filename);
            } else {
                Log.d("MemoryCache", "Can't find file. Ignoring");
            }
        }
    };

    // Call this to “shut down” the MemoryContentCache while the application is still running.
    void destroy(){
        mCache.unregisterAll();
    }
}
