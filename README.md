# MangaFire Android App

An Android WebView app for [MangaFire](https://mangafire.to/). The manga,
reader and account stuff still comes from the normal website.

## Features

- MangaFire reader and account support
- Opens MangaFire links from other apps
- Keeps the screen on while a chapter is open
- Saves the last chapter for the Continue reading button
- Fullscreen reader support
- Blocks unsafe links, popups, downloads and permission requests

The Continue reading button only saves the manga name, chapter and MangaFire
link on the phone. Clearing the app data also removes that shortcut.

## Building

You need JDK 17, Android SDK 36 and Android 8 or newer.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The APK is created in:

```text
app/build/outputs/apk/debug/app-debug.apk
```
