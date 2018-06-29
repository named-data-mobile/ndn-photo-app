package memphis.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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
    public static Bitmap makeQRFriendCode(Context context) {
        FileManager manager = new FileManager(context);
        String name = manager.getUsername();
        // this will likely change in the future
        net.named_data.jndn.security.certificate.PublicKey publicKey = manager.getPubKey();
        if(publicKey != null) {
            // String pubKey = publicKey.toString();
            String pubKey = new String(publicKey.getKeyDer().getImmutableArray());
            // make sure we check later during registration that a username has no spaces
            String qrContents = name + " " + pubKey;
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
