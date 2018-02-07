package memphis.myapplication;

/*
 * Copyright(C) 2018 ndn-snapchat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2012-2018 ZXing authors, Journey Mobile
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

/*import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;*/
import android.content.Intent;
/*import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;*/
import android.os.Bundle;
//import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
//import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
/*import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceView;*/
import android.view.View;
import android.widget.Toast;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import static com.google.zxing.integration.android.IntentIntegrator.QR_CODE_TYPES;

//import static android.content.Context.CAMERA_SERVICE;

/**
 * This class gives the ndn-snapchat application the ability to scan QR codes presented to it
 */

public class QRScanner extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onClick(View view) {
        IntentIntegrator scanner = new IntentIntegrator(this);
        // only want QR code scanner
        scanner.setDesiredBarcodeFormats(QR_CODE_TYPES);
        scanner.setOrientationLocked(true);
        // back facing camera id
        scanner.setCameraId(0);

        scanner.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Nothing is here", Toast.LENGTH_LONG).show();
            } else {
                String name = result.getContents();
                Toast.makeText(this, name, Toast.LENGTH_LONG).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Backwards facing camera has an int value that equals 0, but opening a capture session
    // requires a String representation of the id.
   /* public static final String BACK_CAM = "0";
    private CameraManager m_camMan;
    private CameraDevice m_camDevice;
    private Surface m_surface;
    private String m_imgName;

    public QRScanner() {
        //every scan session will require camera usage; we need our CameraManager
        m_camMan = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        m_imgName = null;
    }*/

    /*/**
     * Sets up a Surface for the camera to use for image processing.
     */
    /*private Surface preparePreview() {
        // Things needed: dimensions of our Surface(Texture), I'm still confused about the
        // difference btw S-Texture and just Surface. Also SurfaceView???
        SurfaceTexture texture = new SurfaceTexture();
        //return Surface
    }

    protected void useCamera() {
        m_surface = preparePreview();
        m_camDevice.createCaptureSession(m_surface, capStateCallback, null);
        m_camDevice.close();
    }*/

    /*/**
     * Scan the presented QR code and return the scanned value. Scanning will be used for
     * public key registration or name acquisition, both of which will be translated from
     * its QR value to String format.
     */
    /*protected String scanQR(View view) {
        QRScanner scanner = new QRScanner();

        //Camera management stuff
        try {
            int perm = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if(perm == 0) {
                try {
                    m_camMan.openCamera(BACK_CAM, opStateCallback, null);
                }
                catch(NullPointerException np) {
                    Toast.makeText(this, "No camera available!", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, "No scans for you!", Toast.LENGTH_SHORT).show();
                return "";
            }
        }
        catch(CameraAccessException e) {
            Toast.makeText(this, "No no no", Toast.LENGTH_SHORT).show();
        }
        //Need to check that the back facing camera is the one we're using
        return "pass";
    }*/

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // All callbacks:

    /*/**
     * Callback for whenever a CameraDevice attempts to open a camera.
     */
    /*private CameraDevice.StateCallback opStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            m_camDevice = cameraDevice;
            useCamera();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e("CAMERA:", "Camera disconnected");
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e("CAMERA:", "Unexpected camera device error! Error value: " + i);
            cameraDevice.close();
        }
    };*/

    /*/**
     * Callback for when a CameraDevice starts a CaptureSession
     */
    /*private CameraDevice.StateCallback capStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e("CAMERA:", "Capture session unexpectedly interrupted!");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.e("CAMERA:", "Capture session failed! Error value: " + i);
        }
    }*/

}
