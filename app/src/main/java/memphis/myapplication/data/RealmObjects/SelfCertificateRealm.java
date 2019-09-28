package memphis.myapplication.data.RealmObjects;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.v2.CertificateV2;

import java.nio.ByteBuffer;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * This Realm class is for storing the user's own certificates signed by their friends.
 */


public class SelfCertificateRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String username;

    @Required
    private byte[] cert;


    public String getUsername(){
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

    public byte[] getCertInByte()  {
        return cert;
    }
        public void setCert(byte[] cert) {
        this.cert = cert;
    }
}
