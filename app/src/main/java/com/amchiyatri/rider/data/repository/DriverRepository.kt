package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.Driver
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.data.remote.fromFirestore
import com.amchiyatri.rider.data.remote.toFirestoreMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The driver-mode half of the same `rides` collection [RideRepository] uses. A driver "accepting"
 * a ride is just a guarded Firestore update (see firestore.rules: it only succeeds if the ride is
 * still unclaimed) - no separate backend, and the rider side needs zero code changes to see it,
 * since it's already watching this same document.
 *
 * [FirestoreDriverRepository] is the real implementation. [FakeDriverRepository] is the offline
 * dev fallback - see SETUP.md.
 */
interface DriverRepository {
    val isOnline: StateFlow<Boolean>

    /** Unclaimed ride requests, live, while [isOnline] is true. */
    val pendingRides: StateFlow<List<Ride>>

    /** The ride this driver is currently handling, if any. */
    val activeDriverRide: StateFlow<Ride?>
    val activeDriverRideError: StateFlow<String?>

    fun goOnline()
    fun goOffline()

    /** Fails (without throwing) if someone else claimed the ride first - see firestore.rules. */
    suspend fun acceptRide(rideId: String): Result<Unit>

    suspend fun markArrived(): Result<Unit>

    /** Verifies [enteredOtp] against the ride's startOtp before starting the trip. */
    suspend fun startTrip(enteredOtp: String): Result<Unit>

    suspend fun completeTrip(): Result<Unit>

    /** Dismisses the active ride from driver-mode state once its outcome has been seen. */
    fun leaveActiveRide()
}

