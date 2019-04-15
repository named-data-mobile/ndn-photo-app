package memphis.myapplication.tasks;

import net.named_data.jndn.Interest;

import javax.crypto.SecretKey;

public class FetchingTaskParams
{
    Interest interest;
    SecretKey secretKey;
    byte[] iv;

    public FetchingTaskParams(Interest interest, SecretKey secretKey, byte[] iv) {
        this.interest = interest;
        this.secretKey = secretKey;
        this.iv  = iv;
    }
}
