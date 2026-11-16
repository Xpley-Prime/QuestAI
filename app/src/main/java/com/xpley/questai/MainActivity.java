package com.xpley.questai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements RecognitionListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status, transcript;
    private ScrollView scroll;
    private boolean enabled = false, speaking = false, listening = false, destroyed = false, awaitingCommand = false;
    private long lastError = 0;
    private static final String WAKE = "эй квест";
    private final Runnable restart = this::listen;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(0);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 20);
        root.setBackgroundColor(Color.rgb(13, 19, 32));
        TextView heading = new TextView(this);
        heading.setText("QUEST AI  •  VOICE");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(25);
        heading.setTypeface(null, Typeface.BOLD);
        root.addView(heading);
        status = new TextView(this);
        status.setTextColor(Color.rgb(120, 222, 205));
        status.setTextSize(16);
        status.setPadding(0, 12, 0, 12);
        root.addView(status);
        LinearLayout actions = new LinearLayout(this);
        Button toggle = new Button(this);
        toggle.setText("▶ Запустить");
        actions.addView(toggle, new LinearLayout.LayoutParams(0, -2, 1));
        Button clear = new Button(this);
        clear.setText("Очистить");
        actions.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actions);
        scroll = new ScrollView(this);
        transcript = new TextView(this);
        transcript.setTextColor(Color.WHITE);
        transcript.setTextSize(17);
        transcript.setPadding(4, 16, 4, 16);
        scroll.addView(transcript);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView hint = new TextView(this);
        hint.setText("Скажи: «Эй, Квест, сколько времени?»\nРаботает только при открытом приложении. Распознавание зависит от установленного движка Android.");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(13);
        root.addView(hint);
        setContentView(root);
        showStatus("Подготовка микрофона...");
        append("QuestAI", "Привет! Я готов к голосовым командам.");
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int lang = tts.setLanguage(new Locale("ru", "RU"));
                if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED)
                    append("Система", "Русский голос TTS не установлен. Проверь настройки синтеза речи.");
            } else append("Система", "Движок озвучивания недоступен.");
        });
        toggle.setOnClickListener(v -> {
            enabled = !enabled;
            toggle.setText(enabled ? "■ Остановить" : "▶ Запустить");
            if (enabled) startVoice();
            else stopVoice();
        });
        clear.setOnClickListener(v -> transcript.setText(""));
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 42);
        else showStatus("Нажми «Запустить»");
    }

    private void startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showStatus("Нужно разрешение на микрофон");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 42);
            enabled = false;
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showStatus("Нет движка распознавания речи Android");
            enabled = false;
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
        }
        listen();
    }

    private void listen() {
        handler.removeCallbacks(restart);
        if (!enabled || speaking || destroyed || recognizer == null || listening) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        try {
            listening = true;
            recognizer.startListening(intent);
            showStatus("● Слушаю: «Эй, Квест...»");
        } catch (Exception e) {
            listening = false;
            showStatus("Ошибка микрофона: " + e.getMessage());
            schedule(1200);
        }
    }

    private void schedule(long delay) {
        handler.removeCallbacks(restart);
        if (enabled && !destroyed) handler.postDelayed(restart, delay);
    }

    private void stopVoice() {
        handler.removeCallbacks(restart);
        listening = false;
        if (recognizer != null) { recognizer.cancel(); }
        if (tts != null) tts.stop();
        speaking = false;
        showStatus("Остановлен");
    }

    private void append(String who, String message) {
        transcript.append(who + ": " + message + "\n\n");
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void showStatus(String value) { status.setText(value); }

    private String answer(String question) {
        String q = question.toLowerCase(new Locale("ru", "RU")).trim();
        if (q.contains("время") || q.contains("который час"))
            return "Сейчас " + new SimpleDateFormat("HH:mm", new Locale("ru", "RU")).format(new Date());
        if (q.contains("дата") || q.contains("какое сегодня число") || q.contains("какой сегодня день"))
            return "Сегодня " + new SimpleDateFormat("d MMMM yyyy", new Locale("ru", "RU")).format(new Date());
        if (q.contains("привет")) return "Привет! Я QuestAI, твой голосовой помощник.";
        if (q.contains("как дела")) return "Отлично! Готов помогать.";
        if (q.contains("что ты умеешь") || q.contains("помощь"))
            return "Могу назвать время и дату, открыть настройки и отвечать на простые команды. Свободные вопросы пока не поддерживаются.";
        if (q.contains("настройк")) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); return "Открываю настройки."; }
            catch (Exception e) { return "Не получилось открыть настройки."; }
        }
        if (q.contains("повтори")) return "Повторяю: " + question;
        return "Пока я понимаю только встроенные команды. Для свободных ответов нужна локальная языковая модель.";
    }

    private void handle(String raw) {
        String text = raw.toLowerCase(new Locale("ru", "RU"));
        int i = text.indexOf(WAKE);
        if (i < 0 && !awaitingCommand) { schedule(250); return; }
        String command = (i >= 0 ? raw.substring(i + WAKE.length()) : raw).replaceFirst("^[,.:!\\s]+", "").trim();
        if (command.isEmpty()) {
            awaitingCommand = true;
            append("QuestAI", "Слушаю. Говори команду.");
            schedule(400);
            return;
        }
        awaitingCommand = false;
        append("Ты", command);
        String reply = answer(command);
        append("QuestAI", reply);
        if (tts != null) {
            speaking = true;
            showStatus("◉ Отвечаю...");
            // Wait for the utterance to finish before resuming the microphone.
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) { runOnUiThread(() -> { speaking = false; schedule(400); }); }
                @Override public void onError(String id) { runOnUiThread(() -> { speaking = false; schedule(400); }); }
            });
            int result = tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "quest-reply");
            if (result == TextToSpeech.ERROR) { speaking = false; schedule(400); }
        } else schedule(400);
    }

    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() { showStatus("● Распознаю речь..."); }
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onError(int error) {
        listening = false;
        if (!enabled) return;
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            enabled = false;
            showStatus("Нет разрешения на микрофон");
            return;
        }
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            try { recognizer.cancel(); } catch (Exception ignored) {}
        }
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            long now = System.currentTimeMillis();
            if (now - lastError > 10000) {
                append("Система", "Ошибка распознавания: " + error + ". Проверь доступность офлайн-пакета русского языка.");
                lastError = now;
            }
        }
        schedule(700);
    }
    @Override public void onResults(Bundle results) {
        listening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) handle(matches.get(0));
        else schedule(350);
    }
    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 42) showStatus(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
            ? "Микрофон разрешён. Нажми «Запустить»." : "Микрофон запрещён.");
    }
    @Override protected void onPause() { super.onPause(); if (enabled) stopVoice(); enabled = false; }
    @Override protected void onDestroy() {
        destroyed = true;
        enabled = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
