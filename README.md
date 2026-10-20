# Yina

Yina is a calm, local-first flashcard app for Android that opens real Anki
collections. It uses **Anki's own Rust engine** — the same scheduling core that
Anki and AnkiDroid use — so imported cards keep their decks, templates, media
and review history, and answers are scheduled exactly the way Anki schedules
them.

Yina is not affiliated with Anki, AnkiDroid or AnkiWeb. The name "Anki" is used
only to describe file-format compatibility.

## Download

Signed APKs are attached to the [latest release](https://github.com/tutualican/yina/releases/latest).
Most phones need the `arm64-v8a` build; `armeabi-v7a` is for older 32-bit
devices, and `x86_64` is for emulators. Android 8.0 or newer is required.

## What works

- **Import `.apkg` and `.colpkg`** through Android's file picker. Media files
  are extracted into the collection's media folder.
- **Real deck tree and counts** from Anki's database, including review, learning
  and new counts.
- **Study sessions** with Anki's queue: question, reveal, then four answers
  (Again / Hard / Good / Easy). Each button shows the interval Anki itself
  computed.
- **Anki scheduling and persistence.** FSRS, deck limits, learning steps and
  review history are handled by Anki's backend and survive app restarts.
- **Undo** of the last answer.
- **12 UI languages**: English, Turkish, German, Spanish, French, Italian,
  Japanese, Korean, Polish, Portuguese, Russian and Simplified Chinese.
- **No account, no network, no analytics, no ads.** The app does not request the
  `INTERNET` permission.

## What is not here yet

This is a focused first release, not an AnkiDroid replacement:

- no sync, AnkiWeb login or account system;
- no card editor, browser, filtered decks or deck options;
- no audio playback or text-to-speech (`[sound:…]` markers are hidden for now);
- no LaTeX/MathJax rendering;
- only top-level decks are listed. Studying a parent deck includes its
  subdecks, which is how Anki works, but there is no separate nested-deck
  browser.

## How it works

The UI never reads the collection itself. Everything goes through one
interface, `CollectionEngine`:

```text
Compose screens
      │
      ▼
HomeViewModel / StudyViewModel
      │
      ▼
CollectionEngine
      │
      ▼
AnkiCollectionEngine
      │  protobuf over JNI
      ▼
anki-android-backend (Anki rslib + fsrs-rs)
      │
      ▼
collection.anki2 + collection.media
```

The engine is
[`io.github.david-allison:anki-android-backend`](https://github.com/ankidroid/Anki-Android),
AnkiDroid's published build of Anki's Rust backend. Yina calls its protobuf
`Backend` API directly: `openCollection`, `importAnkiPackage`,
`importCollectionPackage`, `deckTree`, `getQueuedCards`, `renderExistingCard`,
`describeNextStates`, `answerCard` and `undo`.

Card rendering returns a question/answer node tree plus the notetype CSS. Yina
assembles those nodes into a small HTML document and shows it in a WebView whose
base URL is the collection's media folder. Relative image sources therefore
resolve to the imported media files.

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build

Requirements:

- JDK 17 or newer (CI uses 21)
- Android SDK platform 36 and build-tools 36.0.0
- Android Studio is optional

```bash
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/

./gradlew :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`.

### Tests

`AnkiCollectionEngineTest` is an end-to-end host test. It runs Robolectric, loads
the desktop build of Anki's native backend through the `-testing` artifact, then
imports `media.apkg`, reads the deck tree, fetches and renders a card, answers it
with `GOOD`, and reopens the collection to verify persistence. A second test
exports a real `.colpkg` through the backend and imports it back. This is the
same code path the phone uses, minus the Android UI.

### Release signing

Release keys are never part of this repository. Create a local keystore:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-keystore.ps1
```

It writes `release.jks` and `keystore.properties`; both are gitignored. Back them
up: without the same key you cannot update an installed copy. Then:

```powershell
$env:VERSION_NAME = "0.2.0"
$env:VERSION_CODE = "2"
.\gradlew :app:assembleRelease
```

The build uses ABI splits and produces `app-arm64-v8a-release.apk`,
`app-armeabi-v7a-release.apk` and `app-x86_64-release.apk`. Most phones need the
first one. Upload the APK that matches the device to a GitHub Release.

The `Release` workflow is manual on purpose: by default the signing key stays on
the maintainer's machine. If you prefer CI releases, add the four signing secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) and run the
workflow from the Actions tab with a version name.

## Project layout

```text
app/src/main/java/io/github/yina/
  MainActivity.kt
  core/
    engine/        CollectionEngine + AnkiCollectionEngine
    model/         Deck and collection projections
  di/              Small manual dependency container
  ui/
    components/    Pressable surface, small shared pieces
    home/          Deck list and import
    study/         Review screen, WebView card, ViewModel
    theme/         Color, type, shape, motion
app/src/main/res/  Icons, themes, strings for 12 locales
app/src/test/      Robolectric + real-backend integration test
docs/              Architecture, design, privacy, roadmap
```

## Privacy

Everything stays on the device. There is no server component and no telemetry.
Android's own backup system may include app data if the user has enabled device
backup; that backup belongs to the user's Google account. See
[`docs/PRIVACY.md`](docs/PRIVACY.md).

## Credits and licence

Yina is licensed under **AGPL-3.0-or-later**. The Anki backend is AGPL-3.0, and
linking it requires the app to use the same licence.

- [Anki](https://github.com/ankitects/anki) — collection format, Rust backend,
  FSRS scheduler. Copyright Ankitects Pty Ltd and contributors.
- [AnkiDroid](https://github.com/ankidroid/Anki-Android) — Android packaging and
  the Maven backend artifacts used here. Copyright AnkiDroid contributors.

See [`NOTICE.md`](NOTICE.md) for the other bundled components. If you
redistribute Yina, keep the licence and attribution notices intact.
