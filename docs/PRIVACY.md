# Privacy

Yina is a local application. It has no server, no account system, no analytics
and no advertising SDKs. The Android manifest deliberately does not request the
`INTERNET` permission.

## What the app stores

On the device only:

- decks, notes, cards and media imported from `.apkg` / `.colpkg` files;
- review history and scheduling data written by Anki's engine;
- app preferences.

The collection lives in the app's private storage:

```text
filesDir/collection.anki2
filesDir/collection.media/
filesDir/collection.media.db
```

Other apps cannot read this directory on a normal Android device.

## What leaves the device

Nothing. Yina does not send collection data, diagnostics or usage information
anywhere. There is no sync feature in this release.

The file picker is provided by Android. The app receives temporary read access
only to the package the user chooses, copies it into its own cache for the
duration of the import, and deletes the copy afterwards.

## Backup

Android's system backup may include app data if the user has enabled device
backup in their Google account settings. That backup is controlled by the user
and by Google's terms; the Yina project has no access to it. Users who do not
want this can disable backup for the device or for Yina in Android settings.

## Removing data

Clearing Yina's storage or uninstalling the app removes the local collection.
Imported packages are not modified.

## Children and sensitive data

The app has no age gate and no behavioural profiling. It does not request
location, contacts, calendar, camera, microphone or phone permissions. A deck's
content is whatever the user imported; Yina does not inspect or upload it.

## Contact

There is no dedicated privacy mailbox. Issues about privacy can be raised in the
project's public issue tracker.
