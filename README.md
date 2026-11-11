# Inner Lotus

An offline healing and self-care app for Android, by [sjpixels](https://sjpixels.com/).
It covers affirmations, EFT tapping, sound healing, Ho'oponopono, journaling, a habit tracker, breathwork and a chakra guide, in 9 languages.

- **Package:** `com.sjpixels.innerlotus`
- **Stack:** Kotlin 2.2 · Jetpack Compose (BOM 2025.12) · Material 3 · AGP 8.13 · Gradle 8.14
- **SDK:** compileSdk / targetSdk 36 · minSdk 26 (Android 8.0+)
- **Privacy:** no internet permission, servers, accounts, ads or analytics. Everything stays on the device.

---

## Features

| Section | What it does |
|---|---|
| **Affirmations** | 33 life areas × 1008 unique affirmations each (33,264 per language). Swipe cards, chakra picker (Root → Crown, "I AM … I KNOW"), or all 7 chakras at random. Favourites, copy, share as an image, save to Pictures/InnerLotus. Read aloud with the phone's own TTS in 4 modes: tap, auto-play with auto-swipe, repeat, or ×N count. Bulk edit your own lines per area. Import/export as JSON, CSV or TXT, for the whole library or one section. |
| **Learn** | Research-backed explainers for affirmations, EFT, Ho'oponopono, journaling, sound, breath and chakras, each with a Listen button. |
| **EFT Tapping** | 12 guided topics × 4 techniques (Basic, Full with 9 Gamut, Gentle touch & breathe, Positive round). Includes a SUD rating before and after, a drawn tapping-point figure, and optional voice guidance. |
| **Sound Healing** | 17 frequencies (Solfeggio, 432 Hz, OM 136.1 Hz, Schumann and binaural delta to gamma) plus 22 nature sounds, instruments and noise textures. All of them are synthesised live, with no audio files. Mix any number of layers at their own volumes, save mixes, use 8 presets, play an endless autoplay "journey", set a sleep timer, and keep playing in the background with a notification. |
| **Ho'oponopono** | 15 topics, one of them a specific person you name. The prayer is editable and can be read once, autoplayed in a continuous loop, or repeated a set number of times. |
| **Journal** | Free, Gratitude and Prompt modes (30 prompts), moods, and grouping by day, week or month. Includes search, a 30-day mood summary, JSON/TXT export, import, delete by entry, group or all, and an optional PIN lock (salted SHA-256). |
| **Habit Tracker** | Manual habits with 20 suggestions, emoji, colour, times per day and weekdays. Shows streaks, a 12-week heatmap, a weekday rhythm and per-habit stats. **App usage** shows screen time for today, 7 days and 30 days from Android's UsageStatsManager, after the user grants Usage access. |
| **Breathwork** | Box, 4-7-8, Coherent, Cyclic sighing, Nadi Shodhana, Bhramari and Calming breath. An animated circle guides each one, with optional spoken cues. |
| **Chakra Guide** | Each chakra's location, element, bija mantra, themes, balanced and imbalanced signs and practices, with its tone. |
| **Calm now (SOS)** | Breathing circle, 5-4-3-2-1 grounding, tapping for anxiety, a calm-rain mix and crisis helplines (Tele-MANAS 14416, findahelpline.com). |
| **Mantra chanting** | Links to the separate [Mantra Mala](https://play.google.com/store/apps/details?id=com.sjpixels.mantramala) app. This app intentionally has no mantra counter. |
| **Everywhere** | Flag + language dropdown in the top-right corner. "Developed by https://sjpixels.com/" footer on every page. Background either rotates every N seconds (default 15) through 18 images or stays fixed. Daily reminders. Masculine/feminine form of address for gendered languages. |

**Languages:** English, Hindi, Russian, Korean, Chinese (Simplified), Spanish, French, Italian, Japanese.

---

## Open, build and run

1. Install a current **Android Studio** (Narwhal 2025.1.3 or newer). It bundles a JDK 17+ and the Android SDK.
2. **File → Open…** and choose this folder. Let Gradle sync; it downloads Gradle 8.14.3 and all dependencies.
3. Pick a device or emulator (Android 8.0+) and press **Run**. The debug build installs as `com.sjpixels.innerlotus.debug` alongside any release build.

From a terminal:

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew bundleRelease          # app/build/outputs/bundle/release/app-release.aab (for Play)
```

> **First build note.** This project was written in an environment without the Android SDK. The pure-Kotlin parts (content composer, stores, analytics, CSV, all 39 synthesised sounds) compile and pass 105 JVM tests. Every source file passes a full Kotlin syntax check. The Android/Compose layer has not yet been compiled against the real SDK. If the first sync reports a small API mismatch, apply Studio's quick-fix or the suggested replacement; the code uses only stable APIs from the pinned versions.

### Release signing

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/innerlotus-release.jks
storePassword=••••••
keyAlias=innerlotus
keyPassword=••••••
```

To create a keystore, use **Build → Generate Signed App Bundle** in Studio, or run:

```bash
keytool -genkeypair -v -keystore innerlotus-release.jks -alias innerlotus -keyalg RSA -keysize 4096 -validity 10000
```

Use **Play App Signing** in Play Console. Your keystore then serves as the *upload* key, and it must be backed up safely. Bump `versionCode` and `versionName` in `app/build.gradle.kts` for every upload.

---

## Project structure

```
app/src/main/
  AndroidManifest.xml
  assets/i18n/<lang>.json          ← generated by tools/build_content.py (don't edit by hand)
  res/drawable-nodpi/bg_*.webp     ← 18 backgrounds (from theme-bg.zip)
  res/font/                        ← Cormorant Garamond, Dancing Script (OFL)
  java/com/sjpixels/innerlotus/
    App.kt                         ← Application, Screen routes, Navigator, singletons
    MainActivity.kt                ← root composable, onboarding, back handling
    core/      Lang, I18n, StringTable (gender markup), Catalog (ids, colours), AppSettings
    content/   Composer (1008-card maths), Affirm (cards), Practices (breath patterns, prayer)
    data/      JSON stores: affirmations, journal, habits, mixes; analytics; CSV; backup
    audio/dsp/ real-time synthesis: filters, noise, modal bells/bowls, Karplus-Strong, reverb, mixer
    audio/     SoundEngine (AudioTrack thread), SoundService (foreground, mediaPlayback)
    tts/       Speaker (Android TextToSpeech wrapper, chunking, language switching)
    usage/     UsageRepo (UsageStatsManager)
    share/     card image rendering, share, save to gallery, clipboard, SAF files
    reminders/ daily notifications (AlarmManager inexact), boot receiver
    ui/        theme, components (scaffold, footer, language menu, charts), screens/*
content/src/<lang>/*.txt           ← all translatable text (source of truth)
tools/build_content.py             ← validates + builds the JSON
tools/expand_gender.py             ← expands Hindi shorthand करता{|ी} → {करता|करती}
tools/jvmtest/                     ← JVM tests for the Android-free code
tools/store/                       ← renders the Play icon + feature graphic
docs/                              ← Play Console guide, privacy policy, store graphics, licences
```

---

## Editing content and translations

All text lives in `content/src/<lang>/` as plain text files: `areas.txt`, `chakras.txt`, `learn.txt`, `eft.txt`, `practices.txt`, `ui.txt`.

```text
key = single line value
key:
- list item
- list item
key <<
multi-line block
>>
# comment
```

After editing, run:

```bash
python3 tools/build_content.py           # validate + write app/src/main/assets/i18n/*.json
python3 tools/build_content.py --check   # validate only
```

The validator checks every language against English: same keys, same list lengths, same `{placeholders}`, and well-formed gender markup.

- **Affirmations** are composed as 24 area lines × 6 chakra seed lines × 7 chakras = 1008 per area. Keep each area's 24 lines unique, and keep them unique across areas too; the tests check this.
- **Gender:** in Hindi, Russian, Spanish, French and Italian, write `{masculine|feminine}` with whole words, e.g. `{grato|grata}` or `{करता|करती}`. The app resolves it from **Settings → Speak to me as** (feminine by default; also asked during onboarding).
- **Adding a language:** add an entry to `core/Lang.kt` (code, flag, native name, BCP-47 tag, gendered?, Latin script?), add the code to `LANGS` in `tools/build_content.py`, create `content/src/<code>/` by copying `en/`, translate it, and build.
- **Adding an area:** add the id, emoji and background to `Catalog.areas`, then add `area.<id>.name/.desc/.lines` (24 lines) in every language.

---

## Tests

```bash
tools/jvmtest/run.sh            # needs a Kotlin compiler + kotlinx-serialization/coroutines jars (see script header)
```

These cover:

- Deck maths: 144 unique cards per chakra and 1008 per area.
- Real content in all 9 languages and both address forms: 33 × 1008 unique cards with no stray markup.
- Gender and placeholder resolution, the prayer builder, all data stores (round-trip, import, merge, delete), streaks and analytics, and CSV.
- All 39 sounds, rendered to WAV with checks for level, NaN and real-time speed.

In Android Studio you can also move `PureTests.kt` into `app/src/test/` as a normal unit test.

---

## Permissions (and why)

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Optional App-usage screen. The user turns it on in system settings after an in-app explanation. Data is read on the device only. |
| `POST_NOTIFICATIONS` | Daily affirmation and journaling reminders (asked only when the user enables one). |
| `RECEIVE_BOOT_COMPLETED` | Re-schedules reminders after a reboot. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | Keeps user-started healing sounds playing with the screen off, with a Stop button in the notification. |
| `WRITE_EXTERNAL_STORAGE` (max SDK 28) | Saves card images on Android 8–9 only. |

There is **no** `INTERNET` permission. Backups are disabled (`allowBackup=false` with data-extraction rules excluding everything), so journal data never leaves the phone unless the user exports it.

---

## Publishing

See **[docs/PLAY_STORE.md](docs/PLAY_STORE.md)** for the store listing in 9 languages, Data safety answers, Health apps declaration, foreground-service declaration, content rating and release checklist. Host **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)** (or `docs/privacy-policy.html`) on sjpixels.com and paste its URL into Play Console.

---

## Credits and licences

- Code, content, chakra artwork and synthesised sounds: © sjpixels. All rights reserved.
- Fonts: Cormorant Garamond and Dancing Script under the SIL Open Font License 1.1 (`docs/licenses/`).
- Background photos come from the supplied `theme-bg.zip`. Make sure you hold the rights to publish them.
- The wellness content is educational and is not medical advice; see the in-app disclaimer.
