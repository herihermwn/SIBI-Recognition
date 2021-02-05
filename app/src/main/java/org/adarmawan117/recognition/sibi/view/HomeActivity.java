package org.adarmawan117.recognition.sibi.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.adarmawan117.recognition.sibi.DetectorActivity;
import org.adarmawan117.recognition.sibi.R;

public class HomeActivity extends AppCompatActivity {

    LinearLayout speechToText;
    LinearLayout gestureToText;
    LinearLayout infoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        init();
    }

    private void init() {
        speechToText = findViewById(R.id.speechToGestureLayout);
        gestureToText = findViewById(R.id.gestureToTextLayout);
        infoButton = findViewById(R.id.aboutAppsLayout);

        speechToText.setOnClickListener(v -> {
            speechToTextOnClickListener();
        });
        gestureToText.setOnClickListener(v -> {
            gestureToTextClickListener();
        });
        infoButton.setOnClickListener(v -> {
            aboutAppsOnClickListener();
        });
    }

    private void speechToTextOnClickListener() {
        Intent speechToTextIntent = new Intent(HomeActivity.this, SpeechToGestureActivity.class);
        startActivity(speechToTextIntent);
    }

    private void gestureToTextClickListener() {
        Intent gestureToTextIntent = new Intent(HomeActivity.this, DetectorActivity.class);
        startActivity(gestureToTextIntent);
    }

    private void aboutAppsOnClickListener() {
        Intent aboutIntent = new Intent(HomeActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }
}