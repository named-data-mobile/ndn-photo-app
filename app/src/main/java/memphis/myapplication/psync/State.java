package memphis.myapplication.psync;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.Tlv0_2WireFormat;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

public class State {
	ArrayList<Name> m_content = new ArrayList<Name>();
	public static final int PSyncContent = 128;
	Tlv0_2WireFormat wireFormat = Tlv0_2WireFormat.get();

	public void
	addContent(Name prefix) {
		m_content.add(prefix);
	}

	public ArrayList<Name>
	getContent() {
		return m_content;
	}

	public Blob
	wireEncode() {
		TlvEncoder encoder = new TlvEncoder();
		int saveLength = encoder.getLength();
		for (Name prefix : m_content) {
			encoder.writeBuffer(prefix.wireEncode(Tlv0_2WireFormat.get()).buf());
		}
		encoder.writeTypeAndLength(PSyncContent, encoder.getLength() - saveLength);
	    return new Blob(encoder.getOutput(), false);
	}

	public void
	wireDecode(ByteBuffer input) throws EncodingException {
		 TlvDecoder decoder = new TlvDecoder(input);
		 int endOffset = decoder.readNestedTlvsStart(PSyncContent);

		 while (decoder.getOffset() < endOffset) {
			 Name name = new Name();
		     int endOffsetName = decoder.readNestedTlvsStart(Tlv.Name);
		     while (decoder.getOffset() < endOffsetName) {
		       name.append(new Blob(decoder.readBlobTlv(Tlv.NameComponent), true));
		     }
		     decoder.finishNestedTlvs(endOffsetName);
		     m_content.add(name);
		 }

		 decoder.finishNestedTlvs(endOffset);
	}
	
	public String
	toString() {
		String os = "[";
		for (Name prefix : m_content) {
			os += prefix + ", ";
		}
		os += "]";
		return os;
	}
}
