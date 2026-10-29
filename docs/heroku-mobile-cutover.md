# Android uses Heroku

All debug and release builds now use the same API as the web app:
`https://qurannur-backend-d7d7312f118d.herokuapp.com`.
There is no backend-enable flag or Appwrite SDK/transport. The old
`quranNur.renderApiEnabled` property has no effect. For a different deployment,
set only `-PquranNur.apiBaseUrl=https://your-api.example`.

## Accounts and cross-device data

Sign in with the same account on Android and the web. Existing migrated accounts
retain their original ids and compatible passwords. Old Appwrite sessions do not
transfer: install the new APK and sign in once. Credentials are scoped to the API
origin. Device bookmarks, reading progress, downloaded packs, and preferences are
retained; changing accounts does not erase the device's local library.

Auth, recovery, sync, Quran metadata/content, translations, tafsir, word packs,
reciter metadata, and footnotes use Heroku. Local Room/assets remain available
offline. Recitation bytes still use the audio CDNs and prayer times use Aladhan.
The `quran-nur.appwrite.network` share links identify the web frontend's static
host, not a data API.

Sync uses the common `/v1/sync` `userId` + JSON `stateData` contract and server
watermarks. Sign-in/foregrounding restores cloud state. WebSocket announcements
pull changes; 60-second foreground reconciliation also uploads local edits even
when the server timestamp has not advanced. Backgrounding queues a connected-only
sync; WorkManager retains the periodic/retry path. Web detects mobile updates on
its next visible-tab poll (about 30 seconds) or when returning to the tab. Offline
changes upload after connectivity returns and sync next runs. Backup/Restore in
Settings can request an immediate pass.

Merge behavior is shared with the web: collections and progress are unioned,
settings use incoming values, unknown keys survive, and deletions are not currently
propagated. This is eventual convergence, not a transaction spanning both devices.
Each read validates its owner and each write needs a positive server timestamp.
Account changes during a read abort the operation; late auth refresh responses
cannot restore a logged-out session or clear a newer login.

The reader's existing Sauka assignment completion action now calls the real
Heroku endpoint and reports success only after the backend accepts it. Android's
current navigation does not contain the web's complete group-management UI.

## Build and verify

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
No operator credentials or database keys belong in the Android app.
Google sign-in and recovery delivery still depend on provider configuration;
see [Google setup](google-sign-in.md). Google is only displayed when enabled by
the backend.

## Cutover verification (2026-10-29)

The debug APK built successfully and all 421 JVM tests passed. The opt-in
`HerokuSyncIntegrationTest` ran against the deployed API with a disposable
`@example.test` account: mobile progress uploaded, web-shaped changes restored,
unknown web fields survived a mobile update, and a real 401 refreshed tokens and
replayed the request. The disposable account and its sync row were removed.
A compiled-DEX scan found the Heroku origin and no Appwrite SDK namespace, old
Appwrite API origin, old Render origin, or migration-enable flag.

The opt-in test reads `QURAN_NUR_LIVE_TEST_CONFIG`, the path to a private JSON file
with `apiUrl`, `email`, and `password` for an operator-created disposable account.
Use only a test account: this check writes progress. Remove that account after
the run. It skips during ordinary builds when the environment variable is unset.
No physical Android device was connected for installation or UI verification.
