package org.adarmawan117.recognition.sibi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class Splashscreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        moveActivity();
    }

    void moveActivity() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent moveIntent = new Intent(Splashscreen.this, DetectorActivity.class);
                startActivity(moveIntent);
                finish();
            }
        }, 2000);
    }

}