> Historical migration notes. The migration is now active in all Android builds;
> see [current Heroku setup](heroku-mobile-cutover.md). The flag and Appwrite
> rollback paths described below have been removed.

# Android Render backend migration

The Android client and sibling `quran-backend` now implement the missing migration
features. Production activation remains gated on deployment configuration and an
operator-run migration; the shipping default remains disabled.

## Implemented paths

| Feature | Render path |
| --- | --- |
| Accounts and persistent sessions | `/auth/register`, `/auth/login`, `/auth/refresh` |
| Password recovery | `/auth/recovery`; emailed link opens backend `/auth/reset-password` form |
| Cloud progress | `/v1/sync`, WebSocket notifications, on-resume sync, fallback polling |
| Chapters | `/v1/chapters` |
| Chapter reader | `/v1/verses/:ch`, complete pagination, selected translation and Mushaf |
| Printed pages | `/v1/pages/:page`, selected translation and Mushaf |
| Word packs | `/v1/packs/words/:ch.json`, flat bundle strings normalized before decoding |
| Tafsir | `/v1/tafsir/:id/by_chapter/:ch`, reading and downloads |
| Translation footnotes | `/v1/footnotes/:id`, including persistent backend caching |
| Reciter manifest | `/v1/reciters`, persisted locally with built-in offline fallback |

Tajweed uses optional verse markup and the bundled fallback. Audio stays
CDN-direct as required by the backend contract. Prayer timings remain on
Aladhan. Appwrite code remains available for rollback; when Render is enabled,
account/sync operations and Quran content requests use the backend.

## Recovery and migration correctness

Recovery grants are hashed, expire in 30 minutes, and can be consumed once.
Migrated passwordless accounts require verified email recovery before setting
credentials; registration cannot claim them. Password setup preserves the
original Appwrite user id and reconnects its migrated sync document. Reset
invalidates previous refresh-token generations; access tokens expire within
their normal 15-minute lifetime.

The migration command supports a write-free `--dry-run`, requires users.read
before writes, preserves original document/user ids and source JSON/timestamps,
and conditionally skips newer destination watermarks. The readiness gate rejects
unmapped accounts and missing/invalid sync timestamps.

Client fixes cover first uploads against the empty-document canary, restoration
of legacy flattened state, owner mismatch, missing server timestamps, concurrent
sync and refresh, explicit word opt-out, chapter pagination, and credential-log
redaction. WebSocket authentication uses the supported Bearer header.

## Content and deployment bridge

The backend includes a portable seed archive with all 114 chapters, 6236 verses,
114 word packs, Ibn Kathir tafsir, and reciters. Its seed command works in a
checkout or container without the Android source tree. Additional translations
and layouts are imported from the fixed Quran.com source on demand and retained
in Mongo; footnotes also support a pre-import command. Failed/wrong-edition
responses are not cached.

`/_ready` checks baseline content, Mongo, JWT/email configuration, and account
mappings. `npm run verify:deployment -- https://actual-api-host` performs a
read-only release check. Render/Heroku configuration and the Docker image include
recovery settings and the seed tooling.

## Build and activate

Build Android with:

```sh
./gradlew :app:assembleDebug \
  -PquranNur.renderApiEnabled=true \
  -PquranNur.apiBaseUrl=https://actual-api-host
```

Set `PUBLIC_API_URL`, `RESEND_API_KEY`, and `RECOVERY_EMAIL_FROM` in the backend
deployment environment. The sender must be verified with Resend. Render's
`RENDER_EXTERNAL_URL` is the origin fallback. No real credentials are committed.

The configured default host returned HTTP 404 for `/_health` and `/v1/chapters`
on 2026-10-28. No deployment or production Appwrite/Mongo/email credentials are
configured in this session, so production activation, actual email delivery,
and the production data copy have not been performed.

Follow the [backend cutover instructions](../../quran-backend/docs/cutover.md)
for seeding, migration dry run/apply, readiness checks, real email testing, and
two-device verification before enabling the shipping default.

## Verification

- Android: 413 tests passed; Render-enabled debug APK built successfully.
- Backend: TypeScript build and all 128 tests passed against disposable MongoDB.
- Portable archive: successfully seeded a separate disposable database with all
  baseline content and zero anomalies.
- Applied backend: actual HTTP readiness/content deployment verifier passed on
  localhost. Test email delivery used fakes; no real emails were sent.

These local checks validate the implementation; they do not assert that the
production service or sender configuration is active.

Email provider: Resend Free for transactional delivery, with optional Testmail
inbox verification through the GitHub Student Developer Pack. See the
[email activation guide](../../quran-backend/docs/email-setup.md).
