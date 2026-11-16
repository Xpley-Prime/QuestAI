package com.xpley.questai;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalLlm {
    public interface Listener {
        void onProgress(String message);
        void onReady();
        void onError(String message);
    }
    private static final String MODEL_URL =
        "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf?download=true";
    private static final long MIN_MODEL_BYTES = 100_000_000L;
    private static boolean libraryLoaded;
    static {
        try { System.loadLibrary("questllm"); libraryLoaded = true; }
        catch (Throwable ignored) { libraryLoaded = false; }
    }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final File modelFile;
    private volatile boolean ready;
    public LocalLlm(Context context) { modelFile = new File(context.getFilesDir(), "qwen2.5-0.5b-q4.gguf"); }
    public boolean isReady() { return ready; }
    public void prepare(Listener listener) {
        executor.execute(() -> {
            if (!libraryLoaded) { listener.onError("Нативный движок llama.cpp не загружен"); return; }
            try {
                if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) {
                    File part = new File(modelFile.getPath() + ".part");
                    listener.onProgress("Скачиваю локальную нейросеть (~400 МБ). Нужен интернет только один раз...");
                    HttpURLConnection conn = (HttpURLConnection)new URL(MODEL_URL).openConnection();
                    conn.setConnectTimeout(30000); conn.setReadTimeout(45000);
                    conn.setInstanceFollowRedirects(true);
                    int response = conn.getResponseCode();
                    if (response != 200) throw new IllegalStateException("HTTP " + response);
                    long total = conn.getContentLengthLong(), count = 0, lastUpdate = 0;
                    try (InputStream input = conn.getInputStream(); FileOutputStream out = new FileOutputStream(part)) {
                        byte[] buf = new byte[64 * 1024]; int n;
                        while ((n = input.read(buf)) != -1) {
                            out.write(buf, 0, n); count += n;
                            if (count - lastUpdate > 8_000_000) {
                                lastUpdate = count;
                                listener.onProgress("Нейросеть: " + (count / 1_000_000) + " МБ" +
                                    (total > 0 ? " / " + (total / 1_000_000) + " МБ" : ""));
                            }
                        }
                    } finally { conn.disconnect(); }
                    if (part.length() < MIN_MODEL_BYTES) throw new IllegalStateException("Файл модели слишком мал");
                    if (!part.renameTo(modelFile)) throw new IllegalStateException("Не удалось сохранить модель");
                }
                listener.onProgress("Загружаю нейросеть в память Quest...");
                String error = nativeLoad(modelFile.getAbsolutePath());
                if (error != null) throw new IllegalStateException(error);
                ready = true;
                listener.onReady();
            } catch (Throwable e) { listener.onError(e.toString()); }
        });
    }
    public void generate(String question, java.util.function.Consumer<String> done) {
        executor.execute(() -> {
            String result;
            try {
                if (!ready) result = "Нейросеть ещё загружается.";
                else result = nativeGenerate(question, 160);
            } catch (Throwable e) { result = "Ошибка нейросети: " + e; }
            done.accept(result);
        });
    }
    public void shutdown() { executor.shutdown(); }
    private static native String nativeLoad(String path);
    private static native String nativeGenerate(String prompt, int maxTokens);
}
