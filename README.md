# MangaFire for Android

A private, sideloaded Android WebView app for `https://mangafire.to/`. It keeps
MangaFire's catalog, search, account, preferences, title pages, chapter reader,
and reader controls under the website's control. It does not scrape, download,
proxy, or rehost manga pages.

## Build and install

Requirements: JDK 17, Android SDK Platform 36, and Android 8.0 (API 26) or newer.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## Behavior

- Exact-host HTTPS navigation to `mangafire.to` stays in the WebView.
- Secure CDN chapter images load as ordinary subresources.
- User-initiated external HTTPS links open outside the trusted WebView.
- Popups, downloads, insecure URLs, custom schemes, file access, uploads,
  location, camera, and microphone access are blocked.
- JavaScript, DOM storage, and persistent cookies support the site's account,
  preferences, reading progress, and reader.
- Android Back exits reader fullscreen first, then navigates WebView history.
- Fullscreen reader mode rotates to landscape and returns to portrait on exit.
- Clear browsing data removes cookies, site storage, cache, and WebView history.

The remote site and CDN can change independently, so live reading remains a
manual smoke test. The safe fallback is opening the current page externally.
