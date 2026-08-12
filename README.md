# Amchi Yatri

An Android rider app for Mumbai, built in the spirit of [Namma Yatri](https://github.com/nammayatri) —
the open-source, zero-commission auto/cab booking platform from Bengaluru. "Amchi" is Marathi for
"our," mirroring "Namma" (Kannada for "our"): same idea, Mumbai edition.

This is a **complete, self-contained rider app** — onboarding through payment and rating — that
builds and runs with **no API keys, no backend, and no account signups**. Maps, OTP delivery, and
payments are all implemented behind small interfaces with realistic fake/simulated data, so the
whole booking flow works end-to-end out of the box. Swap any one of them for a real integration
later without touching the UI layer — see [Going live](#going-live-swapping-in-real-services)
below.

## What's implemented

- **Onboarding**: splash screen, language selection (English / Hindi / Marathi), phone number +
  OTP login (demo OTP is always `1234`)
- **Booking**: current-location pickup, destination search over real Mumbai landmarks, Home/Work
  quick-fill, recent destinations
- **Fare estimation**: Auto / Bike / Sedan / SUV options with distance- and time-based fares,
  occasional surge pricing, ETAs — computed from real haversine distance between pickup and drop
- **Ride lifecycle**: searching for a driver, driver assigned (name, rating, vehicle, phone),
  start-OTP, live map tracking as the driver approaches and then drives the trip, trip completion
- **Safety**: an SOS button during any active ride (call police / call an emergency contact /
  share trip), emergency contacts management in Profile
- **Payments**: Cash / UPI / Wallet selection, itemised fare breakdown, tipping
- **Ratings**: 5-star rating with contextual positive/negative feedback tags
- **Ride history**: full list + per-trip detail view
- **Profile**: name/email/gender, saved places (Home/Work/Other), emergency contacts, language
  switch, help & FAQs, logout

## Tech stack

- Kotlin + Jetpack Compose (Material 3), single-Activity
- MVVM: `ViewModel` + `StateFlow`, Navigation Compose
- Hilt for dependency injection
- DataStore for on-device preferences (language)
- No Maps SDK, no networking library, no payment SDK — see below

## Architecture: why it's all "fakes," and how that's meant to be used

Every external dependency — maps, OTP delivery, place search, payments, the driver-matching
backend — sits behind a small Kotlin interface in `data/repository/`:

| Interface | Fake implementation | What it stands in for |
|---|---|---|
| `AuthRepository` | `FakeAuthRepository` | SMS OTP provider (Firebase Auth phone sign-in, MSG91, 2Factor…) |
| `LocationRepository` | `FakeLocationRepository` | Places Autocomplete + Fused Location Provider |
| `FareRepository` | `DefaultFareRepository` | Directions/routing API for real road distance & ETA |
| `RideRepository` | `FakeRideRepository` | A dispatch/matching backend (Beckn-style search → select → confirm → track) |
| `ProfileRepository` | `FakeProfileRepository` | Your user-profile microservice |

All five are wired up in one place: [`di/RepositoryModule.kt`](app/src/main/java/com/amchiyatri/rider/di/RepositoryModule.kt).
Nothing in `ui/` talks to a "Fake*" class directly — screens and ViewModels only ever see the
interface. That means going live is a matter of writing a new class that implements the interface
against a real API and changing the one `@Binds` line that points to it, not rewriting screens.

The map itself ([`ui/components/MockMap.kt`](app/src/main/java/com/amchiyatri/rider/ui/components/MockMap.kt))
is a `Canvas`-based projection of pickup/drop/driver lat-lngs — deliberately not a real map, so
there's zero Maps API key dependency. It's built so a real `GoogleMap` composable (from
`com.google.maps.android:maps-compose`) can be dropped in behind the same `pickup`/`drop`/`driver`
parameters.

## Going live: swapping in real services

1. **Maps**: add `com.google.maps.android:maps-compose`, get a Google Maps API key, replace the
   body of `MockMap` with a `GoogleMap { Marker(...) }`, add the key to your manifest /
   `local.properties` per the [Maps Compose docs](https://developers.google.com/maps/documentation/android-sdk/maps-compose).
2. **OTP / auth**: implement `AuthRepository` against Firebase Auth phone sign-in or an SMS
   gateway; rebind it in `RepositoryModule`.
3. **Place search**: implement `LocationRepository.search()` against Google Places Autocomplete
   (or an open alternative like OSM Nominatim / Mapbox Search), and `currentLocation` against the
   Fused Location Provider (`com.google.android.gms:play-services-location`).
4. **Routing/fare**: implement `FareRepository.estimateFares()` against the Directions API (or
   OSRM) for real road distance/duration instead of the haversine approximation.
5. **Dispatch backend**: this is the big one — `RideRepository` currently *is* the backend
   (a coroutine simulating driver search → assignment → arrival → trip → completion in-memory).
   A real version needs an actual service matching riders to driver-partner apps — Namma Yatri's
   own stack uses the open [Beckn protocol](https://becknprotocol.io/); you could build your own,
   or integrate with an existing mobility network.
6. **Payments**: wire a UPI intent (`upi://pay`) or a gateway SDK (Razorpay, PhonePe, Cashfree)
   behind the payment-confirmation step in `FareSummaryScreen`.

## Project structure

```
app/src/main/java/com/amchiyatri/rider/
├── data/
│   ├── model/          Plain data classes (Ride, Driver, FareEstimate, UserProfile, ...)
│   ├── repository/      Interfaces + Fake* implementations (see table above)
│   └── local/           DataStore-backed preferences
├── di/                  Hilt modules
├── ui/
│   ├── components/      MockMap, buttons, bottom nav bar
│   ├── navigation/      Destinations + NavHost graph
│   ├── screens/         onboarding/, auth/, home/, location/, booking/, ride/, history/,
│   │                    profile/, support/, splash/
│   ├── theme/           Color, typography, MaterialTheme
│   └── viewmodel/       AuthViewModel, BookingViewModel, RideViewModel, ProfileViewModel,
│                        SettingsViewModel
├── MainActivity.kt
└── AmchiYatriApp.kt      @HiltAndroidApp
```

## Running it

Open the project folder in Android Studio (Iguana or newer) and hit Run — it's a standard Gradle
project, no extra setup needed. `local.properties` already points at this machine's Android SDK.

From the command line:

```
./gradlew.bat assembleDebug     # or ./gradlew on macOS/Linux
```

The debug build was verified to compile and package end-to-end while building this project (Kotlin
1.9.24, Compose BOM 2024.06.00, AGP 8.4.2, targeting SDK 34 / min SDK 24). If your JDK is very new
(24+), the Kotlin 1.9.x compiler may fail to parse its version string — point Gradle at a JDK 17
via `org.gradle.java.home` in `gradle.properties` or `JAVA_HOME` if you hit
`java.lang.IllegalArgumentException` mentioning a Java version number during the build.

## Known limitations (by design, for this build)

- One driver app doesn't exist — the "driver" is a simulation inside `FakeRideRepository`. A real
  product needs a companion driver-partner app and a real matching backend.
- No persistence beyond the current process for ride history/profile (no local database) —
  add Room if you need it to survive app restarts.
- No push notifications; ride status only updates while the app is open.
- Fare/detour logic is a reasonable approximation, not a routing engine.
