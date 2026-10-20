# Contributing

Thanks for taking a look. Small, focused changes are easiest to review and
merge.

## Development setup

- JDK 17 or newer (CI uses 21)
- Android SDK 36 and build-tools 36.0.0
- Android Studio is optional

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`.

The host tests use Robolectric and the desktop build of Anki's native backend.
They do not need an emulator. If you run them on a Turkish-locale machine, keep
`@ConscryptMode(OFF)` on the test class; see `docs/ARCHITECTURE.md`.

## Rules

- **Language:** code and commit messages in English. User-visible text belongs
  in `strings.xml`.
- **Formatting:** official Kotlin style, 4-space indentation.
- **Architecture:** UI code does not reach around `CollectionEngine`, and no code
  outside `core.engine` opens the collection file.
- **Dependencies:** explain the licence (AGPL compatibility) and size impact in
  the pull request.
- **Commits:** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.

## Pull request checklist

- [ ] CI is green
- [ ] Behaviour changes have a test
- [ ] UI changes include a screenshot
- [ ] Import/scheduler changes note compatibility with real `.apkg` files
- [ ] No keystore, password, personal path or private collection is committed

## Translations

Copy `app/src/main/res/values/strings.xml` into the right `values-<language>`
folder and translate every key except `app_name`. Missing keys fall back to
English. Keep plurals correct for the target language and escape apostrophes in
Android XML (`\'`).

The app currently ships: English, Turkish, German, Spanish, French, Italian,
Japanese, Korean, Polish, Portuguese, Russian and Simplified Chinese.

## Licence

Contributions are accepted under AGPL-3.0-or-later. New source files should start
with:

```
// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Yina contributors
```

## Code of conduct

Be decent to each other. This project follows the
[Contributor Covenant v2.1](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).
