package com.ajinkyabadve.uartcommunication;

import android.app.Activity;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements UARTHelper.KeyReceivedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    UARTHelper uartHelper = new UARTHelper();
    /**
     * A Handler for running tasks in the background.
     */
    private Handler mCameraHandler;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    /**
     * Camera capture device wrapper
     */
    private DoorbellCamera mCamera;
    // Callback to receive captured camera image data
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Get the raw image bytes
                    Image image = reader.acquireLatestImage();
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uartHelper.init(this);
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

    }

    @Override
    public void onStringReceived(String data) {
        mCamera.takePicture();
    }

    @Override
    protected void onDestroy() {
        uartHelper.close();
        mCameraThread.quitSafely();
        mCamera.shutDown();
        super.onDestroy();
    }

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            // ...process the captured image...
            Log.d(TAG, "onPictureTaken() called with: imageBytes = [" + imageBytes + "]");
//            File photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
//
//            if (photo.exists()) {
//                photo.delete();
//            }
//
//            try {
//                FileOutputStream fos = new FileOutputStream(photo.getAbsolutePath());
//
//                fos.write(imageBytes);
//                fos.close();
//            } catch (java.io.IOException e) {
//                Log.e("PictureDemo", "Exception in photoCallback", e);
//            }


            File file = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
            Log.d(TAG, "paths: file AbsolutePath" + file.getAbsolutePath() + " path" + file.getPath());
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(imageBytes);
                os.close();
                // TODO: 23/1/17 add later
            } catch (IOException e) {
                Log.w(TAG, "Cannot write to " + file, e);

            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }

            }


        }
    }

}
