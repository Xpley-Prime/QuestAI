# QuestAI — Quest 3S Button Tester

Minimal Android 2D-panel test app to discover which key and button events Horizon OS forwards to an ordinary application. **This does not remap system buttons.**

## Safety
- No root, ADB shell changes, privileged permissions, network, microphone or camera.
- Does not run in background.
- System-reserved action button may never reach this application.
- You can uninstall it normally.

## Build
GitHub Actions → Build Quest APK → latest successful run → Artifacts → QuestAI-ButtonTester-APK. Download and extract ZIP to get `app-debug.apk`.

## Test
Open the app, press the round action button on Quest 3S, compare with controller button events. If the system takes focus, the app may log a pause event instead. If nothing is logged, this does not establish that the button can be remapped.

## Installation
Downloading APK alone does not install it. Use an authorized sideloading method with developer mode; direct on-headset installation without PC is not guaranteed.