@Singleton
class FirestoreDriverRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val profileRepository: ProfileRepository,
    private val locationRepository: LocationRepository,
) : DriverRepository {

    private val repoScope = CoroutineScope(SupervisorJob())

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingRides = MutableStateFlow<List<Ride>>(emptyList())
    override val pendingRides: StateFlow<List<Ride>> = _pendingRides.asStateFlow()

    private val _activeDriverRide = MutableStateFlow<Ride?>(null)
    override val activeDriverRide: StateFlow<Ride?> = _activeDriverRide.asStateFlow()

    private val _activeDriverRideError = MutableStateFlow<String?>(null)
    override val activeDriverRideError: StateFlow<String?> = _activeDriverRideError.asStateFlow()

    private var pendingRidesListener: ListenerRegistration? = null
    private var activeRideListener: ListenerRegistration? = null
    private var locationPushJob: Job? = null
    private var activeRideId: String? = null

    override fun goOnline() {
        _isOnline.value = true
        locationRepository.startLocationUpdates()
        repoScope.launch { profileRepository.setDriverOnline(true) }
        pendingRidesListener?.remove()
        pendingRidesListener = firestore.collection("rides")
            .whereEqualTo("status", RideStatus.SEARCHING_DRIVER.name)
            .orderBy("requestedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                _pendingRides.value = snapshot?.documents?.mapNotNull { Ride.fromFirestore(it) } ?: emptyList()
            }
    }

    override fun goOffline() {
        _isOnline.value = false
        repoScope.launch { profileRepository.setDriverOnline(false) }
        pendingRidesListener?.remove()
        pendingRidesListener = null
        _pendingRides.value = emptyList()
    }

    override suspend fun acceptRide(rideId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("Not signed in")
        val profile = profileRepository.profile.value ?: error("Profile not loaded yet")
        val details = profile.driverDetails ?: error("Finish driver onboarding first")
        val location = locationRepository.currentLocation.value

        val driver = Driver(
            id = uid,
            name = profile.name,
            rating = details.rating,
            totalTrips = details.totalTrips,
            vehicleNumber = details.vehicleNumber,
            vehicleModel = details.vehicleModel,
            phoneNumber = profile.phoneNumber,
            photoInitials = profile.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
        )
        val otp = Random.nextInt(1000, 9999).toString()

        firestore.collection("rides").document(rideId).update(
            mapOf(
                "status" to RideStatus.DRIVER_ARRIVING.name,
                "driverId" to uid,
                "driver" to driver.toFirestoreMap(),
                "startOtp" to otp,
                "driverLocation" to location.toFirestoreMap(),
            ),
        ).await()

        activeRideId = rideId
        _activeDriverRideError.value = null
        listenToActiveDriverRide(rideId)
        pushLocationWhileActive(rideId)
    }

    private fun listenToActiveDriverRide(rideId: String) {
        activeRideListener?.remove()
        activeRideListener = firestore.collection("rides").document(rideId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _activeDriverRideError.value = "Couldn't load this ride: ${error.message}"
                    return@addSnapshotListener
                }
                _activeDriverRide.value = snapshot?.let { Ride.fromFirestore(it) }
            }
    }

    /** Keeps the ride's driverLocation fresh for the rider's map while a ride is in progress. */
    private fun pushLocationWhileActive(rideId: String) {
        locationPushJob?.cancel()
        locationPushJob = repoScope.launch {
            while (true) {
                val ride = _activeDriverRide.value
                if (ride == null || ride.id != rideId || ride.status == RideStatus.COMPLETED || ride.status == RideStatus.CANCELLED) {
                    break
                }
                runCatching {
                    firestore.collection("rides").document(rideId)
                        .update("driverLocation", locationRepository.currentLocation.value.toFirestoreMap())
                        .await()
                }
                delay(4000)
            }
        }
    }

    override suspend fun markArrived(): Result<Unit> = updateActiveRide {
        mapOf("status" to RideStatus.DRIVER_ARRIVED.name)
    }

    override suspend fun startTrip(enteredOtp: String): Result<Unit> {
        val expected = _activeDriverRide.value?.startOtp
        if (expected == null || expected != enteredOtp) {
            return Result.failure(IllegalArgumentException("Incorrect OTP"))
        }
        return updateActiveRide { mapOf("status" to RideStatus.ON_TRIP.name) }
    }

    override suspend fun completeTrip(): Result<Unit> {
        val fare = _activeDriverRide.value?.fare?.totalFare ?: return Result.failure(IllegalStateException("No active ride"))
        return updateActiveRide {
            mapOf(
                "status" to RideStatus.COMPLETED.name,
                "completedAt" to FieldValue.serverTimestamp(),
                "finalFare" to fare,
            )
        }
    }

    private suspend fun updateActiveRide(fields: () -> Map<String, Any?>): Result<Unit> = runCatching {
        val rideId = activeRideId ?: error("No active ride")
        firestore.collection("rides").document(rideId).update(fields()).await()
    }

    override fun leaveActiveRide() {
        activeRideListener?.remove()
        activeRideListener = null
        locationPushJob?.cancel()
        locationPushJob = null
        activeRideId = null
        _activeDriverRide.value = null
        _activeDriverRideError.value = null
    }
}

@Singleton
class FakeDriverRepository @Inject constructor() : DriverRepository {

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    override val pendingRides: StateFlow<List<Ride>> = MutableStateFlow(emptyList<Ride>()).asStateFlow()

    private val _activeDriverRide = MutableStateFlow<Ride?>(null)
    override val activeDriverRide: StateFlow<Ride?> = _activeDriverRide.asStateFlow()

    override val activeDriverRideError: StateFlow<String?> = MutableStateFlow(null).asStateFlow()

    override fun goOnline() {
        _isOnline.value = true
    }

    override fun goOffline() {
        _isOnline.value = false
    }

    override suspend fun acceptRide(rideId: String): Result<Unit> =
        Result.failure(IllegalStateException("Driver mode needs the real Firestore backend - see SETUP.md"))

    override suspend fun markArrived(): Result<Unit> = Result.success(Unit)
    override suspend fun startTrip(enteredOtp: String): Result<Unit> = Result.success(Unit)
    override suspend fun completeTrip(): Result<Unit> = Result.success(Unit)
    override fun leaveActiveRide() {
        _activeDriverRide.value = null
    }
}
