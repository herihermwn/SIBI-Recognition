package org.adarmawan117.recognition.sibi.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.adarmawan117.recognition.sibi.CameraActivity;
import org.adarmawan117.recognition.sibi.DetectorActivity;
import org.adarmawan117.recognition.sibi.R;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    CardView speechToText;
    CardView gestureToText;
    CardView textToGesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        speechToText = findViewById(R.id.speechToTextCard);
        gestureToText = findViewById(R.id.gestureToTextCard);
        textToGesture = findViewById(R.id.textToGestureCard);

        speechToText.setOnClickListener(this);
        gestureToText.setOnClickListener(this);
        textToGesture.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.speechToTextCard:
                Intent speechToTextIntent = new Intent(HomeActivity.this, SpeechToTextActivity.class);
                startActivity(speechToTextIntent);
                break;

            case R.id.gestureToTextCard:
                Intent gestureToTextIntent = new Intent(HomeActivity.this, DetectorActivity.class);
                startActivity(gestureToTextIntent);
                break;

            case R.id.textToGestureCard:
                // TODO : Change target intent
                Intent textToGestureIntent = new Intent(HomeActivity.this, Splashscreen.class);
                startActivity(textToGestureIntent);
                break;
        }
    }
}