package com.example.cs426_seminar_tensorflow_detection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.cs426_seminar_tensorflow_detection.customview.OverlayView;
import com.example.cs426_seminar_tensorflow_detection.deepmodel.DetectionResult;
import com.example.cs426_seminar_tensorflow_detection.deepmodel.MobileNetObjDetector;
import com.example.cs426_seminar_tensorflow_detection.utils.ImageUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RealTimeFragment extends Fragment implements OnImageAvailableListener {
    private final String LOGGING_TAG = MainActivity.class.getName();

    private int previewWidth = 0;
    private int previewHeight = 0;
    private Bitmap imageBitmapForModel;
    private Bitmap rgbBitmapForCameraImage;
    private boolean computing = false;
    private Matrix imageTransformMatrix;

    private OverlayView overlayView;

    private MobileNetObjDetector objectDetector;
    private Runnable runInBackground;

    private static final int PERMISSIONS_REQUEST = 1;

    private long fps = 0;


    public RealTimeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_real_time, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
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

        runInBackground = () -> {
            Date currentTime = Calendar.getInstance().getTime();
            List<DetectionResult> results;
            try {
                results = objectDetector.detectObjects(imageBitmapForModel);
                overlayView.setResults(results, fps);
                requestRender();
                Date endTime = Calendar.getInstance().getTime();
                // calc fps
                long diff = endTime.getTime() - currentTime.getTime();
                fps = 1000 / diff;
                Log.i(LOGGING_TAG, String.format("FPS: %d", fps));
            }
            catch (Exception e) {
                Log.e(LOGGING_TAG, e.getMessage());
            }
            computing = false;
        };

        process();
    }

    private void process() {
        runInBackground.run();
    }


    private void preprocessImageForModel(final Image imageFromCamera) {
        rgbBitmapForCameraImage.setPixels(ImageUtils.convertYUVToARGB(imageFromCamera, previewWidth, previewHeight),
                0, previewWidth, 0, 0, previewWidth, previewHeight);

        new Canvas(imageBitmapForModel).drawBitmap(rgbBitmapForCameraImage, imageTransformMatrix, null);
    }

    protected void setFragment() {
        CameraConnectionFragment cameraConnectionFragment = new CameraConnectionFragment();
        cameraConnectionFragment.addConnectionListener(this::onPreviewSizeChosen);
        cameraConnectionFragment.addImageAvailableListener(this);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, cameraConnectionFragment)
                .commit();
    }

    protected void onPreviewSizeChosen(final Size previewSize, final int rotation) {

        try {
            objectDetector = MobileNetObjDetector.create(requireActivity().getAssets());
            Log.i(LOGGING_TAG, "Model Initiated successfully.");
            Toast.makeText(requireActivity().getApplicationContext(), "MobileNetObjDetector created", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireActivity().getApplicationContext(), "MobileNetObjDetector could not be created", Toast.LENGTH_SHORT).show();
        }

        overlayView = requireActivity().findViewById(R.id.overlay);

        int screenOrientation = requireActivity().getWindowManager().getDefaultDisplay().getRotation();
        //Sensor orientation: 90, Screen orientation: 0
        int sensorOrientation = rotation + screenOrientation;
        Log.i(LOGGING_TAG, String.format("Camera rotation: %d, Screen orientation: %d, Sensor orientation: %d",
                rotation, screenOrientation, sensorOrientation));

        previewWidth = previewSize.getWidth();
        previewHeight = previewSize.getHeight();

        Log.i(LOGGING_TAG, "preview width: " + previewWidth);
        Log.i(LOGGING_TAG, "preview height: " + previewHeight);
        // create empty bitmap
        int MODEL_IMAGE_INPUT_SIZE = 300;
        imageBitmapForModel = Bitmap.createBitmap(MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        rgbBitmapForCameraImage = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        imageTransformMatrix = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                MODEL_IMAGE_INPUT_SIZE, MODEL_IMAGE_INPUT_SIZE, sensorOrientation, true);
        imageTransformMatrix.invert(new Matrix());
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        return requireActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(requireContext(),
                    "Camera AND storage permission are required for this app", Toast.LENGTH_LONG).show();
        }
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
    }

    public void requestRender() {
        OverlayView overlay = requireActivity().findViewById(R.id.overlay);
        overlay.postInvalidate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (objectDetector != null) {
            objectDetector.close();
        }
    }
}