# Architecture

## Principle

Do not reimplement Anki. Reuse its engine and put a calmer interface on top.

Yina talks to Anki's Rust core (`rslib`, including `fsrs-rs`) through AnkiDroid's
published Android backend AAR. The collection format, schema upgrades, template
rendering and scheduling are therefore Anki's own code, not a partial clone of
it.

## Layers

```text
Compose screens
      │
      ▼
HomeViewModel / StudyViewModel
      │
      ▼
CollectionEngine                    (core/engine/CollectionEngine.kt)
      │
      ▼
AnkiCollectionEngine                (core/engine/AnkiCollectionEngine.kt)
      │  generated protobuf methods over JNI
      ▼
net.ankiweb.rsdroid.Backend
      │
      ▼
librsdroid.so + SQLite
      │
      ▼
collection.anki2, collection.media, collection.media.db
```

`CollectionEngine` is the only boundary the UI knows:

```kotlin
interface CollectionEngine {
    suspend fun open(): CollectionSnapshot
    suspend fun importPackage(uri: Uri, displayName: String?): ImportResult
    suspend fun startSession(deckId: Long): SessionInfo
    suspend fun nextCard(): StudyCard?
    suspend fun answer(rating: StudyRating, millisecondsTaken: Long)
    suspend fun undo(): Boolean
    suspend fun close()
}
```

Everything else in the app depends on the models in `core/model` and
`core/engine/StudyModels.kt`, not on backend classes.

## Backend setup

The backend comes from
`io.github.david-allison:anki-android-backend:0.1.71-anki26.09.3`, built by the
AnkiDroid project from Anki 26.09.3. It contains the native libraries for
`arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`.

On Android the engine:

1. points `TMPDIR` at the app cache directory with `android.system.Os.setenv`
   (SQLite and the Rust core need a writable temp directory);
2. sets `PLATFORM=android`;
3. calls `System.loadLibrary("rsdroid")`;
4. creates one `Backend` with the system language plus English;
5. opens `filesDir/collection.anki2` with the media folder and media database
   beside it.

All calls are serialized behind a `Mutex` and run on `Dispatchers.IO`. The
backend object owns one open collection, and the queue is stateful, so
concurrent calls would corrupt the session.

On the host test, `Build.FINGERPRINT == "robolectric"` skips the Android library
load. The test calls `RustBackendLoader.ensureSetup()` from the backend's
`-testing` artifact, which extracts the desktop native library.

## Import

The Storage Access Framework gives Yina a `content://` URI. The engine copies it
to a temporary file, then chooses a path based on the filename:

- **`.apkg`** — `importAnkiPackage(path, presets)`. This adds decks, notes,
  cards and media to the current collection.
- **`.colpkg`** — close the open collection, call
  `importCollectionPackage(collectionPath, packagePath, mediaFolder, mediaDb)`,
  then reopen. A collection package replaces the collection by design, matching
  Anki's behaviour.

The temporary file is removed in `finally`. The deck tree is read again after
import so the home screen shows real counts.

## Review

`startSession(deckId)` selects the deck with `setCurrentDeck`. `nextCard()`:

1. calls `getQueuedCards(1, false)`;
2. takes the single queued card and remembers its `SchedulingStates`;
3. calls `renderExistingCard(cardId, browser = false, partialRender = false)`;
4. builds the question and answer documents from the returned node tree and CSS;
5. calls `describeNextStates(states)` for the four interval labels;
6. returns a `StudyCard`.

`answer()` builds an Anki `CardAnswer` containing the current state, the state
for the chosen rating, the rating, the answer timestamp and the time spent on the
card in milliseconds. Anki then applies its normal scheduler, including FSRS and
learning steps. `undo()` calls the backend's undo stack.

The WebView loads the document with a `file://` base URL pointing at
`collection.media/`. That is why imported images work without a local web server.
Anki's desktop webview uses `/_media/` paths; the engine strips that prefix so
relative paths resolve. `[sound:…]` markers are removed because there is no
audio pipeline yet.

## Data layout

```text
filesDir/
  collection.anki2
  collection.anki2-wal
  collection.anki2-shm
  collection.media/
  collection.media.db
```

The engine is the only code that opens these files. There is no network access
and no telemetry.

## Testing

`app/src/test/java/io/github/yina/core/engine/AnkiCollectionEngineTest.kt` is a
Robolectric test against the real host backend. It imports a sample package,
checks the deck projection, renders a card, answers it and verifies that the
collection still opens with the answer persisted. This catches API mismatches,
schema issues and JNI/protobuf mistakes without an emulator.

`@ConscryptMode(OFF)` is required on a Turkish host locale: Conscrypt 2.5.2
lowercases `"windows"` without a fixed locale, producing `"wındows"` and failing
to load its native library. Robolectric's BouncyCastle fallback is fine for these
tests.

## Constraints

- ABI splits are used because the native backend is large.
- R8/minification is disabled for release: the backend is reached through
  generated protobuf classes and JNI, and shrinking risks removing something the
  bridge needs for little benefit next to the native libraries.
- `minSdk` is 26. The backend AAR targets modern Android; below that the native
  bridge and WebView behaviour are not tested.
