package memphis.myapplication;

import android.app.Activity;
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
import javax.crypto.spec.SecretKeySpec;

import memphis.myapplication.tasks.FetchingTask;
import memphis.myapplication.tasks.FetchingTaskParams;

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
        byte[] keyBytes = (SharedPrefsManager.getInstance(m_currContext).getSymKey(filename).getBytes());
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        Name iName = new Name(filename);

        Log.d("process", "Called process in FaceProxy");

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
        String s = manager.addAppPrefix(filename);
        Name prefixName = new Name(s);
        Encrypter encrypter = new Encrypter(m_currContext);
        byte[] iv = encrypter.generateIV();
        try {
            Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
            Common.publishData(encryptedBlob, new Name(prefixName));

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


        Bitmap bitmap = QRExchange.makeQRCode(filename);
        Log.d("publishData", "filename: " + filename + " bitmap: " + (bitmap == null));
        manager.saveFileQR(bitmap, filename);

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
            String filename = interest.getName().toUri();
            System.out.println("What is the interest? " + interest.toUri());
            if (SharedPrefsManager.getInstance(m_currContext).contains(interest.getName().toUri())) {
                // Need to get fetching params (filename, key) and execute fetching task
                process(filename);
                }
            }
    };

    // Call this to “shut down” the MemoryContentCache while the application is still running.
    void destroy(){
        mCache.unregisterAll();
    }
}
