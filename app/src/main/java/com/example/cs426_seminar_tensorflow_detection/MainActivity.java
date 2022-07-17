package com.example.cs426_seminar_tensorflow_detection;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSampleFragment();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_samples:
                    // Toast.makeText(getApplicationContext(), "Samples", Toast.LENGTH_SHORT).show();
                    getSampleFragment();
                    return true;
                case R.id.navigation_attachment:
                     Toast.makeText(getApplicationContext(), "Attachment not supported yet", Toast.LENGTH_SHORT).show();

                    return true;
                case R.id.navigation_realtime:
                    // Toast.makeText(getApplicationContext(), "Realtime", Toast.LENGTH_SHORT).show();
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .add(R.id.frame_container, new RealTimeFragment())
//                            .commit();
                    MainCameraActivity activity = MainCameraActivity.getInstance();
                    startActivity(new Intent(getApplicationContext(), activity.getClass()));
                    return true;
            }
            return false;
        });
    }

    private void getSampleFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, new SamplesFragment())
                .commit();
    }
}