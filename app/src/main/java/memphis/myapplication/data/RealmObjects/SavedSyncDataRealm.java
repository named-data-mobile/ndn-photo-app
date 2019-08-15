package memphis.myapplication.data.RealmObjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class SavedSyncDataRealm extends RealmObject {
    @PrimaryKey
    private long seqNum;

    @Required
    String syncData;

    public void setSyncData(String s) { syncData = s; }

    public String getSyncData() { return syncData; }



}
