package com.safepayshield.app.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public final class VoiceManager implements TextToSpeech.OnInitListener {
    private final Context context;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private final VoiceListener listener;
    private boolean ttsReady = false;

    public interface VoiceListener {
        void onCommand(String command);
        void onReady();
        void onError(String error);
    }

    public VoiceManager(Context context, VoiceListener listener) {
        this.context = context;
        this.listener = listener;
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
            ttsReady = true;
            if (listener != null) listener.onReady();
        }
    }

    public void speak(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void startListening() {
        startListening(Locale.getDefault());
    }

    public void startListening(Locale locale) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO}, 102);
            }
            if (listener != null) listener.onError("Microphone permission required.");
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (listener != null) listener.onError("Speech recognition is not available on this device.");
            return;
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    if (listener != null) listener.onReady();
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    String message = "Could not understand. Please try again.";
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Microphone audio error.";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Speech service error.";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "Microphone permission required.";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            message = "Network or speech service is unavailable.";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "Could not understand. Please try again.";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "No speech detected. Please try again.";
                            break;
                        default:
                            message = "Speech recognition failed.";
                            break;
                    }
                    if (listener != null) listener.onError(message);
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty() && listener != null) {
                        listener.onCommand(matches.get(0));
                    } else if (listener != null) {
                        listener.onError("No speech detected.");
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void setLanguage(Locale locale) {
        if (tts != null && ttsReady) {
            tts.setLanguage(locale);
        }
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
