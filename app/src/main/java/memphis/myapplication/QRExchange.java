package memphis.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import timber.log.Timber;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import net.named_data.jndn.Data;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.pib.Pib;
import net.named_data.jndn.security.pib.PibImpl;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import java.nio.ByteBuffer;

public class QRExchange {

    private static final int BIT_HEIGHT = 400;
    private static final int BIT_WIDTH = 400;

    public QRExchange() {

    }

    // generic QR Code call
    public static Bitmap makeQRCode(String qrContents){
        QRCodeWriter qrWriter = new QRCodeWriter();
        try {
            BitMatrix qrMatrix = qrWriter.encode(qrContents, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
            Bitmap bitmap = Bitmap.createBitmap(BIT_WIDTH, BIT_HEIGHT, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < BIT_HEIGHT; i++) {
                for (int j = 0; j < BIT_WIDTH; j++) {
                    bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        }
        catch(WriterException we) {
            Timber.d("qrWriter failed");
        }
        catch(Exception e) {
            Timber.d("bitmap was not created");
        }
        return null;
    }

    /**
     * come back to this. I save a string of the DER public key. Not sure if desired end result.
     */
    // consider changing this to send an interest for the key since it's in DER format. It does not
    // seem to play nice with strings.
    public static Bitmap makeQRFriendCode(Context context, FileManager manager) {
        CertificateV2 certificate = null;
        try {
            certificate = Globals.pibIdentity.getDefaultKey().getDefaultCertificate();
        } catch (Pib.Error error) {
            error.printStackTrace();
        } catch (PibImpl.Error error) {
            error.printStackTrace();
        }
        Timber.d("Certificate: " + certificate);
        Timber.d("Certificate: " + certificate);
        if(certificate != null) {
            TlvEncoder tlvEncodedDataContent = new TlvEncoder();
            tlvEncodedDataContent.writeBuffer(certificate.wireEncode().buf());
            byte[] finalDataContentByteArray = tlvEncodedDataContent.getOutput().array();
            String certString = Base64.encodeToString(finalDataContentByteArray, 0);
            // make sure we check later during registration that a username has no spaces
            Timber.d("Pubkey: " + certString);
            // replace below section with makeQRCode method
            QRCodeWriter qrWriter = new QRCodeWriter();
            try {
                BitMatrix qrMatrix = qrWriter.encode(certString, BarcodeFormat.QR_CODE, BIT_WIDTH, BIT_HEIGHT);
                Bitmap bitmap = Bitmap.createBitmap(BIT_WIDTH, BIT_HEIGHT, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < BIT_HEIGHT; i++) {
                    for (int j = 0; j < BIT_WIDTH; j++) {
                        bitmap.setPixel(i, j, qrMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                    }
                }
                return bitmap;
            } catch (WriterException we) {
                Timber.d("qrWriter failed");
            } catch (Exception ex) {
                Timber.d("bitmap was not created");
            }
        }
            // if it failed to make the bitmap
        return null;
    }
}