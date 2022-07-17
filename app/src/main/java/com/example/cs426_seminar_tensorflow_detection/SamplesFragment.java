package com.example.cs426_seminar_tensorflow_detection;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class SamplesFragment extends Fragment {

    private ImageView inputImageView;
    private TextView tvPlaceHolder;

    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    private ObjectDetector detector;
    ObjectDetector.ObjectDetectorOptions options;

    public SamplesFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            load_model();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_samples, container, false);

        Button captureImageButton = view.findViewById(R.id.captureImageButton);
        inputImageView = view.findViewById(R.id.imageViewID);
        tvPlaceHolder = view.findViewById(R.id.welcomeMessageTVID);

        ImageView sampleImage1 = view.findViewById(R.id.sampleImage1);
        ImageView sampleImage2 = view.findViewById(R.id.sampleImage2);

        // Click on sample image 1
        sampleImage1.setOnClickListener(v -> {
            try{
                setViewAndDetect(getSampleImage(R.drawable.image_test));
            }
            catch (IOException e){
                e.printStackTrace();
            }
        });

        sampleImage2.setOnClickListener(v -> {
            try{
                setViewAndDetect(getSampleImage(R.drawable.kite));
            }
            catch (IOException e){
                e.printStackTrace();
            }
        });
        // Init button when being clicked
        captureImageButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Taking a photo", Toast.LENGTH_LONG).show();
            if (requireActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            }
            else
            {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            // resize the image to fit the image view
            photo = Bitmap.createScaledBitmap(photo, inputImageView.getWidth(), inputImageView.getHeight(), false);

            try {
                runObjectDetection(photo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void load_model() throws IOException {
        // initialize the detector object
        options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(100)
                .setScoreThreshold(0.35f)
                .build();

        // Load model
        detector = ObjectDetector.createFromFileAndOptions(
                requireActivity(),
                "android.tflite",
                options);
    }

    private void setViewAndDetect(Bitmap bitmap) throws IOException {
        // Display captured image
        inputImageView.setImageBitmap(bitmap);
        tvPlaceHolder.setVisibility(View.INVISIBLE);
        try {
            runObjectDetection(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runObjectDetection(Bitmap bitmap) throws IOException {
        // Step 1: convert bitmap image to tensor image
        TensorImage image = TensorImage.fromBitmap(bitmap);

        List<Detection> results = detector.detect(image);

        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(outputBitmap);
        Paint pen = new Paint();
        pen.setTextAlign(Paint.Align.LEFT);

        for (Detection result: results){
            pen.setColor(Color.RED);
            pen.setStrokeWidth(8F);
            pen.setStyle(Paint.Style.STROKE);
            RectF box = result.getBoundingBox();
            canvas.drawRect(box, pen);

            Rect tagSize = new Rect(0, 0, 0, 0);

            pen.setStyle(Paint.Style.FILL_AND_STROKE);
            pen.setColor(Color.YELLOW);
            pen.setStrokeWidth(2F);

            Category category = result.getCategories().get(0);
            String text = category.getLabel() + " "  + Math.round(category.getScore()*100) + "%";

            pen.setTextSize(96F);
            pen.getTextBounds(text, 0, text.length(), tagSize);

            canvas.drawText(
                    text, box.left,
                    box.top, pen
            );
        }
        inputImageView.setImageBitmap(outputBitmap);
    }

    private Bitmap getSampleImage(int resID) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        return BitmapFactory.decodeResource(getResources(), resID, options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(requireContext(), "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(requireContext(), "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}