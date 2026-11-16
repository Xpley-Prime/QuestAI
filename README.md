# QuestAI for Meta Quest 3S

An Android VR-window voice assistant with **fully local Russian speech recognition** using [Vosk](https://alphacephei.com/vosk/), spoken answers via Android TTS and a visible conversation history.

## How it works
1. Install the APK from the latest **successful** GitHub Actions build.
2. Launch QuestAI from Unknown Sources and grant microphone permission.
3. Press **▶ Запустить**.
4. Say **«Эй, Квест»**, then **«сколько времени?»** (or both in one phrase).
5. The app shows the recognized command and its response, and tries to speak the answer.
6. Recognition continues until you press Stop or leave the app.

The GitHub Actions build downloads `vosk-model-small-ru-0.22.zip` from the official Vosk website and packages the Russian acoustic model inside the APK. The app unpacks the model on first start. **No internet connection is needed for speech recognition after installation.**

## Supported commands
Time, date, greeting, help, repeat, and opening Android settings. Unsupported queries receive an explicit fallback response.

## Limitations
**A local large language model (LLM) is not integrated yet.** Vosk is speech-to-text, not a generative AI. Free-form chat, natural-language tools, and broad device control are not currently implemented. Do not describe this version as a completed AI assistant.

TTS is provided by Android; offline spoken output requires a working Russian voice installed on the headset. Text output works without TTS.

The microphone is active only while the app is in the foreground. This version does not intercept headset buttons or listen during other games.

## Build
GitHub → Actions → Build Quest APK → latest successful run → artifact `QuestAI-Offline-Voice-APK`. Extract the ZIP and install `app-debug.apk`.

**A green GitHub Actions run is required to confirm an installable APK.** If the workflow fails, inspect its job logs before attempting installation.

## License notes
Vosk engine and its model have their own licenses. Review the upstream model's terms before distribution.
