#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include "llama.h"

static std::mutex guard;
static llama_model *model = nullptr;
static llama_context *ctx = nullptr;
static const llama_vocab *vocab = nullptr;

static std::string fromJava(JNIEnv *env, jstring s) {
    if (!s) return "";
    const char *p = env->GetStringUTFChars(s, nullptr);
    std::string out(p ? p : "");
    if (p) env->ReleaseStringUTFChars(s, p);
    return out;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_xpley_questai_LocalLlm_nativeLoad(JNIEnv *env, jclass, jstring path) {
    std::lock_guard<std::mutex> lock(guard);
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_init();
    auto params = llama_model_default_params();
    params.n_gpu_layers = 0;
    model = llama_model_load_from_file(fromJava(env, path).c_str(), params);
    if (!model) return env->NewStringUTF("Не удалось открыть GGUF модель");
    auto cp = llama_context_default_params();
    cp.n_ctx = 2048;
    cp.n_batch = 512;
    cp.n_threads = 4;
    ctx = llama_init_from_model(model, cp);
    if (!ctx) return env->NewStringUTF("Не удалось создать контекст модели");
    vocab = llama_model_get_vocab(model);
    return nullptr;
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_xpley_questai_LocalLlm_nativeGenerate(JNIEnv *env, jclass, jstring prompt, jint maxTokens) {
    std::lock_guard<std::mutex> lock(guard);
    if (!ctx || !model || !vocab) return env->NewStringUTF("Модель не загружена");
    llama_memory_clear(llama_get_memory(ctx), true);
    std::string user = fromJava(env, prompt);
    std::string formatted =
        "<|im_start|>system\nТы QuestAI, полезный голосовой помощник на русском языке. "
        "Отвечай кратко, точно и по-русски. Не выдумывай возможности управления устройством.<|im_end|>\n"
        "<|im_start|>user\n" + user + "<|im_end|>\n<|im_start|>assistant\n";
    int required = -llama_tokenize(vocab, formatted.c_str(), (int)formatted.size(), nullptr, 0, true, true);
    if (required <= 0 || required > 1700) return env->NewStringUTF("Слишком длинный запрос");
    std::vector<llama_token> tokens(required);
    int n = llama_tokenize(vocab, formatted.c_str(), (int)formatted.size(), tokens.data(), required, true, true);
    if (n <= 0) return env->NewStringUTF("Ошибка токенизации");
    llama_batch batch = llama_batch_get_one(tokens.data(), n);
    if (llama_decode(ctx, batch) != 0) return env->NewStringUTF("Ошибка обработки запроса");
    llama_sampler_chain_params samplerParams = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(samplerParams);
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));
    std::string output;
    int limit = std::max(1, std::min((int)maxTokens, 256));
    for (int i = 0; i < limit; i++) {
        llama_token id = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, id)) break;
        char piece[1024];
        int size = llama_token_to_piece(vocab, id, piece, sizeof(piece), 0, true);
        if (size > 0) output.append(piece, size);
        llama_batch next = llama_batch_get_one(&id, 1);
        if (llama_decode(ctx, next) != 0) break;
    }
    llama_sampler_free(sampler);
    if (output.empty()) output = "Не удалось сформировать ответ.";
    return env->NewStringUTF(output.c_str());
}
