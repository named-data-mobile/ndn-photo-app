package memphis.myapplication.data.tasks;

import net.named_data.jndn.Interest;

import javax.crypto.SecretKey;

/**
 * Store parameters to pass to FetchingTask
 */
public class FetchingTaskParams
{
    Interest interest;
    SecretKey secretKey;
    boolean feed;

    public FetchingTaskParams(Interest interest, SecretKey secretKey, boolean feed) {
        this.interest = interest;
        this.secretKey = secretKey;
        this.feed = feed;
    }
}
