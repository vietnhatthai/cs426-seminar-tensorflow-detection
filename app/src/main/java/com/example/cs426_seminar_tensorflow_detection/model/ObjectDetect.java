package com.example.cs426_seminar_tensorflow_detection.model;


import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class ObjectDetect {
    private ObjectDetector detector;        // Object detector

    public ObjectDetect(Context context) throws IOException {
        load_model(context, "voc2007.tflite");
    }

    public ObjectDetect(Context context, String model_name) throws IOException {
        load_model(context, model_name);
    }

    private void load_model(Context context, String model_name) throws IOException {
        // initialize the detector object
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(10)          // set the maximum number of results to return
                .setScoreThreshold(0.35f)   // set the minimum score threshold for the results to be returned
                .build();                   // build the options

        detector = ObjectDetector.createFromFileAndOptions(
                context,
                model_name,
                options);
    }

    public List<Detection> runObjectDetection(Bitmap bitmap) {
        TensorImage image = TensorImage.fromBitmap(bitmap);
        return detector.detect(image);
    }
}
