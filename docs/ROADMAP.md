# Roadmap

## 0.2 — working core

- [x] Kotlin + Compose foundation, light/dark theme
- [x] Anki Rust backend wired through AnkiDroid's Android AAR
- [x] `.apkg` import and `.colpkg` collection-package import
- [x] Real deck tree, counts and media folder
- [x] Review screen with Anki-rendered question/answer HTML
- [x] Again / Hard / Good / Easy using Anki's scheduler and FSRS
- [x] Persistence, undo, completion state
- [x] End-to-end Robolectric test against the real backend
- [x] 12 UI locales and Android 13 per-app language metadata
- [x] GitHub Actions build, test, lint and release artifacts

## Next

- [ ] Audio playback and text-to-speech
- [ ] LaTeX/MathJax rendering
- [ ] Nested deck browser and deck options
- [ ] Card browser and editor
- [ ] Automatic backups and restore
- [ ] Import progress and cancellation
- [ ] Accessibility pass: TalkBack labels, large text, contrast
- [ ] More scheduler parity tests against desktop Anki

## Later

- [ ] Optional sync with a self-hosted server (Anki sync protocol)
- [ ] Statistics: retention, workload, heatmap
- [ ] Study planning and leech triage
- [ ] Optional, off-by-default AI-assisted card drafting

## Not planned

- iOS (not part of this project)
- a plugin marketplace
- social feeds or leaderboards
- a public deck marketplace
