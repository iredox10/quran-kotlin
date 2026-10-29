# Continue with Google (Android)

The backend uses Google ID token verification and the Android app uses Credential
Manager's explicit account chooser. The app fetches provider availability and the
Web client ID from the backend, so configuring Google does not require another
APK build. All current builds use Heroku; Google sign-in is shown only when the backend
reports it is configured.

## One-time Google setup

1. Open https://console.cloud.google.com/auth/overview and create/select a project.
2. Configure Google Auth Platform branding (Quran Nur), support/contact email,
   audience, and the basic `openid`, `email`, and `profile` scopes. During testing,
   add the Google accounts that will test the app to the audience's test users.
3. Create an OAuth client with application type **Web application**. Copy its
   client ID into `GOOGLE_CLIENT_ID` in `../quran-backend/.env`. This native ID
   token flow does not use a redirect URI or a client secret.
4. In that SAME Google project, create an **Android** OAuth client:
   - Package: `com.nur.quran`
   - Local debug signing SHA-1:
     `38:A5:C0:61:99:EB:6C:1B:1C:4B:D8:56:05:CC:85:DC:FC:CB:2C:90`
   - This fingerprint identifies this workstation's debug keystore. Other
     workstations need their own Android clients. For release/Play distribution,
     register the actual release/Google Play app-signing SHA-1 separately.
5. Set the same `GOOGLE_CLIENT_ID` as a Heroku config var. Open/reopen Profile in
   the new APK: expand Cloud Sync and select **Continue with Google**.

The Web client ID is the token audience used by the backend. Do not put the
Android OAuth client ID into `GOOGLE_CLIENT_ID`. No Google client secret,
Firebase project, Drive access, contacts access, or paid provider is required.
Google may require consent/brand verification before broad public distribution;
follow the prompts for the configured audience.

## Build

```sh
./gradlew --no-daemon \
  -Dorg.gradle.java.home=/home/iredox/Downloads/android-studio/jbr \
  :app:assembleDebug :app:testDebugUnitTest
```

The backend URL is the deployed Heroku API in every build.
`quranNur.apiBaseUrl` can override the HTTPS host; there is no enable flag.

## UI reference lock

| Decision | Reference | Application |
|---|---|---|
| Placement and existing visual language | Existing `ProfileCloudSyncCard` | Preserve the card, typography, email form, and alerts; place Google first in sign-in/register mode. |
| Button branding | Google Identity branding guidelines and official assets | White outlined 48dp touch target, official multicolor G, “Continue with Google”; no logo recoloring. |
| Consent/chooser | Android Credential Manager guidance | Explicit button opens Google's chooser; cancellation returns to the card without an error. |
| Loading and keyboard access | Refero bundled craft-details guidance | Disable duplicate submissions while busy and preserve a labeled, accessible button. |

The icon is the unmodified official 4x PNG from
https://developers.google.com/static/identity/images/signin-assets.zip. Compose
clips the surrounding square button chrome to display its central G on our
custom button. No generated or redrawn brand logo is used.

## Behavior and verification limits

Migrate existing Appwrite identities/sync documents before enabling Google for
those users. An empty backend cannot infer their original Appwrite ids from
email addresses.

The backend creates an account automatically or links a verified Google identity
to an existing trusted Gmail/Workspace email, preserving the original backend
user id and cloud document. A Google subject already linked to another account
cannot claim a matching email. Google accounts using third-party email addresses
need fresh email ownership verification before initial signup/linking, so the UI
explains how to use email sign-in/recovery. Returning linked accounts resolve by
Google's immutable subject even if the email address changes.

Automated tests verify signed token rejection, nonce replay/expiry, concurrent
account creation, linking, and token persistence. Real device consent/account
selection still requires the Google project setup and a device with Google Play
services. Account chooser screenshots cannot be validated without that setup.

Sources: [Android implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation),
[server verification](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token),
[Google branding](https://developers.google.com/identity/branding-guidelines).
