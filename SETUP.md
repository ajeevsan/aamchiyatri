# Setup: going from fakes to real services

The app now defaults to **real** integrations (Firebase Auth/Firestore, Google Maps/Places/
Directions, Razorpay) — [`di/RepositoryModule.kt`](app/src/main/java/com/amchiyatri/rider/di/RepositoryModule.kt)
binds every interface to its real implementation. Without the setup below, it **will build fine
but crash on launch** (Firebase isn't initialized) or silently fail Maps calls (placeholder API
key). This doc is the checklist to make it actually work end-to-end.

Everything here needs your own accounts — nothing in this repo can create a Google Cloud project,
a Firebase project, or a Razorpay account on your behalf.

## 1. Android app signing info you'll need to register

Your debug keystore's fingerprints (needed for Firebase phone auth / Play Integrity):

```
SHA-1:   0E:D0:A4:33:FF:44:4D:E3:1B:2F:AE:52:C3:EC:15:9E:DD:C0:15:38
SHA256:  49:CB:CB:05:AB:10:96:4F:6B:DF:1C:7D:ED:7B:EF:1B:D9:E4:FD:28:9E:62:7D:AB:F5:C6:60:5E:7F:8B:2E:82
```

(Regenerate with `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` if your debug keystore is different, or once you add a release keystore.)

Package name: `com.amchiyatri.rider`

## 2. Firebase project

1. In the [Firebase console](https://console.firebase.google.com/), add an Android app with
   package name `com.amchiyatri.rider`, paste in the SHA-1 above, and download the generated
   **`google-services.json`**. Put it at `app/google-services.json` (gitignored — it's read
   locally, never committed).
2. **Authentication** → Sign-in method → enable **Phone**.
   - While testing, add your own number under "Phone numbers for testing" with a fixed code
     (e.g. `+91XXXXXXXXXX` → `123456`) so you're not burning real SMS quota or waiting on delivery.
3. **Firestore Database** → create a database (production mode, region `asia-south1` to match the
   Cloud Functions region below).
4. Deploy the security rules and indexes from this repo:
   ```
   npm install -g firebase-tools   # if you don't have it
   firebase login
   firebase use --add              # pick your Firebase project
   firebase deploy --only firestore:rules,firestore:indexes
   ```
   (`firestore.rules` and `firestore.indexes.json` are at the repo root.)

## 3. Google Maps Platform (can be the same Google Cloud project as Firebase)

1. In [Google Cloud Console](https://console.cloud.google.com/) for that project, enable:
   **Maps SDK for Android**, **Places API**, **Directions API**. Billing must be on (all three
   have a free monthly credit; a dev/demo build won't come close to it).
2. Create an API key, restrict it to **Android apps**, and add your package name +
   the SHA-1 above.
3. Copy `app/local.defaults.properties` to `app/secrets.properties` (gitignored) and put your
   real key in it:
   ```
   MAPS_API_KEY=AIzaSy...your real key...
   ```
   This one key covers Maps, Places, *and* the Directions REST calls — `NetworkModule` attaches
   `X-Android-Package`/`X-Android-Cert` headers so the Android-restricted key also works for the
   plain Directions endpoint (see [Google's API security guide](https://developers.google.com/maps/api-security-best-practices)).

## 4. Cloud Functions (the dispatch simulator + Razorpay backend)

```
cd functions
npm install
firebase deploy --only functions
```

This deploys three functions (region `asia-south1`):
- `onRideCreated` — watches new `rides/{rideId}` documents and runs the driver-assignment/
  movement simulation (this is what makes ride tracking "real-time": it's a server process, not
  code in the app).
- `createRazorpayOrder` / `verifyRazorpayPayment` — see step 5.

If you skip this step, requesting a ride will create a Firestore document that just sits at
`SEARCHING_DRIVER` forever (nothing is watching it).

## 5. Razorpay

1. Get your **Key ID** and **Key Secret** from the [Razorpay dashboard](https://dashboard.razorpay.com/)
   (test mode is fine to start).
2. Key ID (not secret) goes in `functions/.env` (copy from `functions/.env.example`):
   ```
   RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
   ```
3. Key **secret** must never sit in a file — set it as a Cloud Functions secret instead:
   ```
   firebase functions:secrets:set RAZORPAY_KEY_SECRET
   ```
   (paste the secret when prompted). Re-run `firebase deploy --only functions` after setting it.

Cash payments skip Razorpay entirely; only UPI/Wallet selections trigger Checkout.

## 6. Build and run

```
./gradlew.bat assembleDebug     # or ./gradlew on macOS/Linux
```

or just hit Run in Android Studio. If everything above is in place: real SMS OTP login, a live
Google Map, real place autocomplete restricted to Mumbai, real road-distance fares, a ride that
gets matched and animated by your own Cloud Function, and a real Razorpay payment sheet.

## Rolling back to fakes for offline development

Every real repository still has a `Fake*`/mock sibling in the same file
(`data/repository/*.kt`). To go back to fully offline behaviour temporarily, edit
[`di/RepositoryModule.kt`](app/src/main/java/com/amchiyatri/rider/di/RepositoryModule.kt) and swap
the `impl` type on the relevant `@Binds` method, e.g.:

```kotlin
// abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
```

Nothing else in the app needs to change either way.

## Troubleshooting

- **Crashes on launch with "Default FirebaseApp is not initialized"** → `app/google-services.json`
  is missing; see step 2.
- **Map is blank / grey** → `app/secrets.properties` is missing or the key isn't enabled for Maps
  SDK for Android; see step 3.
- **Ride stays stuck on "Finding you a nearby driver…"** → the `onRideCreated` function isn't
  deployed, or check `firebase functions:log` for an error in it.
- **Directions/autocomplete return nothing** → check the key has Places API + Directions API
  enabled (not just Maps SDK), and that billing is on for the project.
- **Payment fails immediately** → `RAZORPAY_KEY_SECRET` isn't set (step 5.3), or check
  `firebase functions:log` for the `createRazorpayOrder`/`verifyRazorpayPayment` functions.
