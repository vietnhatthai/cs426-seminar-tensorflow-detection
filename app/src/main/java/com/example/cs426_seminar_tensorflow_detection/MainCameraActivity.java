package com.example.cs426_seminar_tensorflow_detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.example.cs426_seminar_tensorflow_detection.customview.OverlayView;
import com.example.cs426_seminar_tensorflow_detection.deepmodel.MobileNetObjDetector;
import com.example.cs426_seminar_tensorflow_detection.deepmodel.DetectionResult;
import com.example.cs426_seminar_tensorflow_detection.utils.ImageUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Calendar;

public class MainCameraActivity extends CameraActivity implements OnImageAvailableListener {
    private static MainCameraActivity instance;

    private static int MODEL_IMAGE_INPUT_SIZE = 300;
    private static String LOGGING_TAG = MainCameraActivity.class.getName();
    private static float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private MobileNetObjDetector objectDetector;
    private Bitmap imageBitmapForModel = null;
    private Bitmap rgbBitmapForCameraImage = null;
    private boolean computing = false;
    private Matrix imageTransformMatrix;

    private long fps = 0;

    private OverlayView overlayView;

    public static MainCameraActivity getInstance() {
        if (instance == null) {
            instance = new MainCameraActivity();
        }
        return instance;
    }

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        try {
            objectDetector = MobileNetObjDetector.create(getAssets());
            Log.i(LOGGING_TAG, "Model Initiated successfully.");
            Toast.makeText(getApplicationContext(), "MobileNetObjDetector created", Toast.LENGTH_SHORT).show();
        } catch(IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "MobileNetObjDetector could not be created", Toast.LENGTH_SHORT).show();
            finish();
        }

        overlayView = (OverlayView) findViewById(R.id.overlay);

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();
        //Sensor orientation: 90, Screen orientation: 0
        sensorOrientation = rotation + screenOrientation;
        Log.i(LOGGING_TAG, String.format("Camera rotation: %d, Screen orientation: %d, Sensor orientation: %d",
                rotation, screenOrientation, sensorOrientation));

        previewWidth = previewSize.getWidth();
        previewHeight = previewSize.getHeight();
        Log.i(LOGGING_TAG, "preview width: " + previewWidth);
        Log.i(LOGGING_TAG, "preview height: " + previewHeight);
        // create empty bitmap
        imageBitmapForModel = Bitmap.createBitmap(MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, Config.ARGB_8888);
        rgbBitmapForCameraImage = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        imageTransformMatrix = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, sensorOrientation,true);
        imageTransformMatrix.invert(new Matrix());
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image imageFromCamera = null;

        try {
            imageFromCamera = reader.acquireLatestImage();
            if (imageFromCamera == null) {
                return;
            }
            if (computing) {
                imageFromCamera.close();
                return;
            }
            computing = true;
            preprocessImageForModel(imageFromCamera);
            imageFromCamera.close();
        } catch (final Exception ex) {
            if (imageFromCamera != null) {
                imageFromCamera.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }

        runInBackground(() -> {
            Date currentTime = Calendar.getInstance().getTime();
            final List<DetectionResult> results = objectDetector.detectObjects(imageBitmapForModel);
            overlayView.setResults(results, fps);
            requestRender();
            Date endTime = Calendar.getInstance().getTime();
            // calc fps
            long diff = endTime.getTime() - currentTime.getTime();
            fps = 1000 / diff;
            Log.i(LOGGING_TAG, String.format("FPS: %d", fps));
            computing = false;
        });
    }

    private void preprocessImageForModel(final Image imageFromCamera) {
        rgbBitmapForCameraImage.setPixels(ImageUtils.convertYUVToARGB(imageFromCamera, previewWidth, previewHeight),
                0, previewWidth, 0, 0, previewWidth, previewHeight);

        new Canvas(imageBitmapForModel).drawBitmap(rgbBitmapForCameraImage, imageTransformMatrix, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}