package memphis.myapplication.data.tasks;

import net.named_data.jndn.Interest;

import javax.crypto.SecretKey;

public class FetchingTaskParams
{
    Interest interest;
    SecretKey secretKey;

    public FetchingTaskParams(Interest interest, SecretKey secretKey) {
        this.interest = interest;
        this.secretKey = secretKey;
    }
}
