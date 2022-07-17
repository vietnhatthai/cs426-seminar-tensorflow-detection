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

    private ObjectDetector detector;        // Object detector
    private ObjectDetector.ObjectDetectorOptions options; // Object detector options

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

        // Click on sample image 2
        sampleImage2.setOnClickListener(v -> {
            try{
                setViewAndDetect(getSampleImage(R.drawable.kite));
            }
            catch (IOException e){
                e.printStackTrace();
            }
        });

        // Click on capture image button
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
            Bitmap photo = (Bitmap) data.getExtras().get("data"); //get the image from the camera
            // resize the image to fit the image view
            photo = Bitmap.createScaledBitmap(photo, inputImageView.getWidth(), inputImageView.getHeight(), false);

            try {
                runObjectDetection(photo); // run the object detection on the photo
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void load_model() throws IOException {
        // initialize the detector object
        options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)          // set the maximum number of results to return
                .setScoreThreshold(0.35f)  // set the minimum score threshold for the results to be returned
                .build();                  // build the options

        // Load model
        detector = ObjectDetector.createFromFileAndOptions(
                requireActivity(),
                "voc2007.tflite",     // path to the model file
                options);
    }

    private void setViewAndDetect(Bitmap bitmap) throws IOException {
        // Display captured image
        inputImageView.setImageBitmap(bitmap); // set the image view with the captured image
        tvPlaceHolder.setVisibility(View.INVISIBLE); // hide the placeholder text view
        try {
            runObjectDetection(bitmap);     // run object detection on the image
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runObjectDetection(Bitmap bitmap) throws IOException {
        // convert bitmap image to tensor image
        TensorImage image = TensorImage.fromBitmap(bitmap);
        List<Detection> results = detector.detect(image);       // detect the objects in the image
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); // create a copy of the original image
        Canvas canvas = new Canvas(outputBitmap);   // create a canvas to draw on the image
        Paint pen = new Paint();                    // create a paint object to draw on the canvas
        pen.setTextAlign(Paint.Align.LEFT);         // set the text alignment to left

        for (Detection result: results){    // for each result
            pen.setColor(Color.RED);                // set the color of the text to red
            pen.setStrokeWidth(8F);                 // set the stroke width to 8 pixels
            pen.setStyle(Paint.Style.STROKE);       // set the style to stroke
            RectF box = result.getBoundingBox();    // get the bounding box of the object
            canvas.drawRect(box, pen);              // draw the bounding box on the canvas

            Rect tagSize = new Rect(0, 0, 0, 0);    // create a rectangle to store the size of the text
            Category category = result.getCategories().get(0);           // get the category of the object
            String text = category.getLabel() + " "  + Math.round(category.getScore()*100) + "%";   // create the text to be drawn

            pen.setTextSize(96F);                   // set the text size to 96 pixels
            pen.getTextBounds(text, 0, text.length(), tagSize);     // get the size of the text
            // fill background of the text
            pen.setColor(Color.WHITE);              // set the color of the text to white
            pen.setStyle(Paint.Style.FILL_AND_STROKE);      // set the style to fill and stroke
            // draw the background of the text
            canvas.drawRect(box.left, box.top - tagSize.height(), box.left + tagSize.width(), box.top, pen);
            pen.setColor(Color.RED);                // set the color of the text to red
            pen.setStrokeWidth(2F);                 // set the stroke width to 2 pixels
            canvas.drawText(text, box.left, box.top, pen);  // draw the text on the canvas
        }
        inputImageView.setImageBitmap(outputBitmap);    // display the output image on the image view
    }

    private Bitmap getSampleImage(int resID) {
        final BitmapFactory.Options options = new BitmapFactory.Options();  //  create a new options object
        options.inMutable = true;   // set the options to be mutable
        return BitmapFactory.decodeResource(getResources(), resID, options); // decode the resource into a bitmap
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
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); // create a new intent to capture an image
                startActivityForResult(cameraIntent, CAMERA_REQUEST);       // start the camera activity
            }
            else
            {
                Toast.makeText(requireContext(), "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}