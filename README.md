# Amchi Yatri

An Android rider app for Mumbai, built in the spirit of [Namma Yatri](https://github.com/nammayatri) —
the open-source, zero-commission auto/cab booking platform from Bengaluru. "Amchi" is Marathi for
"our," mirroring "Namma" (Kannada for "our"): same idea, Mumbai edition.

This is a **complete rider app** — onboarding through payment and rating — wired to **real
services**: Firebase Phone Auth, live Firestore-backed ride dispatch, Google Maps/Places/
Directions, and Razorpay. A Cloud Functions backend ([`functions/`](functions)) runs the actual
driver-matching simulation server-side, so ride tracking is genuinely real-time across devices,
not a client-side animation.

**New checkout: see [SETUP.md](SETUP.md) for the exact steps** to connect your own Firebase/Google
Cloud/Razorpay accounts — none of that can be done for you, since it needs your credentials. Until
you do, the app builds fine but won't run (no Firebase project = crash on launch).

Every real integration still has an offline, no-account `Fake*` counterpart in the same file for
local development — see [Rolling back to fakes](SETUP.md#rolling-back-to-fakes-for-offline-development).

## What's implemented

- **Onboarding**: splash screen, language selection (English / Hindi / Marathi), real phone
  number + SMS OTP login via Firebase Phone Auth (auto-retrieval where the device supports it)
- **Booking**: live GPS pickup location, Places Autocomplete search restricted to Mumbai,
  Home/Work quick-fill, recent destinations
- **Fare estimation**: Auto / Bike / Sedan / SUV options priced off real road distance/duration
  from the Directions API, with a drawn route polyline, occasional surge pricing
- **Ride lifecycle**: request → a Cloud Function (`onRideCreated`) matches a driver, animates
  their approach and the trip on a live Google Map, all streamed back via a Firestore listener
- **Safety**: an SOS button during any active ride (real one-tap dial to police/emergency
  contacts via `ACTION_DIAL`), emergency contacts management in Profile
- **Payments**: Cash (skips the gateway) or UPI/Wallet via real Razorpay Checkout, with order
  creation and signature verification done server-side in Cloud Functions
- **Ratings**: 5-star rating with contextual positive/negative feedback tags
- **Ride history**: full list + per-trip detail view, synced live from Firestore
- **Profile**: name/email/gender, saved places (Home/Work/Other), emergency contacts, language
  switch, help & FAQs, logout — persisted to Firestore (`users/{uid}`)

## Tech stack

- Kotlin + Jetpack Compose (Material 3), single-Activity
- MVVM: `ViewModel` + `StateFlow`, Navigation Compose
- Hilt for dependency injection
- Firebase: Auth (phone), Firestore (profile + ride state), Cloud Functions (dispatch simulator,
  Razorpay order/verify)
- Google Maps SDK, Places SDK, Directions API (via Retrofit), Fused Location Provider
- Razorpay Android Checkout SDK
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
| `PaymentRepository` | `RazorpayPaymentRepository` | `FakePaymentRepository` | Razorpay via `functions/payments.js` |

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
inside the Android app. Requesting a ride just creates a `rides/{rideId}` Firestore document; a
Cloud Function (`functions/dispatch.js`) picks it up, assigns a (still simulated, for now) driver,
and animates their location over time by writing updates to that same document — which every
device watching it (via a Firestore snapshot listener) sees live. `functions/payments.js` does the
equivalent for Razorpay: creating orders and verifying payment signatures server-side, since the
secret key can never live in the app. See [SETUP.md](SETUP.md) to deploy these.

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
│   │                    profile/, support/, splash/
│   ├── theme/           Color, typography, MaterialTheme
│   └── viewmodel/       Auth, Booking, Ride, Profile, Settings, Payment
├── util/                ApiKeys, Dialer, PolylineDecoder, RazorpayResultBridge
├── MainActivity.kt      Also implements Razorpay's PaymentResultWithDataListener
└── AmchiYatriApp.kt      @HiltAndroidApp; initializes Places SDK + Razorpay Checkout
functions/                Cloud Functions: dispatch.js (ride simulator), payments.js (Razorpay)
firestore.rules           Per-rider access control for users/ and rides/
firestore.indexes.json    Composite index for the ride-history query
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

- There's still no driver-partner app — `onRideCreated` *simulates* a driver server-side rather
  than matching a real one. Swapping in real drivers means building that companion app and
  replacing `functions/dispatch.js`'s random assignment with an actual matching query.
- No push notifications; ride status only updates while the app is open (Firestore listeners are
  live, but there's no FCM wake-up if the app is killed).
- Directions-based fare/route calls happen directly from the device; for stricter key security
  you could proxy them through a Cloud Function the same way payments are, at the cost of one more
  network hop.
