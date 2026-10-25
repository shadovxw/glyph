# Glyph — Android Launcher Project: Setup & Workflow Guide

This file tells Claude Code how to set up, build and deploy **Glyph**, a personal Android launcher for my phone. It includes home-screen widgets, a stock-style app drawer, a custom page transition, a live wallpaper and Quick Settings tiles.

**What to build is defined in [`DESIGN.md`](DESIGN.md).** Read it fully before writing any UI or logic, and follow it over any defaults here. Visual reference: https://claude.ai/artifact/CMCQHVKnUxp2z65xsp8yW3 (private; `DESIGN.md` holds every value if you can't open it).

## Target environment

- **Dev machine:** Lenovo Legion 5 laptop (check the OS with `uname -a` / `ver` before running OS-specific commands)
- **Target device:** OnePlus 12R (Android, OxygenOS)
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3 only as a base; Glyph styles everything itself)
- **Build system:** Gradle with Kotlin DSL (`*.gradle.kts`)
- **Distribution:** sideloaded APK only. No Play Store.

## Architecture (important)

- Glyph is a **launcher app** (`HOME` intent), and the home screens are a Compose `HorizontalPager` with 5 pages.
- **Home widgets are Compose components inside the launcher, not Android app widgets.** This is what makes the transparent cards, page transition and blur possible. Only use Glance if I later want a widget on another launcher.
- **Two backgrounds:**
  - **Home screen:** the launcher draws **Orbit and dots** itself (`DESIGN.md` §5A) on a Compose `Canvas` behind the widgets. The launcher window is **opaque** (it does not show the wallpaper).
  - **Lock screen:** **Bloom and wind** is a `WallpaperService` live wallpaper in the same app (`DESIGN.md` §5B). Because the launcher is opaque, it's only visible on the lock screen and pauses itself on the home screen.
- **Quick Settings tiles** are `TileService`s in the same app.
- The **app drawer** must copy the stock OxygenOS drawer exactly (see `DESIGN.md` §6).

---

## 1. One-time environment setup

Check each item first, and only install what is missing. Ask me before installing anything system-wide.

1. **JDK 17+**: check with `java -version`. Android Studio bundles a JBR; prefer pointing `JAVA_HOME` at it.
2. **Android Studio** (latest stable): download from developer.android.com/studio. If I already have it, just locate the SDK path.
3. **Android SDK components** (via SDK Manager or `sdkmanager`):
   - Latest stable SDK Platform (API 35+)
   - Android SDK Build-Tools (latest)
   - Android SDK Platform-Tools (provides `adb`)
4. **`adb` on PATH**: verify with `adb version`.
5. Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to the SDK path, and write `sdk.dir` in `local.properties`. Never commit `local.properties`.

### Phone setup (I do these by hand; just remind me)

1. Settings → About device → tap **Build number** 7 times to enable Developer options.
2. Developer options → enable **USB debugging** (and optionally **Wireless debugging**).
3. Connect via USB and accept the RSA prompt on the phone.
4. Verify: `adb devices` should list the phone as `device`, not `unauthorized`.

**Wireless option:** in Wireless debugging, choose "Pair device with pairing code", then run:
```
adb pair <ip>:<pair-port>
adb connect <ip>:<port>
```

After installing Glyph, remind me to:
- set Glyph as the default Home app (Settings → Apps → Default apps → Home app)
- set Bloom and wind as the live wallpaper (choose "Lock screen" if OxygenOS offers it, otherwise both)
- grant notification access, usage access, Health Connect and the other permissions in `DESIGN.md` §8
- set Glyph's battery usage to **Don't optimize / Allow background activity**, because OxygenOS aggressively kills background work and updates will stall otherwise.

---

## 2. Project scaffold

```
<project-root>/
├── CLAUDE.md
├── DESIGN.md                    # source of truth for the design
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml    # version catalog
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/<package>/
    │   ├── launcher/            # HomeActivity, pager, dock, page indicator, transition
    │   ├── screens/             # Today, Music, WhatsApp, GitHub, World
    │   ├── drawer/              # OxygenOS-style app drawer
    │   ├── wallpaper/           # BloomWallpaperService + renderer
    │   ├── tiles/               # TileService classes
    │   ├── data/                # repositories: media, notifications, weather, github…
    │   ├── services/            # NotificationListener, workers
    │   └── theme/               # tokens from DESIGN.md §2 (colours, type, spacing)
    └── res/
        ├── font/                # Geist, Geist Mono
        ├── drawable/            # lotus asset, icons
        └── xml/                 # wallpaper metadata
```

### Versions

Use the **latest stable** versions of AGP, Kotlin, the Compose BOM and the other libraries. Check them before pinning; don't trust remembered numbers. Keep them in `gradle/libs.versions.toml`.

