package memphis.myapplication.data.RealmObjects;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;

import java.nio.ByteBuffer;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class UserRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String username;

    @Required
    private String domain;

    private RealmList<String> friends = new RealmList<>();

    private byte[] cert;
    private boolean friend;
    private boolean trust;

    public void setCert(CertificateV2 c) {
        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
        tlvEncodedDataContent.writeBuffer(c.wireEncode().buf());
        cert = tlvEncodedDataContent.getOutput().array();
    }

    public void setFriend(boolean b) {
        friend = b;
    }

    public void setTrust(boolean b) {
        trust = b;
    }

    public void setDomain(String d) { domain = d; }

    public String getDomain() {
        return domain;
    }

    public String getUsername() {
        return username;
    }

    public CertificateV2 getCert() throws EncodingException {
        CertificateV2 certObj = new CertificateV2();
        certObj.wireDecode(ByteBuffer.wrap(cert));
        return certObj;
    }

    public byte[] getCertByreArray() {
        return cert;
    }

    public boolean isFriend() {
        return friend;
    }

    public boolean haveTrust() {
        return trust;
    }

    public void addFriend(String f) {
        if (friends.contains(f)) {
            return;
        }
        friends.add(f);
    }

    public void setCert(byte[] cert) {
        this.cert = cert;
    }
}
