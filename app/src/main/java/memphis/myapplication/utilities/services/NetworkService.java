package memphis.myapplication.utilities.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * This will be needed when we implement PSync. Until then, we will just use the Network Thread in
 * MainFragment.
 */
public class NetworkService extends Service {

    @Override
    public void onCreate() {
        // anything special?
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

    }
}
