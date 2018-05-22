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
import java.util.function.Function;

/**
 * FaceProxy is a class that caches data to be served by the face upon future requests. If the data
 * is no longer present in the cache, the FaceProxy will call the onInterest callback to initialize
 * publishing of the requested data. This is required because currently the jndn library does not
 * provide a data caching option and if data is pushed to the face before any requests are received,
 * the data is removed. This hinders the app's flexibility.
 */
public class FaceProxy {

    private Data[] mCache;
    private int mCurrentIndex;

    public FaceProxy() {
        mCache = new Data[500];
        mCurrentIndex = 0;
    }

    public void process(Interest interest, MainActivity mainActivity) {
        Log.d("process", "Called process in FaceProxy");
        int index = findDataSegmentIndex(interest);
        // we don't have the data stored in the cache. Let's retrieve the requested segment and go
        // ahead and push future segments to the cache for faster retrieval
        if(index == -404) {
            // Nick said we should have a FaceProxy.process() function where we pass a fallback
            // function (publishing) if we do not see what we want to see (answer to below comments)
            // if we call publishData with the right content, it will be put in the face and the faceProxy
            Log.d("process", "We hit that -404.");
            Log.d("process", "-404 with interest: " + interest.getName().toString());
            String interestName = interest.getName().toUri();
            String temp = FileManager.removeAppPrefix(interestName);
            /*int fileIndex = 0;
            String temp = interestName.substring(fileIndex);
            Log.d("Faceproxy Process", "temp string before manipulations: " + temp);
            // name is of the form /ndn-snapchat/username/full-file-path; find third instance of "/"
            for(int i = 0; i < 3; i++) {
                fileIndex = temp.indexOf("/");
                temp = temp.substring(fileIndex + 1);
                Log.d("Faceproxy Process", "temp string after " + i + "th change: " + temp);
            }
            // String filePath = interestName.substring(fileIndex);
            Log.d("FaceProxy process", "filepath: " + temp);*/
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
            FileManager manager = new FileManager(mainActivity.getApplicationContext());
            String s = "/ndn-snapchat/test-user/" + temp;
            // String s = "/ndn-snapchat/" + manager.getUsername() + filePath;
            Name prefix = new Name(s);
            mainActivity.publishData(blob, prefix);
            // try again; temporary; this could form a loop if we don't actually have this content
            // return process(interest, mainActivity);
        }
        // otherwise, we know the name prefix has matched and we know where it is in the cache
        else {
            try {
                Log.d("process", "face.putData with index: " + index);
                Log.d("process", "data name: " + mCache[index].getName().toUri());
                Log.d("process", "data in face: " + mCache[index].getFullName().toUri());
                // Log.d("process", "data's metaInfo: " + mCache[index].getMetaInfo().getFinalBlockId().toEscapedString());
                // mainActivity.face2.putData(mCache[index]);
                mainActivity.face.putData(mCache[index]);
                /*mainActivity.face2.processEvents();
                try {
                    Thread.sleep(500);
                }
                catch(java.lang.InterruptedException e) {
                    Log.d("PublisherAndFetcherTest", "Refused to sleep thread.");
                }*/
            }
            catch(IOException | EncodingException e) {
                Log.d("process", "Data not put in face.");
                e.printStackTrace();
            }
        }

    }

    // consider changing the design of the cache to hold all segments for a file in one index; that way
    // we know we have all of the contents and we do not have to keep checking data name and segments. It's
    // an alternative, not necessarily required but you should consider it. Also, to extend that
    // alternative, we could use a map data structure. This would reduce my checks since I'm currently
    // checking every data packet in the
    private int findDataSegmentIndex(Interest requested_data) {
        // another note: if we do not find the first segment, "it's not there" even if the rest of
        // the segments are in the cache. Nick said treat it like memory deleting (i.e. removing the pointer)
        // it's technically physically there, but we do not have access. Obviously, it is still accessible
        // for my cache but as he said, treat it like memory in that case. All or nothing.
        Name requestedName = requested_data.getName();
        Log.d("findDataSegmentIndex", "Looking for data with Interest name: " + requestedName.toUri());
        for(int i = 0; i < mCache.length; i++) {
            mCurrentIndex += 1;
            if(mCurrentIndex > mCache.length - 1) {
                mCurrentIndex = mCurrentIndex%mCache.length;
            }

            if (mCache[mCurrentIndex] != null) {
                // Name cachedDataName = mCache[mCurrentIndex].getName().getPrefix(2);
                Name cachedDataName = mCache[mCurrentIndex].getName();
                Log.d("findDataSegmentIndex", "Interest name: " + requestedName.toUri() + " , cache data name: " + cachedDataName.toUri());
                /* if name prefix is same and segment number is same, we found what we were looking for
                if(cachedDataName.get(0).equals(requestedName.get(0)) &&
                    cachedDataName.get(2).equals(requestedName.get(2))) {*/
                if (cachedDataName.equals(requestedName)) {
                    // increment one more time so if we require more sequential segment numbers, we will
                    // have the right index when we return for future segments in the best case scenario.
                    return mCurrentIndex++;
                }
            }
        }
        // If we don't find it, return a negative 404; this will trigger publishing of data
        return -404;
    }

    // need to put the newly published data segment in the cache
    public void putInCache(Data data) {
        // take data array and cycle through its content; put each segment into an index in cache
        // or it might just be a single segment sent each time
        if(mCurrentIndex > mCache.length - 1) {
            mCurrentIndex = mCurrentIndex%mCache.length;
        }
        mCache[mCurrentIndex++] = data;
        Log.d("putInCache", "Put " + data.getName().toUri() + " in the cache.");
        Log.d("putInCache", "Content: " + new String(data.getContent().getImmutableArray()));
        //Log.d("putInCache", "Fresh?: " + data.getMetaInfo().getFreshnessPeriod());
        Log.d("putInCache", "MetaInfo: " + data.getMetaInfo().getFinalBlockId().toEscapedString());
        Log.d("putInCache", "MetaInfo Value: " + data.getMetaInfo().getFinalBlockId().getValue().size());
    }

    public Data[] getCache() {
        return mCache;
    }
}

// find the file and publish the specific segment...maybe? Dr. Wang wants this
// make a deterministic mapping of your file directories (/snapchat/username/<fullpath>)
   // Nick said this is a security issue so maybe we can hash them (need a reverse hash to "decode")
