package org.adarmawan117.recognition.sibi.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import org.adarmawan117.recognition.sibi.DetectorActivity;
import org.adarmawan117.recognition.sibi.R;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

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

        speechToText.setOnClickListener(this);
        gestureToText.setOnClickListener(this);
        infoButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.speechToGestureLayout:
                Intent speechToTextIntent = new Intent(HomeActivity.this, SpeechToGestureActivity.class);
                startActivity(speechToTextIntent);
                break;

            case R.id.gestureToTextLayout:
                Intent gestureToTextIntent = new Intent(HomeActivity.this, DetectorActivity.class);
                startActivity(gestureToTextIntent);
                break;

            case R.id.aboutAppsLayout:
                Intent aboutIntent = new Intent(HomeActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
                break;
        }
    }

}