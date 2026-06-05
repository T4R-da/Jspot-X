# Jspot-X

A lightweight, high-performance Spotify wrapper for Android that provides a premium-like experience without the restrictions.

## 🚀 Features

*   **No Forced Shuffle**: Uses a Tablet User-Agent to bypass mobile-only shuffle restrictions, allowing you to play any song in any order.
*   **Unlocked Controls**: Full support for the "Previous" track button, which is typically locked on free mobile accounts.
*   **Realistic Spotify Notification**: Integrated with **Android Media3**, providing a system-level notification with album art, play/pause, and skip controls that look and feel like the official app.
*   **Ad & Tracker Blocking**: Built-in aggressive ad-blocker that stops audio ads and tracking scripts, speeding up page loads and saving battery.
*   **Clean Interface**:
    *   Removed the redundant bottom player bar.
    *   Hidden "Upgrade to Premium" banners and sections.
    *   Pure black theme for OLED screens.
*   **Background Playback**: Optimized to keep music running even when the app is minimized or the screen is off.
*   **Self-Healing**: Automatically recovers and reloads if the web renderer encounters an error or crash.

## 🛠️ Technical Details

*   **Language**: Java
*   **UI Engine**: Android WebView (Hardware Accelerated)
*   **Media Stack**: AndroidX Media3 (Session + ExoPlayer sync)
*   **Min SDK**: 24 (Android 7.0)
*   **Target SDK**: 34 (Android 14)

## 📦 How to Build

1.  Clone the repository.
2.  Open the project in **Android Studio**.
3.  Ensure you have the Android SDK 34 installed.
4.  Build the project using `./gradlew assembleDebug`.
5.  The output APK will be named `Jspot-X.apk`.

## ⚠️ Disclaimer

This project is for educational purposes only. All Spotify branding and content are the property of Spotify AB. This app is a web wrapper and does not host or distribute any music files.
