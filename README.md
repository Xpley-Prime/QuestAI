# QuestAI Voice for Meta Quest 3S

Android foreground voice assistant prototype. No PC is required **to run the APK once installed**.

## Current features
- Russian speech commands with wake phrase **«Эй, Квест»** (together with a command, or as a separate phrase)
- Visible conversation transcript in the app
- Spoken answers through installed Android Text-to-Speech
- Time, date, greetings, help, repeat, attempt to open system settings
- Start/stop microphone button; microphone stops when the app is not foregrounded

## Important limitations
- This is **not yet a local LLM**. Unknown questions receive an honest fallback.
- Speech recognition uses Android SpeechRecognizer with EXTRA_PREFER_OFFLINE. This is a preference, **not a guarantee** that recognition works offline. An installed offline Russian recognition engine is required for genuine offline use.
- Android TTS requires a Russian voice installed on the device for offline speech output.
- Continuous recognition is implemented by restarting speech recognition after each utterance. Behavior on Quest/Horizon OS must be tested; the OS can restrict microphone access.
- No background service, no system button interception, no cloud API keys.

## Build
GitHub Actions: Actions → Build Quest APK → latest **successful** run → artifact **QuestAI-Voice-APK**. Extract the ZIP and install app-debug.apk.

If the build is red, no valid APK should be assumed. The Android project can also be built using Gradle 8.9 and Android SDK 35.

## Usage
Grant microphone permission, tap **Запустить**, say **«Эй, Квест, сколько времени?»**. Responses appear in text and are spoken.
