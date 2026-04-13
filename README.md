# 📺 NepuTV — Android TV WebView Wrapper

A clean Android TV wrapper for **nepu.to** with full D-pad navigation,
hardware-accelerated 4K playback, and automatic TV keyboard support.

---

## ✅ Features

| Feature | Implementation |
|---|---|
| D-pad Navigation | JS focus injection on all posters & buttons |
| OK/Enter = Click | `KEYCODE_DPAD_CENTER` → `element.click()` |
| Back button | WebView history back |
| 4K/1080p Video | Hardware-accelerated WebView layer |
| TV User-Agent | Chrome 120 on Android TV string |
| On-screen Keyboard | AndroidBridge → InputMethodManager |
| Dynamic content | MutationObserver re-injects focus on new elements |
| Media keys | Play/Pause mapped to `<video>.play()/.pause()` |

---

## 🚀 Quick Start (GitHub Actions — Free Build)

1. **Fork** or push this project to a GitHub repository.
2. Go to **Actions** tab → the workflow runs automatically on every push.
3. Download the APK from the **Artifacts** section of the workflow run.
4. Sideload onto your Android TV with ADB:

```bash
adb connect <TV_IP>:5555
adb install NepuTV-debug.apk
```

Or use **Downloader** app (code: your GitHub raw URL) on the TV directly.

---

## 🛠️ Build Locally (Android Studio)

**Requirements:**
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

**Steps:**
```bash
git clone https://github.com/YOUR_USERNAME/NepuTV.git
cd NepuTV
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Project Structure

```
NepuTV/
├── .github/workflows/
│   └── build.yml              ← Free GitHub Actions CI
├── app/src/main/
│   ├── java/com/neputv/app/
│   │   └── MainActivity.java  ← All logic: WebView, D-pad, JS injection
│   ├── res/
│   │   ├── drawable/ic_launcher.xml
│   │   ├── values/strings.xml
│   │   ├── values/styles.xml
│   │   └── xml/network_security_config.xml
│   └── AndroidManifest.xml
├── app/build.gradle
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

---

## 🎮 Remote Control Mapping

| Remote Button | Action |
|---|---|
| **D-pad Up/Down/Left/Right** | Navigate between focusable elements |
| **OK / Center** | Click the focused element |
| **Back** | Go back in browser history |
| **Play/Pause** | Toggle video playback |

---

## ⚠️ Legal Notice

This app is a browser wrapper for publicly accessible content on nepu.to.
The user is responsible for ensuring compliance with local copyright laws
and the website's Terms of Service.