Baseline config:
- `compileSdk` / `targetSdk`: latest stable (35+)
- `minSdk`: 31 (needed for `RenderEffect` blur and modern Bluetooth APIs)
- Kotlin Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) for Kotlin 2.x

Add libraries only when a step needs them (WorkManager, OkHttp/Ktor, kotlinx-serialization, CameraX + ML Kit barcode, Health Connect client, DataStore).

---

## 3. Required manifest pieces

```xml
<!-- Launcher -->
<activity android:name=".launcher.HomeActivity" android:exported="true"
    android:launchMode="singleTask" android:excludeFromRecents="true"
    android:theme="@style/Theme.Glyph.Wallpaper">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- Live wallpaper -->
<service android:name=".wallpaper.BloomWallpaperService" android:exported="true"
    android:permission="android.permission.BIND_WALLPAPER">
    <intent-filter><action android:name="android.service.wallpaper.WallpaperService" /></intent-filter>
    <meta-data android:name="android.service.wallpaper" android:resource="@xml/bloom_wallpaper" />
</service>

<!-- Notification listener (WhatsApp, orders, media sessions) -->
<service android:name=".services.GlyphNotificationListener" android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter><action android:name="android.service.notification.NotificationListenerService" /></intent-filter>
</service>

<!-- One TileService per tile in DESIGN.md §7 -->
```

The launcher theme is **opaque** (`android:windowShowWallpaper="false"`, solid window background matching the mode), because the home screen draws its own Orbit and dots background and the live wallpaper is meant for the lock screen only. Package visibility: the drawer needs `<queries>` for `MAIN/LAUNCHER` intents to list installed apps.

---

## 4. Implementation notes

- **Theme tokens first:** put every colour, size and font from `DESIGN.md` §2 into `theme/` and use only those.
- **Page transition (`DESIGN.md` §4):** compute each page's offset from `pagerState.currentPage + currentPageOffsetFraction`. Apply page alpha and blur with `graphicsLayer`, apply the card cascade per card, and feed the same position to the home background so the dots shift and the rings rotate with the swipe.
- **Home background (`DESIGN.md` §5A):** a Compose `Canvas` driven by `rememberInfiniteTransition` (dot pan, ring rotation, breathing ring), paused when the activity stops.
- **Lock screen wallpaper (`DESIGN.md` §5B):** draw on a `Canvas` in the engine, cap at 30 fps with `Choreographer`, stop in `onVisibilityChanged(false)`, seed particle positions once, read light/dark from the shared DataStore setting, and override `onComputeColors()` with neutral greys only.
- **App drawer (`DESIGN.md` §6):** load apps with `LauncherApps`/`PackageManager` and show their real icons unmodified, except a configurable custom-icon list.
- **Media (YouTube Music):** use `MediaSessionManager.getActiveSessions(listenerComponent)` and a `MediaController.Callback`; like/dislike are custom actions (`sendCustomAction`). Desaturate album art with a `ColorMatrix`.
- **Secrets:** GitHub tokens and any API keys go in `local.properties`, read via `BuildConfig`. Never commit them.

---

## 5. Everyday commands

Run from the project root (`gradlew.bat` on Windows, `./gradlew` on Linux/macOS):

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Build + install on phone | `./gradlew installDebug` |
| Uninstall | `./gradlew uninstallDebug` |
| Lint | `./gradlew lint` |
| Unit tests | `./gradlew test` |
| Clean | `./gradlew clean` |
| Live logs (filtered) | `adb logcat -s Glyph` (use `Log.d("Glyph", ...)`) |
| Crash logs | `adb logcat *:E` |
| Open Home picker | `adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME` |

---

## 6. Workflow for Claude Code

Build in this order, and run `./gradlew assembleDebug` after each step, fixing all errors before moving on:

1. Theme tokens and fonts.
2. Launcher shell: `HomeActivity`, the 5-page pager, dock, page indicator (`DESIGN.md` §2.6), light/dark mode switch, and the Orbit and dots home background.
3. The Glyph blend transition, including background parallax.
4. All five screens with **sample data** matching `DESIGN.md`.
5. App drawer (swipe up), OxygenOS-identical.
6. Lock screen live wallpaper (Bloom and wind) with light/dark and system colours.
7. Real data sources, one at a time (`DESIGN.md` §8).
8. Quick Settings tiles.

Rules:
- If `DESIGN.md` is unclear on something that blocks the build, ask me. Don't guess.
- Check the environment (section 1) and report what's missing before installing anything.
- If the phone is connected (`adb devices`), run `./gradlew installDebug` after each step and tell me what to check.
- Keep a short changelog of what was done in `NOTES.md`.
- Package name: suggested `me.shadovxw.glyph`; confirm with me once, then reuse it.
- Use small, focused files: one screen per file, with data/repository code separated from UI.
- Don't add libraries beyond what a step needs.
