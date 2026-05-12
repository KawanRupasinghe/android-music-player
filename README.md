# Smart Music Player Android App

A modern Android music player application developed using **Kotlin** and **Android Studio**.  
This project demonstrates the implementation of Android application fundamentals including:

- Foreground Services
- Broadcast Receivers
- Runtime Permissions
- Media Playback
- Notifications
- Background Processing

The application is capable of playing audio files from device storage while continuing playback even when the application is closed.

---

# Features

## Music Playback
- Load and play audio files from device storage
- Play music using Android `MediaPlayer`
- Stop music manually from the application

## Foreground Service
- Music playback continues even when the app is minimized or closed
- Persistent notification shown during playback

## Broadcast Receiver
- Listens for low battery system broadcasts
- Automatically stops music playback when battery level becomes critically low

## Runtime Permissions
- Supports Android runtime permission handling
- Compatible with both older and newer Android versions

## Modern Android Support
- Android 13+ media permissions
- Android foreground service support
- Notification permission support

---

# Technologies Used

- Kotlin
- Android Studio
- Android SDK
- MediaPlayer API
- Foreground Services
- Broadcast Receivers
- RecyclerView
- Material Design Components

---

# Project Structure

```text
app/
 ├── manifests/
 │    └── AndroidManifest.xml
 │
 ├── java/com/example/take_homeactivity/
 │    ├── MainActivity.kt
 │    ├── MusicService.kt
 │    ├── BatteryReceiver.kt
 │    ├── MusicAdapter.kt
 │    └── Song.kt
 │
 └── res/
      ├── layout/
      ├── drawable/
      ├── mipmap/
      └── values/
