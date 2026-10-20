# Third-party notices

Yina is licensed under AGPL-3.0-or-later (see `LICENSE`). It builds on the
following projects. Their licences apply to their code and assets.

| Component | Licence | Upstream |
|---|---|---|
| Anki (rslib, fsrs-rs, templates, web assets) | AGPL-3.0-or-later | https://github.com/ankitects/anki |
| AnkiDroid Android backend AAR | AGPL-3.0-or-later | https://github.com/ankidroid/Anki-Android |
| MathJax | Apache-2.0 | https://github.com/mathjax/MathJax |
| jQuery | MIT | https://jquery.com/ |
| AndroidX / Jetpack Compose / Material 3 | Apache-2.0 | https://developer.android.com/jetpack |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | https://kotlinlang.org/ |
| Protocol Buffers | BSD-3-Clause | https://github.com/protocolbuffers/protobuf |

The Android APK bundles Anki's rendered template assets and MathJax fonts
through the AnkiDroid backend artifact. If you redistribute a modified build,
keep this notice and the upstream licence texts with it.

Robolectric and JUnit are used only for host tests and are not shipped in the
APK; they are Apache-2.0 and EPL-2.0 respectively.
