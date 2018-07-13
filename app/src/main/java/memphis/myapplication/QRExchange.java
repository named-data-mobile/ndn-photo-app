package memphis.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import net.named_data.jndn.encoding.der.DerNode;
import net.named_data.jndn.util.Blob;

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

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
            Log.d("makeQrFriendCode", "qrWriter failed");
        }
        catch(Exception e) {
            Log.d("makeQrFriendCode", "bitmap was not created");
        }
        return null;
    }

    /**
     * come back to this. I save a string of the DER public key. Not sure if desired end result.
     */
    // consider changing this to send an interest for the key since it's in DER format. It does not
    // seem to play nice with strings.
    public static Bitmap makeQRFriendCode(FileManager manager) {
        String name = manager.getUsername();
        // problem is that this public key is not signed. We need the certificate; switch to v2
        // We should instead encode the CertificateV2 name and request it. This would simplify
        // verification; Dr. Wang would like to avoid this because it increases network traffic
        net.named_data.jndn.security.certificate.PublicKey publicKey = manager.getPubKey();
        Log.d("makeFriendCode", "Pubkey: " + publicKey.toString());
        if(publicKey != null) {
            String pubKey = Base64.encodeToString(publicKey.getKeyDer().getImmutableArray(), 0);
            // make sure we check later during registration that a username has no spaces
            Log.d("makeFriendCode", "Pubkey: " + pubKey.toString());
            String qrContents = name + " " + pubKey;
            // replace below section with makeQRCode method
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
            } catch (WriterException we) {
                Log.d("makeQrFriendCode", "qrWriter failed");
            } catch (Exception e) {
                Log.d("makeQrFriendCode", "bitmap was not created");
            }
        }
        // if it failed to make the bitmap
        return null;
    }
}
