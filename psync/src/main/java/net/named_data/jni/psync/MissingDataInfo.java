package net.named_data.jni.psync;

public class MissingDataInfo {
    public MissingDataInfo(String p, long low, long high) {
        this.prefix = p;
        this.lowSeq = low;
        this.highSeq = high;
    }
    public String prefix;
    public long lowSeq;
    public long highSeq;
}
