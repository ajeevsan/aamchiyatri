# Amchi Yatri

<img width="376" height="839" alt="image" src="https://github.com/user-attachments/assets/8e1916fe-079b-4956-9e77-36828aca0853" />
<img width="384" height="831" alt="image" src="https://github.com/user-attachments/assets/6178abdd-d0ae-4bc1-877b-58469acacc3f" />
<img width="385" height="842" alt="image" src="https://github.com/user-attachments/assets/7a491249-159d-47f6-82be-bec944511dfd" />
<img width="381" height="841" alt="image" src="https://github.com/user-attachments/assets/da947955-94fa-49f8-8232-837af9485211" />
<img width="386" height="847" alt="image" src="https://github.com/user-attachments/assets/268f1c24-c9c0-4366-9a88-e09b1e0370bd" />
<img width="380" height="839" alt="image" src="https://github.com/user-attachments/assets/96a13ae5-8280-405b-a56d-0d9e19b030c7" />
<img width="383" height="849" alt="image" src="https://github.com/user-attachments/assets/dd4d08c8-10ea-4e45-b9c4-2a262f233d00" />


An Android rider app for Mumbai, built in the spirit of [Namma Yatri](https://github.com/nammayatri) —
the open-source, zero-commission auto/cab booking platform from Bengaluru. "Amchi" is Marathi for
"our," mirroring "Namma" (Kannada for "our"): same idea, Mumbai edition.

This is a **complete rider app** — onboarding through payment and rating — wired to **real
services**: Firebase Phone Auth, live Firestore-backed ride dispatch, Google Maps/Places/
Directions, and direct UPI payments. A Cloud Functions backend ([`functions/`](functions)) runs the
actual driver-matching simulation server-side, so ride tracking is genuinely real-time across
devices, not a client-side animation.

**See [SETUP.md](SETUP.md) for the exact steps** to connect your own Firebase/Google Cloud
project — that part can't be done for you, since it needs your credentials. Until you do, the app
builds fine but won't run (no Firebase project = crash on launch). Payments are the one piece that
needs no account at all - see below.

Every real integration still has an offline, no-account `Fake*` counterpart in the same file for
local development — see [Rolling back to fakes](SETUP.md#rolling-back-to-fakes-for-offline-development).

## What's implemented

- **Onboarding**: splash screen, language selection (English / Hindi / Marathi), real phone
  number + SMS OTP login via Firebase Phone Auth (auto-retrieval where the device supports it)
- **Booking**: live GPS pickup location, Places Autocomplete search restricted to Mumbai,
  Home/Work quick-fill, recent destinations
- **Fare estimation**: Auto / Bike / Sedan / SUV options priced off real road distance/duration
  from the Directions API, with a drawn route polyline, occasional surge pricing
- **Ride lifecycle**: request → any online driver-mode user can claim it in real time, or (if
  none do within ~15-20s) a Cloud Function (`onRideCreated`) assigns a simulated one - either way
  the approach and trip animate on a live Google Map, streamed via a Firestore listener
- **Driver mode**: the same app and account can switch into driving (Profile → "Switch to
  driving"): vehicle onboarding, an online/offline toggle, a live list of nearby unclaimed rides,
  claim-and-drive with arrive/OTP-start/complete actions, and live location broadcast to the
  rider - no second app or Play Developer account needed. See **Driver mode** below.
- **Safety**: an SOS button during any active ride (real one-tap dial to police/emergency
  contacts via `ACTION_DIAL`), emergency contacts management in Profile
- **Payments**: Cash, or a real UPI payment (QR code + UPI-app intent) straight to one configured
  VPA - no payment gateway, no merchant account, no Play Developer account needed
- **Ratings**: 5-star rating with contextual positive/negative feedback tags
- **Ride history**: full list + per-trip detail view, synced live from Firestore
- **Profile**: name/email/gender, saved places (Home/Work/Other), emergency contacts, language
  switch, help & FAQs, logout — persisted to Firestore (`users/{uid}`)

## Tech stack

- Kotlin + Jetpack Compose (Material 3), single-Activity
- MVVM: `ViewModel` + `StateFlow`, Navigation Compose
- Hilt for dependency injection
- Firebase: Auth (phone), Firestore (profile + ride state), Cloud Functions (dispatch simulator)
- Google Maps SDK, Places SDK, Directions API (via Retrofit), Fused Location Provider
- ZXing for UPI QR code generation
- DataStore for on-device preferences (language)

## Architecture: real implementations, fakes kept alongside

Every external dependency — maps, OTP delivery, place search, payments, the driver-matching
backend — sits behind a small Kotlin interface in `data/repository/`, each with **two**
implementations in the same file:

| Interface | Real implementation | Fake (offline dev) | What it talks to |
|---|---|---|---|
| `AuthRepository` | `FirebaseAuthRepository` | `FakeAuthRepository` | Firebase Phone Auth |
| `ProfileRepository` | `FirestoreProfileRepository` | `FakeProfileRepository` | Firestore `users/{uid}` |
| `LocationRepository` | `GoogleLocationRepository` | `FakeLocationRepository` | Places Autocomplete + Fused Location Provider |
| `FareRepository` | `DirectionsFareRepository` | `FakeFareRepository` | Directions API |
| `RideRepository` | `FirestoreRideRepository` | `FakeRideRepository` | Firestore `rides/{rideId}` + `functions/dispatch.js` |
| `PaymentRepository` | `UpiPaymentRepository` | `FakePaymentRepository` | A UPI deep link + QR code - no backend at all |

All six are wired up in one place: [`di/RepositoryModule.kt`](app/src/main/java/com/amchiyatri/rider/di/RepositoryModule.kt).
Nothing in `ui/` talks to a concrete class directly — screens and ViewModels only ever see the
interface, so switching between real and fake (or swapping in a different provider entirely) is a
one-line change per repository.

The map ([`ui/components/AmchiYatriMap.kt`](app/src/main/java/com/amchiyatri/rider/ui/components/AmchiYatriMap.kt))
is a real `GoogleMap` (via `maps-compose`) with pickup/drop/driver markers and a decoded route
polyline. The old dependency-free `Canvas` mock (`ui/components/MockMap.kt`) is still in the repo,
unused by default, in case you ever want to strip the Maps dependency again.

## Server-side pieces (`functions/`, `firestore.rules`, `firestore.indexes.json`)

The biggest architectural change from a "fakes-only" build: ride dispatch is no longer simulated
inside the Android app. Requesting a ride just creates a `rides/{rideId}` Firestore document that
any online driver-mode user can claim directly (a plain, rules-guarded Firestore update - see
Driver mode below); `functions/dispatch.js` only steps in ~15-20s later, and only if nobody has
claimed it yet, assigning a simulated driver so the app is still fully demoable solo. See
[SETUP.md](SETUP.md) to deploy it.

Payments deliberately have **no server-side piece** - see the Payments section below.

## Driver mode: same app, same account, no second app

Profile → **"Switch to driving"** turns any account into a driver: a short vehicle-onboarding form
(`DriverOnboardingScreen`), then a **Driver Home** with an online/offline toggle and a live list of
unclaimed nearby rides (`DriverHomeScreen`), and an active-trip screen
(`DriverActiveTripScreen`) with arrive → OTP-verified start → complete actions, continuously
pushing the driver's live GPS to the ride document.

This works with **zero changes to the rider side** - `RideTrackingScreen` was already just reading
whatever's in the `rides/{rideId}` document; it doesn't care whether a Cloud Function or a real
driver wrote it. The only new pieces are [`DriverRepository`](app/src/main/java/com/amchiyatri/rider/data/repository/DriverRepository.kt)
(claim/arrive/start/complete, all plain Firestore reads/writes) and the security rules that let a
driver claim a ride exactly once (`firestore.rules`: the claiming update is only accepted while
`driverId` is still null, so two drivers racing for the same ride can't both win).

**To test solo** (no second phone/account needed): request a ride as a rider, then before the
~15-20s simulator fallback fires, switch to driving from Profile, go online, and accept your own
pending ride from Driver Home. Everything after that - arrive, OTP, complete - plays out exactly
like a real second driver would.

## Payments: direct UPI, no gateway

There's no Razorpay/PayU/etc. integration, and no Google Play Developer account needed, on
purpose: `PaymentRepository` builds a standard NPCI UPI deep link (`upi://pay?pa=...`, the same
parameter scheme as [Dhruvil45/upiqr](https://github.com/Dhruvil45/upiqr), ported to
Kotlin/ZXing since that library targets JS) addressed to **one fixed UPI ID** you configure in
`app/secrets.properties`. `FareSummaryScreen` shows it as a QR code and as a "pay with a UPI app"
button (`ACTION_VIEW` intent); either way the money moves bank-to-bank, with no aggregator in the
middle.

The trade-off: there's no per-driver payout (every ride pays the same VPA - fine for a demo, not
for a real marketplace) and no cryptographic proof of payment the way a gateway's signed webhook
gives you. `FareSummaryScreen` reads back whatever the UPI app's activity result contains and
falls back to asking the rider directly ("Yes, paid" / "It failed") when that's not parseable,
which is most of the time in practice - see `PaymentViewModel`.

## Project structure

```
app/src/main/java/com/amchiyatri/rider/
├── data/
│   ├── model/          Plain data classes (Ride, Driver, FareEstimate, UserProfile, ...)
│   ├── remote/          DirectionsApi (Retrofit) + Firestore (de)serialization mappers
│   ├── repository/      Interfaces + real + Fake* implementations (see table above)
│   └── local/           DataStore-backed preferences
├── di/                  Hilt modules (repositories, network, Firebase/Maps/Places clients)
├── ui/
│   ├── components/      AmchiYatriMap (real), MockMap (unused fallback), buttons, bottom nav
│   ├── navigation/      Destinations + NavHost graph
│   ├── screens/         onboarding/, auth/, home/, location/, booking/, ride/, history/,
│   │                    profile/, support/, splash/, driver/
│   ├── theme/           Color, typography, MaterialTheme
│   └── viewmodel/       Auth, Booking, Ride, Profile, Settings, Payment, Driver
├── util/                ApiKeys, Dialer, PolylineDecoder, UpiQrCode
├── MainActivity.kt
└── AmchiYatriApp.kt      @HiltAndroidApp; initializes the Places SDK
functions/                Cloud Functions: dispatch.js (the ride-dispatch simulator/fallback)
firestore.rules           Per-account access control for users/ and rides/ (incl. driver claims)
firestore.indexes.json    Composite indexes for ride-history and pending-rides queries
```

## Running it

**First, do the [SETUP.md](SETUP.md) steps** — without them the app builds but crashes on launch
(no Firebase project configured yet).

Open the project folder in Android Studio (Iguana or newer) and hit Run — it's a standard Gradle
project. `local.properties` already points at this machine's Android SDK.

From the command line:

```
./gradlew.bat assembleDebug     # or ./gradlew on macOS/Linux
```

The debug build was verified to compile and package end-to-end while building this project (Kotlin
1.9.24, Compose BOM 2024.06.00, AGP 8.4.2, targeting SDK 34 / min SDK 24). If your JDK is very new
(24+), the Kotlin 1.9.x compiler may fail to parse its version string — point Gradle at a JDK 17
via `org.gradle.java.home` in `gradle.properties` or `JAVA_HOME` if you hit
`java.lang.IllegalArgumentException` mentioning a Java version number during the build.

## Known limitations

- No ride-matching by distance yet - `DriverHomeScreen` lists *every* unclaimed ride, not just
  nearby ones (fine for a single-city demo; a real launch would geo-filter, likely via geohashing
  since Firestore has no native geoqueries).
- No driver-side ride history yet - `RideHistoryScreen` only ever queries `riderId == uid`; a
  driver's completed trips aren't shown anywhere yet (the data's on the ride docs already via
  `driverId`, it just needs its own query + screen).
- No push notifications; ride status only updates while the app is open (Firestore listeners are
  live, but there's no FCM wake-up if the app is killed) - this affects drivers waiting for new
  requests just as much as riders waiting for updates.
- Directions-based fare/route calls happen directly from the device; for stricter key security
  you could proxy them through a Cloud Function, at the cost of one more network hop.
- Payments pay a single fixed UPI ID with no aggregator - see the Payments section above for what
  that does and doesn't give you.
