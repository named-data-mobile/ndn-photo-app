package memphis.myapplication;

import android.util.Log;

import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.tpm.TpmBackEnd;
import net.named_data.jndn.util.Blob;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Common {

    /* This currently contains duplicate functions from FilesActivity except that FilesActivity
     * contains saveFileQR method in its publishData method. I needed access to publishData but from
     * MainActivity (and other places), if I tried creating a FilesActivity and calling publishData,
     * it would crash because of the FileManager being null. Fix this duplication later.
     */

    /**
     * Starts a new thread to publish the file/photo data.
     * @param blob Blob of content
     * @param prefix Name of the file (currently absolute path)
     */
    public static void publishData(final Blob blob, final Name prefix) {
        Thread publishingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ArrayList<Data> fileData = new ArrayList<>();
                    ArrayList<Data> packets = packetize(blob, prefix);
                    // it would be null if this file is already in our cache so we do not packetize
                    if(packets != null) {
                        Log.d("publishData", "Publishing with prefix: " + prefix);
                        for (Data data : packetize(blob, prefix)) {
                            Globals.keyChain.sign(data);
                            fileData.add(data);
                        }
                        Globals.memoryCache.putInCache(fileData);
                    }
                    else {
                        Log.d("publishData", "No need to publish; " + prefix.toUri() + " already in cache.");
                    }
                } catch (PibImpl.Error | SecurityException | TpmBackEnd.Error |
                        KeyChain.Error e)

                {
                    e.printStackTrace();
                }
            }
        });
        publishingThread.start();
    }

    /**
     * This takes a Blob and divides it into NDN data packets
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
            Log.d("packetize things", "PACKET_SIZE: " + PACKET_SIZE);
            byte[] segment_buffer = new byte[PACKET_SIZE];
            Data data = new Data();
            Name segment_name = new Name(prefix);
            segment_name.appendVersion(VERSION_NUMBER);
            segment_name.appendSegment(segment_number);
            data.setName(segment_name);
            try {
                Log.d("packetize things", "full data name: " + data.getFullName().toString());
            } catch (EncodingException e) {
                Log.d("packetize things", "unable to print full name");
            }
            try {
                Log.d("packetize things", "byteBuffer position: " + byteBuffer.position());
                byteBuffer.get(segment_buffer, 0, PACKET_SIZE);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            data.setContent(new Blob(segment_buffer));
            MetaInfo meta_info = new MetaInfo();
            meta_info.setType(ContentType.BLOB);
            // not sure what is a good freshness period
            meta_info.setFreshnessPeriod(90000);
            if (!byteBuffer.hasRemaining()) {
                // Set the final component to have a final block id.
                Name.Component finalBlockId = Name.Component.fromSegment(segment_number);
                meta_info.setFinalBlockId(finalBlockId);
            }
            data.setMetaInfo(meta_info);
            datas.add(data);
            segment_number++;
        } while (byteBuffer.hasRemaining());
        return datas;
    }
}
