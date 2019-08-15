package net.named_data.jni.psync;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PSync {
    private static PSync s_psync;

    static {
        System.loadLibrary("psync-jni");
    }

    public interface OnSyncDataCallBack {
        void onSyncDataCallBack(ArrayList<MissingDataInfo> updates);
    }

    public interface OnHelloDataCallBack {
        void onHelloDataCallBack(ArrayList<String> names, Consumer consumer);
    }

    // Singleton pattern
    private PSync(String homePath) {
        initialize(homePath);
    }

    public static PSync getInstance(String homeFolder) {
        if(s_psync == null) {
            s_psync = new PSync(homeFolder);
        }
        return s_psync;
    }

    public void cleanAndStop() {
        s_psync.stop();
        s_psync = null;
    }

    private native void initialize(String homePath);

    private native void stop();

    public static class FullProducer {
        private OnSyncDataCallBack m_onReceivedSyncData;
        private ByteBuffer m_buffer;

        public FullProducer(int ibfSize,
                     String syncPrefix,
                     String userPrefix,
                     OnSyncDataCallBack onSyncUpdate,
                     long syncInterestLifetimeMillis,
                     long syncReplyFreshnessMillis) {
            m_onReceivedSyncData = onSyncUpdate;
            m_buffer = startFullProducer(ibfSize, syncPrefix, userPrefix,
                    syncInterestLifetimeMillis, syncReplyFreshnessMillis);
        }

        public FullProducer(int ibfSize,
                     String syncPrefix,
                     String userPrefix,
                     OnSyncDataCallBack onSyncUpdate) {
            this(ibfSize , syncPrefix, userPrefix, onSyncUpdate, 1000, 1000);
        }

        private native ByteBuffer startFullProducer(int ibfSize,
                                                    String syncPrefix,
                                                    String userPrefix,
                                                    long syncInterestLifetimeMillis,
                                                    long syncReplyFreshnessMillis);

        public void cleanAndStop() {
            stop(m_buffer);
        }

        public void addUserNode(String prefix) {
            addUserNode(m_buffer, prefix);
        }

        public void removeUserNode(String prefix) {
            removeUserNode(m_buffer, prefix);
        }

        public long getSeqNo(String prefix) {
            return getSeqNo(m_buffer, prefix);
        }

        public void publishName(String prefix) {
            publishName(m_buffer, prefix);
        }

        private native void stop(ByteBuffer buffer);

        private native boolean addUserNode(ByteBuffer buffer, String prefix);

        private native void removeUserNode(ByteBuffer buffer, String prefix);

        private native long getSeqNo(ByteBuffer buffer, String prefix);

        private native void publishName(ByteBuffer buffer, String prefix);

        // Called from C++
        public void onSyncUpdate(ArrayList<MissingDataInfo> updates) {
            m_onReceivedSyncData.onSyncDataCallBack(updates);
        }
    }

    public static class PartialProducer {
        private ByteBuffer m_buffer;

        public PartialProducer(int ibfSize,
                            String syncPrefix,
                            String userPrefix,
                            long helloReplyFreshnessMillis,
                            long syncReplyFreshnessMillis) {
           m_buffer = startPartialProducer(ibfSize, syncPrefix, userPrefix,
                                           helloReplyFreshnessMillis, syncReplyFreshnessMillis);
        }

        private native ByteBuffer startPartialProducer(int ibfSize,
                                                       String syncPrefix,
                                                       String userPrefix,
                                                       long helloReplyFreshness,
                                                       long syncReplyFreshness);

        public void addUserNode(String prefix) {
            addUserNode(m_buffer, prefix);
        }

        public void removeUserNode(String prefix) {
            removeUserNode(m_buffer, prefix);
        }

        public long getSeqNo(String prefix) {
            return getSeqNo(m_buffer, prefix);
        }

        public void stop() {
            stop(m_buffer);
        }

        public void publishName(String prefix) {
            publishName(m_buffer, prefix);
        }

        public void publishName(String prefix, long seq) { publishNameSeq(m_buffer, prefix, seq); }

        private native void stop(ByteBuffer buffer);

        private native boolean addUserNode(ByteBuffer buffer, String prefix);

        private native void removeUserNode(ByteBuffer buffer, String prefix);

        private native long getSeqNo(ByteBuffer buffer, String prefix);

        private native void publishName(ByteBuffer buffer, String prefix);

        private native void publishNameSeq(ByteBuffer buffer, String prefix, long seq);
    }

    public static class Consumer {
        private OnHelloDataCallBack m_onHelloDataCallback;
        private OnSyncDataCallBack m_onSyncDataCallBack;
        private ByteBuffer m_buffer;

        public Consumer(String syncPrefix,
                        OnHelloDataCallBack helloDataCallBack,
                        OnSyncDataCallBack syncDataCallBack,
                        int count,
                        double falsePositive,
                        long helloInterestLifetimeMillis,
                        long syncInterestLifetimeMillis)
        {
            m_onHelloDataCallback = helloDataCallBack;
            m_onSyncDataCallBack = syncDataCallBack;
            m_buffer = initializeConsumer(syncPrefix, count, falsePositive,
                                          helloInterestLifetimeMillis, syncInterestLifetimeMillis);
        }

        public Consumer(String syncPrefix,
                        OnHelloDataCallBack helloDataCallBack,
                        OnSyncDataCallBack syncDataCallBack,
                        int count,
                        double falsePositive)
        {
            this(syncPrefix, helloDataCallBack, syncDataCallBack, count, falsePositive, 1000, 1000);
        }

        public void sendHelloInterest() {
            sendHelloInterest(m_buffer);
        }

        public void sendSyncInterest() {
            sendSyncInterest(m_buffer);
        }

        public boolean addSubscription(String prefix) { return addSubscription(m_buffer, prefix); }

        public ArrayList<String> getSubscriptionList() {
            return getSubscriptionList(m_buffer);
        }

        public boolean isSubscribed(String prefix) {
            return isSubscribed(m_buffer, prefix);
        }

        public long getSeqNo(String prefix) {
            return getSeqNo(m_buffer, prefix);
        }

        public void stop() {
            stop(m_buffer);
        }

        private native ByteBuffer initializeConsumer(String syncPrefix, int count, double falsePositive, long helloInterestLifetimeMillis, long syncInterestLifetimeMillis);

        private native void sendHelloInterest(ByteBuffer buffer);

        private native void sendSyncInterest(ByteBuffer buffer);

        private native boolean addSubscription(ByteBuffer buffer, String prefix);

        private native ArrayList<String> getSubscriptionList(ByteBuffer buffer);

        private native boolean isSubscribed(ByteBuffer buffer, String prefix);

        private native long getSeqNo(ByteBuffer buffer, String prefix);

        private native void stop(ByteBuffer buffer);

        private void onSyncData(ArrayList<MissingDataInfo> updates) {
            m_onSyncDataCallBack.onSyncDataCallBack(updates);
        }

        public void onHelloData(ArrayList<String> names) {
            m_onHelloDataCallback.onHelloDataCallBack(names, this);
        }
    }
}
