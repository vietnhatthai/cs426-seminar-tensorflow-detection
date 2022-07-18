package com.example.cs426_seminar_tensorflow_detection.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.LinkedList;
import java.util.List;


public class OverlayView extends View {
    private static int INPUT_SIZE = 300;

    private final Paint pen;
    private final List<DrawCallback> callbacks = new LinkedList();
    private List<Detection> results;
    private final float resultsViewHeight;
    private long fps = 0;

    public OverlayView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        pen = new Paint();
        pen.setTextAlign(Paint.Align.LEFT);
        pen.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                20, getResources().getDisplayMetrics()));
        resultsViewHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                112, getResources().getDisplayMetrics());
    }

    public void addCallback(final DrawCallback callback) {
        callbacks.add(callback);
    }

    @SuppressLint({"DefaultLocale", "DrawAllocation"})
    @Override
    public synchronized void onDraw(final Canvas canvas) {
        for (final DrawCallback callback : callbacks) {
            callback.drawCallback(canvas);
        }
        if (results != null) {
                for (Detection result: results) {    // for each result
                    RectF box = reCalcSize(result.getBoundingBox());
                    Category category = result.getCategories().get(0);
                    String text = category.getLabel() + " "  + Math.round(category.getScore()*100) + "%";

                    pen.setColor(Color.RED);
                    pen.setStrokeWidth(8F);
                    pen.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(box, pen);

                    Rect tagSize = new Rect(0, 0, 0, 0);
                    pen.setTextSize(96F);
                    pen.getTextBounds(text, 0, text.length(), tagSize);
                    pen.setColor(Color.WHITE);
                    pen.setStyle(Paint.Style.FILL_AND_STROKE);
                    canvas.drawRect(box.left, box.top - tagSize.height(), box.left + tagSize.width(), box.top, pen);
                    pen.setColor(Color.RED);
                    pen.setStrokeWidth(2F);
                    canvas.drawText(text, box.left, box.top, pen);
                }
        }
        if (fps > 0) {
            Log.d("FPS", String.format("%d", 1000 / fps));
            pen.setColor(Color.RED);
            pen.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawText(String.format("%d FPS", fps), 50, 100, pen);
        }
    }

    private void drawResult() {
        invalidate();
    }

    public void setResults(final List<Detection> results) {
        this.results = results;
        postInvalidate();
    }

    public void setResults(final List<Detection> results, long fps) {
        this.fps = fps;
        this.results = results;
        postInvalidate();
    }

    public interface DrawCallback {
        void drawCallback(final Canvas canvas);
    }

    private RectF reCalcSize(RectF rect) {
        int padding = 5;
        float overlayViewHeight = getHeight() - resultsViewHeight;
        float sizeMultiplier = Math.min((float) getWidth() / (float) INPUT_SIZE,
                overlayViewHeight / (float) INPUT_SIZE);

        float offsetX = (getWidth() - INPUT_SIZE * sizeMultiplier) / 2;
        float offsetY = (overlayViewHeight - INPUT_SIZE * sizeMultiplier) / 2 + resultsViewHeight;

        float left = Math.max(padding, sizeMultiplier * rect.left + offsetX);
        float top = Math.max(offsetY + padding, sizeMultiplier * rect.top + offsetY);

        float right = Math.min(rect.right * sizeMultiplier, getWidth() - padding);
        float bottom = Math.min(rect.bottom * sizeMultiplier + offsetY, getHeight() - padding);

        return new RectF(left, top, right, bottom);
    }

}