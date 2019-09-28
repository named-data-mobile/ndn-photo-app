package memphis.myapplication.utilities;

import android.content.Context;

import memphis.myapplication.R;
import memphis.myapplication.data.Common;
import memphis.myapplication.data.RealmObjects.PublishedContent;
import memphis.myapplication.data.RealmRepository;
import timber.log.Timber;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.tpm.TpmBackEnd;
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


    public MemoryCache(Face face, Context context) {
        mCache = new MemoryContentCache(face, 1000);
        mCache.setMinimumCacheLifetime(1000);
        this.face = face;
        m_currContext = context; }

    public MemoryContentCache getmCache() {
        return mCache;
    }

    /**
     * This checks if the corresponding Data to the incoming Interest is in the cache. If so, put
     * it in the face. If not, publish the file, which will result in its placement in the cache.
     * @param filename
     */
    public void process(String filename, String digest) {
        SharedPrefsManager sharedPrefsManager = SharedPrefsManager.getInstance(m_currContext);
        RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
        SecretKey secretKey = realmRepository.getPublishedContent(filename).getKey();
        if (secretKey == null) {
            try {
                secretKey = SharedPrefsManager.getInstance(m_currContext).getKey();
            } catch (TpmBackEnd.Error error) {
                error.printStackTrace();
            }
        }
        realmRepository.close();
        Name iName = new Name(filename);

        Timber.d("Called process in FaceProxy");
        File file;
        try {
            file = new File(FileManager.removeAppPrefix(filename));
        } catch (UnsupportedEncodingException e) {
            Timber.e("URI decode error.");
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
            Timber.e("failed to byte");
            e.printStackTrace();
            bytes = new byte[0];
        }
        String prefixApp = sharedPrefsManager.getNamespace();
        String prefix = prefixApp + "/file" + filename;

        Encrypter encrypter = new Encrypter();
        if (secretKey != null) {
            byte[] iv = encrypter.generateIV();
            try {
                Blob encryptedBlob = encrypter.encrypt(secretKey, iv, bytes);
                Common.publishData(encryptedBlob, new Name(prefix).append(digest));

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
            Common.publishData(unencryptedBlob, new Name(prefix).append(digest));
        }

    }

    // need to put the newly published data segment in the cache
    public void putInCache(ArrayList<Data> fileData) {
        for (Data data : fileData) {
            mCache.add(data);
        }
    }

    // Return the minimum lifetime before removing stale content from the cache.
    double time() {
        return mCache.getMinimumCacheLifetime();
    }

    /**
     * Process an interest for a file
     */
    public final OnInterestCallback onNoDataInterest = new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            Timber.d("Called OnInterestCallback with Interest: " + interest.getName().toUri());
            int start = 0;
            for (int i = 0; i<=interest.getName().size(); i++) {
                if ((interest.getName().getSubName(i, 1).toUri().equals("/npChat"))
                        && (interest.getName().getSubName(i+1, 1).toUri().equals("/" + SharedPrefsManager.getInstance(m_currContext).getUsername()))
                        && (interest.getName().getSubName(i+2, 1).toUri().equals("/file"))) {
                    start = i + 3;
                }
            }
            Name filenameWithKeyDigest = interest.getName().getSubName(start);
            String filename = filenameWithKeyDigest.getPrefix(filenameWithKeyDigest.size()-1).toUri();
            String digest = filenameWithKeyDigest.getSubName(-1).toUri().substring(1);
            Timber.d("What is the filename? %s", filename);

            // If file has been previously published, republish it
            RealmRepository realmRepository = RealmRepository.getInstanceForNonUI();
            PublishedContent publishedContent = realmRepository.getPublishedContent(filename);
            realmRepository.close();

            if (publishedContent != null) {
                Timber.i(publishedContent.getFilename());
                Timber.i(publishedContent.getKey()+"");
                process(filename, digest);
            } else {
                Timber.d("Can't find file. Ignoring");
            }

        }
    };

    // Call this to “shut down” the MemoryContentCache while the application is still running.
    void destroy(){
        mCache.unregisterAll();
    }
}
