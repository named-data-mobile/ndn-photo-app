package memphis.myapplication;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SegmentFetcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * FaceProxy is a class that caches data to be served by the face upon future requests. If the data
 * is no longer present in the cache, the FaceProxy will call the onInterest callback to initialize
 * publishing of the requested data. This is required because currently the jndn library does not
 * provide a data caching option and if data is pushed to the face before any requests are received,
 * the data is removed. This hinders the app's flexibility.
 */
public class FaceProxy {

    private ConcurrentHashMap<String, ArrayList<Data>> mCacheMap;

    public FaceProxy() {
        mCacheMap = new ConcurrentHashMap<>();
    }

    /**
     * This checks if the corresponding Data to the incoming Interest is in the cache. If so, put
     * it in the face. If not, publish the file, which will result in its placement in the cache.
     * @param interest
     */
    public void process(Interest interest) {
        // we should add a Nack or something for when we receive an interest of the wrong format.
        // for instance we sometimes try to publish something like /name/version/segNum/version/segNum
        Log.d("process", "Called process in FaceProxy");

        // we don't have the data stored in the cache. Let's retrieve the requested segment and go
        // ahead and push future segments to the cache for faster retrieval
        if(!hasKey(interest.getName())) {
            Log.d("process", "-404 with interest: " + interest.getName().toString());
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
                File file = new File(FileManager.removeAppPrefix(iName.toUri()));
                if(!file.exists()) {
                    // malformed interest; let's change it to "/" and check for that so we do not attempt to publish
                    iName = new Name("/");
                }
            }
            if(!iName.toUri().equals("/")) {
                String temp = FileManager.removeAppPrefix(iName.toUri());

                byte[] bytes;
                try {
                    InputStream is = FileUtils.openInputStream(new File(temp));
                    bytes = IOUtils.toByteArray(is);
                } catch (IOException e) {
                    Log.d("FaceProxy Process", "failed to byte");
                    e.printStackTrace();
                    bytes = new byte[0];
                }

                Blob blob = new Blob(bytes, true);
                FilesActivity filesActivity = new FilesActivity();
                FileManager manager = new FileManager(filesActivity.getApplicationContext());
                String s = manager.addAppPrefix(temp);
                Name prefix = new Name(s);
                filesActivity.publishData(blob, prefix);
            }
        }
        // otherwise, we know the name prefix has matched and we know where it is in the cache
        else {
            Name interestName = interest.getName();
            String baseInterest;
            // if the Interest Name does not have the version and segment number components, we have the
            // initial Interest
            if(!interestName.get(-2).isVersion()) {
                baseInterest = interestName.toUri();
                // add the 0 version and 0 segment so we can retrieve it below
                interestName.appendVersion(0);
                interestName.appendSegment(0);
            }
            else {
                // this is the key
                baseInterest = interestName.getPrefix(-2).toUri();
            }

            try {
                // get segment number from interest
                int segNum = (int)interestName.get(-1).toSegment();
                Data data = mCacheMap.get(baseInterest).get(segNum);
                Log.d("FaceProxy Process", "data name: " + data.getName().toUri());
                Log.d("FaceProxy Process", "data in face: " + data.getFullName().toUri());
                Globals.face.putData(data);
            }
            catch(IOException | EncodingException e) {
                Log.d("FaceProxy Process", "Data not put in face.");
                e.printStackTrace();
            }
        }
    }

    // need to put the newly published data segment in the cache
    public void putInCache(ArrayList<Data> fileData) {
        Data d = fileData.get(0);
        String key = d.getName().getPrefix(-2).toUri();
        // do this only if it is not in the cache
        if(!mCacheMap.containsKey(key)) {
            Log.d("putInCache", "key: " + key);
            mCacheMap.put(key, new ArrayList<Data>());
            ArrayList<Data> cacheSegments = mCacheMap.get(key);

            // add the segments in order; the index will match their segment number
            for (Data data : fileData) {
                cacheSegments.add(data);
                Log.d("putInCache", "Put " + data.getName().toUri() + " in the cache.");
            }
        }
        else {
            Log.d("putInCache", key + " is already in the cache");
        }
    }

    public boolean hasKey(Name name) {
        String key;
        if(!name.get(-2).isVersion()) {
            key = name.toUri();
            return mCacheMap.containsKey(key);
        }
        else {
            key = name.getPrefix(-2).toUri();
            return mCacheMap.containsKey(key);
        }
    }
}

// find the file and publish the specific segment...maybe? Dr. Wang wants this
// make a deterministic mapping of your file directories (/NP-Chat/username/<fullpath>)
