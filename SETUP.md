# Setup: going from fakes to real services

The app now defaults to **real** integrations (Firebase Auth/Firestore, Google Maps/Places/
Directions, direct UPI payments) — [`di/RepositoryModule.kt`](app/src/main/java/com/amchiyatri/rider/di/RepositoryModule.kt)
binds every interface to its real implementation. Without the setup below, it **will build fine
but crash on launch** (Firebase isn't initialized) or silently fail Maps calls (placeholder API
key). This doc is the checklist to make it actually work end-to-end.

Everything here needs your own accounts — nothing in this repo can create a Google Cloud project
or a Firebase project on your behalf. Payments are the one exception: they need **no account at
all** beyond a UPI ID you already have (GPay/PhonePe/BHIM/etc.) - no Razorpay, no Play Developer
account. See step 5.

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

## 4. Cloud Functions (the dispatch simulator)

```
cd functions
npm install
firebase deploy --only functions
```

This deploys `onRideCreated` — it watches new `rides/{rideId}` documents and runs the
driver-assignment/movement simulation (this is what makes ride tracking "real-time": it's a
server process, not code in the app).

If you skip this step, requesting a ride will create a Firestore document that just sits at
`SEARCHING_DRIVER` forever (nothing is watching it).

## 5. Payments: your own UPI ID, no gateway account

Real UPI payments here don't go through Razorpay/PayU/any aggregator - they're a direct
"scan-or-tap-to-pay" request to **one fixed UPI ID**, the same mechanism as a shop counter's QR
code. That's a deliberate trade-off given there's no merchant account: no signup, but also no
per-driver payouts and no cryptographic proof of payment (see the README's Known limitations).

1. Pick any UPI ID you control (your own GPay/PhonePe/BHIM VPA is fine for testing).
2. Put it in `app/secrets.properties` (create it from `app/local.defaults.properties` if you
   haven't already):
   ```
   UPI_PAYEE_VPA=yourname@upi
   UPI_PAYEE_NAME=Amchi Yatri
   ```
3. That's it - no deploy step, no dashboard, no secret to set. `FareSummaryScreen` builds a
   `upi://pay?...` link and a QR code from these two values (see `util/UpiQrCode.kt`).

Cash payments skip this entirely.

## 6. Build and run

```
./gradlew.bat assembleDebug     # or ./gradlew on macOS/Linux
```

or just hit Run in Android Studio. If everything above is in place: real SMS OTP login, a live
Google Map, real place autocomplete restricted to Mumbai, real road-distance fares, a ride that
gets matched and animated by your own Cloud Function, and a real UPI payment QR/intent.

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
- **"Pay with a UPI app" does nothing / no app opens** → no UPI app is installed (common on
  emulators) - the QR code still works if a phone with a UPI app scans it, and the manual
  "Yes, paid" / "It failed" buttons are always available as a fallback either way.
