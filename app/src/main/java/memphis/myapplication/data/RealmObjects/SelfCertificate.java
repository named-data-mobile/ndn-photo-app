package memphis.myapplication.data.RealmObjects;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;

import java.nio.ByteBuffer;

/**
 * This class is for storing the user's own certificates signed by their friends.
 */


public class SelfCertificate {

    private String username;
    private byte[] cert;

    public String getUsername() {
        return username;
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

    public String getSigner() throws EncodingException {
        CertificateV2 certObj = new CertificateV2();
        certObj.wireDecode(ByteBuffer.wrap(cert));
        return certObj.getName().getSubName(-1, 1).toString().substring(1);
    }

    public String getOwner() throws EncodingException {
        CertificateV2 certObj = new CertificateV2();
        certObj.wireDecode(ByteBuffer.wrap(cert));
        return certObj.getName().getSubName(-4, 1).toString().substring(1);
    }

    public void setCert(byte[] cert) {
        this.cert = cert;
    }
}
