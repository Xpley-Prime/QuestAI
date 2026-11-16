package com.xpley.questai;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import android.provider.Settings;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.vosk.android.RecognitionListener;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity implements RecognitionListener {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status, history, partial;
    private ScrollView scroller;
    private Button startButton;
    private SpeechService service;
    private Model model;
    private TextToSpeech tts;
    private LocalLlm llm;
    private boolean generating;
    private boolean active, preparing, speaking, wake, closing;
    private long lastHeard;
    private static final String WAKE = "эй квест";
    private final Runnable resume = this::beginCapture;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(30, 25, 30, 25);
        root.setBackgroundColor(Color.rgb(15, 20, 33));
        TextView title = new TextView(this);
        title.setText("QUEST AI  •  OFFLINE");
        title.setTextSize(26); title.setTextColor(Color.WHITE);
        root.addView(title);
        status = new TextView(this);
        status.setTextColor(Color.rgb(103, 229, 197));
        status.setTextSize(16);
        status.setPadding(0, 14, 0, 14);
        root.addView(status);
        LinearLayout controls = new LinearLayout(this);
        startButton = new Button(this);
        startButton.setText("▶ Запустить");
        controls.addView(startButton, new LinearLayout.LayoutParams(0, -2, 1));
        Button clear = new Button(this);
        clear.setText("Очистить");
        controls.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(controls);
        partial = new TextView(this);
        partial.setTextColor(Color.LTGRAY);
        partial.setTextSize(15);
        root.addView(partial);
        scroller = new ScrollView(this);
        history = new TextView(this);
        history.setTextColor(Color.WHITE);
        history.setTextSize(18);
        scroller.addView(history);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView footer = new TextView(this);
        footer.setText("Скажи: «Эй, Квест», затем команду.\nОфлайн-распознавание Vosk • работает только при открытом окне.");
        footer.setTextColor(Color.LTGRAY);
        root.addView(footer);
        setContentView(root);
        add("QuestAI", "Привет! Нажми «Запустить», затем скажи «Эй, Квест».");
        setStatus("Подготовка...");
        llm = new LocalLlm(this);
        tts = new TextToSpeech(this, code -> {
            if (code == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(new Locale("ru", "RU"));
                if (language < 0) add("Система", "Русский голос TTS отсутствует. Текстовые ответы продолжат работать.");
            } else add("Система", "Синтез речи недоступен, текстовые ответы работают.");
        });
        startButton.setOnClickListener(v -> {
            if (active) stop();
            else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start();
            else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 7);
        });
        clear.setOnClickListener(v -> history.setText(""));
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 7);
        else setStatus("Нажми «Запустить»");
    }
    private void add(String who, String value) {
        history.append(who + ": " + value + "\n\n");
        scroller.post(() -> scroller.fullScroll(ScrollView.FOCUS_DOWN));
    }
    private void setStatus(String text) { status.setText(text); }
    private void start() {
        if (active || preparing) return;
        llm.prepare(new LocalLlm.Listener() {
            @Override public void onProgress(String message) { runOnUiThread(() -> setStatus(message)); }
            @Override public void onReady() { runOnUiThread(() -> { add("Система", "Локальная нейросеть Qwen готова."); if (active && service != null) setStatus("● Слушаю: «Эй, Квест»"); }); }
            @Override public void onError(String message) { runOnUiThread(() -> add("Ошибка ИИ", message)); }
        });
        active = true; wake = false;
        startButton.setText("■ Остановить");
        if (model != null) { beginCapture(); return; }
        preparing = true;
        setStatus("Распаковываю офлайн-модель русского языка...");
        StorageService.unpack(this, "model-ru", "vosk-ru",
            m -> { preparing = false; model = m; if (active) beginCapture(); },
            e -> { preparing = false; active = false; startButton.setText("▶ Запустить");
                setStatus("Не удалось загрузить модель");
                add("Ошибка", e.getMessage() == null ? e.toString() : e.getMessage()); });
    }
    private void beginCapture() {
        ui.removeCallbacks(resume);
        if (!active || speaking || generating || closing || model == null || service != null) return;
        try {
            service = new SpeechService(new Recognizer(model, 16000.0f), 16000.0f);
            service.startListening(this);
            setStatus(wake ? "● Слушаю команду..." : "● Слушаю: «Эй, Квест»");
        } catch (Exception e) {
            add("Ошибка микрофона", e.toString());
            stopCapture();
            if (active) ui.postDelayed(resume, 1500);
        }
    }
    private void stopCapture() {
        if (service != null) {
            try { service.stop(); service.shutdown(); } catch (Exception ignored) {}
            service = null;
        }
    }
    private void stop() {
        active = false; wake = false;
        ui.removeCallbacks(resume);
        stopCapture();
        if (tts != null) tts.stop();
        speaking = false;
        startButton.setText("▶ Запустить");
        setStatus("Остановлен");
        partial.setText("");
    }
    private void process(String text) {
        text = text.toLowerCase(new Locale("ru", "RU")).trim();
        if (text.isEmpty()) return;
        if (!wake) {
            int at = text.indexOf(WAKE);
            if (at < 0) return;
            wake = true;
            text = text.substring(at + WAKE.length()).trim();
            if (text.isEmpty()) {
                setStatus("● Слушаю команду...");
                return;
            }
        } else if (text.contains(WAKE)) {
            text = text.substring(text.indexOf(WAKE) + WAKE.length()).trim();
        }
        if (text.isEmpty()) return;
        wake = false;
        add("Ты", text);
        String reply = answer(text);
        if (reply == null) {
            stopCapture();
            generating = true;
            setStatus("◉ Думаю локально...");
            llm.generate(text, response -> runOnUiThread(() -> {
                generating = false;
                if (!active || closing) return;
                add("QuestAI", response);
                speak(response);
            }));
        } else {
            add("QuestAI", reply);
            speak(reply);
        }
    }
    private String answer(String q) {
        if (q.contains("время") || q.contains("который час"))
            return "Сейчас " + new SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru-RU")).format(new Date());
        if (q.contains("дата") || q.contains("число") || q.contains("день сегодня"))
            return "Сегодня " + new SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ru-RU")).format(new Date());
        if (q.contains("привет")) return "Привет! Я QuestAI.";
        if (q.contains("как дела")) return "Всё отлично. Я слушаю!";
        if (q.contains("повтори")) return "Ты сказал: " + q;
        if (q.contains("настройк")) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); return "Открываю настройки."; }
            catch (Exception e) { return "Не удалось открыть настройки."; }
        }
        if (q.contains("помощь") || q.contains("умеешь"))
            return "Пока доступны время, дата, настройки и простые команды. На свободные вопросы отвечает локальная нейросеть Qwen.";
        return null;
    }
    private void speak(String reply) {
        stopCapture();
        if (tts == null) { if (active) ui.postDelayed(resume, 400); return; }
        speaking = true;
        setStatus("◉ Отвечаю...");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { ui.post(() -> finishSpeaking()); }
            @Override public void onError(String id) { ui.post(() -> finishSpeaking()); }
        });
        int code = tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "quest-answer");
        if (code == TextToSpeech.ERROR) finishSpeaking();
    }
    private void finishSpeaking() {
        speaking = false;
        if (active) ui.postDelayed(resume, 350);
    }
    private void parse(String json, boolean isPartial) {
        try {
            String text = new JSONObject(json).optString(isPartial ? "partial" : "text", "");
            if (isPartial) {
                partial.setText(text.isEmpty() ? "" : "Слышу: " + text);
            } else {
                partial.setText("");
                process(text);
            }
        } catch (Exception ignored) {}
    }
    @Override public void onPartialResult(String hypothesis) { runOnUiThread(() -> parse(hypothesis, true)); }
    @Override public void onResult(String hypothesis) { runOnUiThread(() -> parse(hypothesis, false)); }
    @Override public void onFinalResult(String hypothesis) { runOnUiThread(() -> parse(hypothesis, false)); }
    @Override public void onError(Exception exception) {
        runOnUiThread(() -> {
            add("Ошибка микрофона", exception.toString());
            stopCapture();
            if (active) ui.postDelayed(resume, 1300);
        });
    }
    @Override public void onTimeout() {
        runOnUiThread(() -> { stopCapture(); if (active) ui.postDelayed(resume, 400); });
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 7) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) setStatus("Нажми «Запустить»");
            else setStatus("Разреши микрофон в настройках Quest");
        }
    }
    @Override protected void onPause() { super.onPause(); if (active) stop(); }
    @Override protected void onDestroy() {
        closing = true; stop(); ui.removeCallbacksAndMessages(null);
        if (model != null) model.close();
        if (llm != null) llm.shutdown();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
