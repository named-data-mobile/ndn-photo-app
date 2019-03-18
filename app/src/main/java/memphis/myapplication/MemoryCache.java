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
import java.util.ArrayList;

public class MemoryCache {

    private MemoryContentCache mCache;
    private Face face;
    private Context m_currContext;

    private final int INTENT_FILTER = 1;


    MemoryCache(Face face, Context context) {
        mCache = new MemoryContentCache(face, 8000);
        this.face = face;
        m_currContext = context;
    }

    MemoryContentCache getmCache() {
        return mCache;
    }

    void process(Interest interest) {

        Log.d("process", "Called process in FaceProxy");

        mCache.setInterestFilter(interest.getName(), new OnInterestCallback() {
            @Override
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

                if (interestFilterId == INTENT_FILTER) {
                    // we don't have the data stored in the cache. Let's retrieve the requested segment and go
                    // ahead and push future segments to the cache for faster retrieval
                    mCache.storePendingInterest(interest, face);
                    Log.d("MemoryCache process", "Interest data not found.");
                    Name iName = interest.getName();
                    if(iName.get(-1).isSegment() && iName.get(-2).isVersion()) {
                        iName = iName.getPrefix(-2);
                        Log.d("process", "interestName after removal of version && segNum: " + iName.toUri());
                        // malformed interest with one or more version and segment numbers (i.e., /prefix/version/segNum/version/segNum/
                        if(iName.get(-1).isSegment() && iName.get(-2).isVersion()) {
                            iName = new Name("/");
                        }
                    }
                    // this is the base interest (has no version or segment number) and we do not have it in our
                    // cache at the moment; first check if the file still exists, then operate accordingly
                    else {
                        File file;
                        try {
                            file = new File(FileManager.removeAppPrefix(iName.toUri()));
                        } catch (UnsupportedEncodingException e) {
                            Log.e("MemoryCache Process" , "URI decode error.");
                            e.printStackTrace();
                            return;
                        }
                        if(!file.exists()) {
                            // malformed interest; let's change it to "/" and check for that so we do not attempt to publish
                            iName = new Name("/");
                        }
                    }
                    if(!iName.toUri().equals("/")) {
                        String temp;
                        try {
                            temp = FileManager.removeAppPrefix(iName.toUri());
                        } catch (UnsupportedEncodingException e) {
                            Log.e("MemoryCache Process" , "URI decode error.");
                            e.printStackTrace();
                            return;
                        }

                        byte[] bytes;
                        try {
                            InputStream is = FileUtils.openInputStream(new File(temp));
                            bytes = IOUtils.toByteArray(is);
                        } catch (IOException e) {
                            Log.e("MemoryCache process", "failed to byte");
                            e.printStackTrace();
                            bytes = new byte[0];
                        }
                        Blob blob = new Blob(bytes, true);
                        FileManager manager = new FileManager(m_currContext);
                        String s = manager.addAppPrefix(temp);
                        Common.publishData(blob, new Name(s));

                        String filename = prefix.toUri();
                        Bitmap bitmap = QRExchange.makeQRCode(filename);
                        manager.saveFileQR(bitmap, filename);
                    }
                }
            }
        });

        mCache.onInterest(interest.getName(), interest, face, INTENT_FILTER, new InterestFilter(interest.getName()));
    }

    void putInCache(ArrayList<Data> fileData) {
        for (Data data : fileData) {
            mCache.add(data);
        }
    }

    double time() {
        return mCache.getMinimumCacheLifetime();
    }

    void destroy(){
        mCache.unregisterAll();
    }
}
