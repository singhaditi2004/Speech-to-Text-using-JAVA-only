package com.example.transcribe_speech;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private SpeechRecognizer speechRecognizer;
    private TextView transcriptionTextView;
    private Intent recognizerIntent;
    private StringBuilder transcriptionBuffer = new StringBuilder();
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transcriptionTextView = findViewById(R.id.tvTranscription);
        Button startButton = findViewById(R.id.btnRecord);
        Button stopButton = findViewById(R.id.btnStop);
        Button saveButton = findViewById(R.id.btnSave);
        Button restartButton = findViewById(R.id.btnRestart);

        if (checkAudioPermission()) {
            initializeSpeechRecognition();
        }

        startButton.setOnClickListener(v -> startSpeechRecognition());
        stopButton.setOnClickListener(v -> stopSpeechRecognition());
        saveButton.setOnClickListener(v -> saveTranscription());
        restartButton.setOnClickListener(v -> restartSpeechRecognition());
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
            return false;
        }
        return true;
    }

    private void initializeSpeechRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                transcriptionTextView.setText("Speak now...");
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
                transcriptionTextView.setText("Listening...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                transcriptionTextView.setText("Processing...");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                transcriptionTextView.setText("Error: " + getErrorText(error));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String newText = matches.get(0);
                    transcriptionBuffer.append(newText).append(" ");
                    transcriptionTextView.setText(transcriptionBuffer.toString());
                }

                // Automatically restart recognition
                if (isListening) {
                    startSpeechRecognition();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partialMatches = partialResults
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (partialMatches != null && !partialMatches.isEmpty()) {
                    transcriptionTextView.setText(transcriptionBuffer.toString() +
                            partialMatches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startSpeechRecognition() {
        if (speechRecognizer != null && !isListening) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            transcriptionTextView.setText("Stopped. Transcription: " +
                    transcriptionBuffer.toString());
        }
    }

    private void restartSpeechRecognition() {
        stopSpeechRecognition();
        transcriptionBuffer.setLength(0); // Clear previous transcription
        transcriptionTextView.setText("Ready to transcribe");
        startSpeechRecognition();
    }

    private void saveTranscription() {
        if (transcriptionBuffer.length() == 0) {
            Toast.makeText(this, "No transcription to save", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(getExternalFilesDir(null),
                "transcription_" + System.currentTimeMillis() + ".txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(transcriptionBuffer.toString().getBytes());
            Toast.makeText(this, "Saved: " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            default:
                return "Unknown error";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}