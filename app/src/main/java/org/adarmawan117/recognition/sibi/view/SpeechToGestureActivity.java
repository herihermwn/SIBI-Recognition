package org.adarmawan117.recognition.sibi.view;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.adarmawan117.recognition.sibi.R;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechToGestureActivity extends AppCompatActivity implements View.OnClickListener {

    public static final Integer RecordAudioRequestCode = 1;
    private EditText resultSpeech;
    private ImageView backButton, plusDelayButton, minusDelayButton, gestureImage;
    private LinearLayout pasteButton, clearButton;
    private TextView delayText;
    private Button showGesture;
    private ClipboardManager clipBoard;
    private boolean playGesture = false;
    private int delay = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_speech);

        init();
    }

    private void init() {

        // Inisialisasi View
        ImageButton speechButton = findViewById(R.id.speech_button);
        resultSpeech = findViewById(R.id.result_speech);
        gestureImage = findViewById(R.id.gesture_image);
        backButton = findViewById(R.id.back_button);
        pasteButton = findViewById(R.id.paste_button);
        clearButton = findViewById(R.id.clear_button);
        delayText = findViewById(R.id.delay);
        plusDelayButton = findViewById(R.id.plus);
        minusDelayButton = findViewById(R.id.minus);
        showGesture = findViewById(R.id.show_gesture);
        clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        // Set event onClick
        speechButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        pasteButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        plusDelayButton.setOnClickListener(this);
        minusDelayButton.setOnClickListener(this);
        showGesture.setOnClickListener(this);

        // Meminta perizinan mengakses mic
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission();
        }

    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> listResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String result = resultSpeech.getText().toString() + " " + listResult.get(0);
                resultSpeech.setText(result);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permintaan di izinkan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;

            case R.id.plus:
                // Menambahkan delay, dengan batas maksimal 9
                if (delay >= 9) return;
                delay++;
                delayText.setText(String.valueOf(delay));
                break;

            case R.id.minus:
                // Mengurangi delay, dengan batas minimal 2
                if (delay == 2) return;
                delay--;
                delayText.setText(String.valueOf(delay));
                break;

            case R.id.clear_button:
                resultSpeech.setText("");
                break;

            case R.id.paste_button:
                // Mendapatkan text yang di salin
                ClipData clipData = clipBoard.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                String text = item.getText().toString();
                resultSpeech.setText(text);
                break;

            case R.id.speech_button:
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                );
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("id-ID"));

                // Check device support speech to text or not
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, 101);
                } else {
                    showSnackbar("Perangkat tidak mendukung text to speech", true);
                }
                break;

            case R.id.show_gesture:
                int lengthResult = resultSpeech.getText().toString().length();

                // Check lenght input
                if (playGesture) {
                    showSnackbar("Sedang menjalankan gesture, mohon tunggu hingga selesai", true);
                }
                else if (lengthResult > 0) {
                    // Run background
                    Runnable r = this::textToGesture;
                    new Thread(r).start();
                } else {
                    showSnackbar("Input tidak boleh kosong", true);
                }
                break;

        }
    }

    private void showSnackbar(String message, boolean warnNotif) {
        int colorId = android.R.color.holo_green_dark;
        if (warnNotif) {
            colorId = android.R.color.holo_red_light;
        }
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(colorId))
                .show();
    }

    private void textToGesture() {
        playGesture = true;
        // Remove all except alphabet & number
        String result = resultSpeech.getText().toString().trim().replaceAll("[^a-zA-Z0-9]", " ");
        // Remove 2 or more space
        result = result.replaceAll(" +", " ");

        for (int i = 0; i < result.length(); i++) {
            try {
                int id = 0;

                // check if input space
                if (' ' == result.charAt(i)) {
                    id = this.getResources().getIdentifier("huruf_spasi", "drawable", this.getPackageName());
                } else {
                    id = this.getResources().getIdentifier("huruf_" + result.charAt(i), "drawable", this.getPackageName());
                }

                // Set image need use main thread
                int finalId = id;
                runOnUiThread(() -> gestureImage.setImageResource(finalId));

                // Check last loop
                if (i != result.length() - 1) {
                    Thread.sleep(delay * 1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Show snackbar
        showSnackbar("Gesture telah selesai", false);
        playGesture = false;
    }
}