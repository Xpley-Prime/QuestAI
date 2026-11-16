# QuestAI — local AI assistant for Meta Quest 3S

## Architecture
- **Vosk small Russian**: bundled offline Russian speech recognition, microphone listens continuously while the app is foregrounded
- **Qwen2.5 0.5B Instruct Q4_K_M**: actual on-device language model for open-ended questions, running with llama.cpp ARM64 CPU inference
- **Android TTS**: spoken answers where a Russian voice is installed
- **Conversation window**: recognized commands and assistant answers in text
- **Wake phrase**: «Эй, Квест» followed by a command
- **Commands**: date, time, greetings, settings, plus LLM fallback for free-form questions

## First launch
1. Download the artifact `QuestAI-Local-LLM-APK` from a **successful** GitHub Actions run.
2. Extract `app-debug.apk` from the artifact ZIP and install it on the Quest.
3. Allow microphone access, press **Запустить**.
4. On first launch, QuestAI downloads Qwen2.5 0.5B Q4 (~400 MB) to the headset from Hugging Face. **Internet is needed once**. It then loads the model locally. Do not close the app during download.
5. The bundled Vosk speech model is unpacked from the APK on first use.
6. Say «Эй, Квест», then your question. The answer is shown and read aloud if Android TTS supports Russian.

## Offline and limitations
After both models are available, voice recognition and LLM inference run **locally**. No paid API key is needed. The Qwen 0.5B model is small and may be inaccurate. LLM inference can be slow on headset hardware. The Android TTS voice may need separate installation. App microphone access stops on leaving the foreground. System-wide assistant activation, access to games, and privileged device control are not implemented.

## Building
The CI workflow fetches the official Vosk Russian model, clones llama.cpp, builds an ARM64 JNI bridge with the Android NDK and packages an APK. The APK is **not verified** until the GitHub Actions run completes successfully and it is tested on a Quest 3S. Native llama.cpp API changes can require pinning a compatible revision.

## Sources
Vosk: https://alphacephei.com/vosk/
llama.cpp: https://github.com/ggml-org/llama.cpp
Qwen GGUF: https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF
