package memphis.myapplication.data.RealmObjects;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class User {

    private String username;
    private String domain;
    private ArrayList<String> friends = new ArrayList<>();

    private byte[] cert;
    private boolean friend;
    private boolean trust;
    private byte[] symKey;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setSymKey(byte[] key) { this.symKey = key; }

    public ArrayList<String> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<String> friends) {
        this.friends = friends;
    }

    public void setCert(CertificateV2 c) {
        TlvEncoder tlvEncodedDataContent = new TlvEncoder();
        tlvEncodedDataContent.writeBuffer(c.wireEncode().buf());
        cert = tlvEncodedDataContent.getOutput().array();
    }

    public CertificateV2 getCert() throws EncodingException {
        CertificateV2 certObj = new CertificateV2();
        certObj.wireDecode(ByteBuffer.wrap(cert));
        return certObj;
    }

    public byte[] getSymKey() { return symKey; }

    public void setCert(byte[] cert) {
        this.cert = cert;
    }

    public boolean isFriend() {
        return friend;
    }

    public void setFriend(boolean friend) {
        this.friend = friend;
    }

    public boolean haveTrust() {
        return trust;
    }

    public void setTrust(boolean trust) {
        this.trust = trust;
    }

    public String getNamespace() { return domain + "/npChat/" + username; }

}
