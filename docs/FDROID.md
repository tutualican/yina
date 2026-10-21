# F-Droid release notes

Yina is prepared for the official F-Droid repository, but it has **not** been
submitted yet. The files in this directory are a draft for
[`fdroiddata`](https://gitlab.com/fdroid/fdroiddata).

## What is already prepared

- Application ID: `io.github.tutualican.yina`.
- Version: `0.4.0`, version code `400`.
- Fastlane metadata: `fastlane/metadata/android/en-US/` and `tr-TR/`.
- F-Droid build flag: `-PyinaUniversalApk` produces one universal APK instead of
  three ABI-specific APKs with the same version code.
- Draft metadata: `fdroid/io.github.tutualican.yina.yml`.

## Build the F-Droid APK

```powershell
$env:VERSION_NAME = "0.4.0"
$env:VERSION_CODE = "400"
.\gradlew :app:assembleRelease -PyinaUniversalApk
```

The result is `app/build/outputs/apk/release/app-release.apk`. F-Droid builds
and signs the app itself; do not upload this APK to F-Droid.

## Submit to fdroiddata

1. Create a GitLab account and fork
   `https://gitlab.com/fdroid/fdroiddata`.
2. Clone your fork.
3. Copy `fdroid/io.github.tutualican.yina.yml` into
   `metadata/io.github.tutualican.yina.yml` in the fork.
4. Install `fdroidserver` and run
   `fdroid lint io.github.tutualican.yina`, then
   `fdroid build io.github.tutualican.yina`.
5. Commit, push and open a merge request against `fdroiddata`.
6. In the merge request, state that you are the upstream author and that Yina
   is licensed under AGPL-3.0-or-later.

The release tag must exist before the merge request is reviewed. The metadata
uses `commit: v0.4.0` and `gradleprops: - yinaUniversalApk`.

## Remaining human steps

- Add real phone screenshots under
  `fastlane/metadata/android/en-US/images/phoneScreenshots/` and, optionally,
  `tr-TR/`. F-Droid treats screenshots as optional, but the listing is much
  better with them.
- Decide whether to keep GitHub Releases and F-Droid releases side by side.
  F-Droid normally signs with its own key. A phone cannot update from the
  GitHub-signed APK to the F-Droid-signed APK in place; the user must uninstall
  and reinstall, which removes local app data.
- Open the `fdroiddata` merge request. This needs a GitLab account; no GitHub
  API token is useful for that step.
- If the contributor graph on GitHub ever shows the old unlinked account again,
  GitHub Support must clear its cache. The repository history itself is clean
  and only contains `Yina maintainers <maintainers@yina.invalid>`.

## Licensing and dependency review

Yina is AGPL-3.0-or-later. The AnkiDroid backend AAR is AGPL-3.0-or-later and is
downloaded from Maven Central. F-Droid allows FLOSS dependencies from trusted
Maven repositories, but reviewers may ask about the prebuilt Rust `.so` files
and bundled JavaScript assets inside the AAR. Keep `NOTICE.md` and the upstream
source links available when answering those questions.
