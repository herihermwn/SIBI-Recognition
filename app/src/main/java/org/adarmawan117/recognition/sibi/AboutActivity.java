package org.adarmawan117.recognition.sibi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class AboutActivity extends AppCompatActivity {

    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> {
            backToMain();
        });
    }

    @Override
    public void onBackPressed() {
        backToMain();
    }

    private void backToMain() {
        Intent moveIntent = new Intent(AboutActivity.this, DetectorActivity.class);
        startActivity(moveIntent);
        finish();
    }
}