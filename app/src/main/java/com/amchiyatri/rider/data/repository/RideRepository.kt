package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.Driver
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.data.remote.fromFirestore
import com.amchiyatri.rider.data.remote.ridePlaceholderMap
import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the full ride lifecycle: request -> search -> match -> arrive -> trip -> complete.
 *
 * [FirestoreRideRepository] only ever writes the *request*; matching, driver assignment and
 * live location updates are all done by a Cloud Function (functions/dispatch.js) reacting to
 * that write, and streamed back down via a Firestore snapshot listener. That split is what makes
 * this "real-time" rather than a client-side animation: the simulation runs server-side and every
 * device watching the ride document sees the same state at the same time.
 *
 * [FakeRideRepository] drives the exact same state machine itself with coroutine delays, for
 * offline dev - see SETUP.md. Every downstream screen (searching, driver-assigned, live tracking,
 * fare summary) only ever reads [activeRide], so which implementation is wired in doesn't affect
 * the UI layer at all.
 */
interface RideRepository {
    val activeRide: StateFlow<Ride?>
    val rideHistory: StateFlow<List<Ride>>

    fun requestRide(
        pickup: PlaceSuggestion,
        drop: PlaceSuggestion,
        fare: FareEstimate,
        paymentMethod: PaymentMethod,
        routePolyline: List<GeoPoint> = emptyList(),
    )

    fun cancelRide(reason: String)

    suspend fun submitRating(stars: Int, tipAmount: Double, feedbackTags: List<String>)

    /** Dismisses a completed/cancelled ride from [activeRide] once the rider has seen its summary. */
    fun clearActiveRide()
}

@Singleton
class FirestoreRideRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : RideRepository {

    private val repoScope = CoroutineScope(SupervisorJob())

    private val _activeRide = MutableStateFlow<Ride?>(null)
    override val activeRide: StateFlow<Ride?> = _activeRide.asStateFlow()

    private val _rideHistory = MutableStateFlow<List<Ride>>(emptyList())
    override val rideHistory: StateFlow<List<Ride>> = _rideHistory.asStateFlow()

    private var activeRideListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var activeRideId: String? = null

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                listenToHistory(uid)
            } else {
                historyListener?.remove()
                _rideHistory.value = emptyList()
            }
        }
    }

    override fun requestRide(
        pickup: PlaceSuggestion,
        drop: PlaceSuggestion,
        fare: FareEstimate,
        paymentMethod: PaymentMethod,
        routePolyline: List<GeoPoint>,
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val data = ridePlaceholderMap(uid, fare.vehicleType, pickup, drop, fare, paymentMethod, routePolyline)

        val docRef = firestore.collection("rides").document()
        activeRideId = docRef.id
        listenToActiveRide(docRef.id)
        docRef.set(data)
        // The onRideCreated Cloud Function (functions/dispatch.js) picks this document up from
        // here and drives status/driver/driverLocation forward - nothing else to do client-side.
    }

    private fun listenToActiveRide(rideId: String) {
        activeRideListener?.remove()
        activeRideListener = firestore.collection("rides").document(rideId)
            .addSnapshotListener { snapshot, _ ->
                _activeRide.value = snapshot?.let { Ride.fromFirestore(it) }
            }
    }

    private fun listenToHistory(uid: String) {
        historyListener?.remove()
        historyListener = firestore.collection("rides")
            .whereEqualTo("riderId", uid)
            .whereIn("status", listOf(RideStatus.COMPLETED.name, RideStatus.CANCELLED.name))
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                _rideHistory.value = snapshot?.documents?.mapNotNull { Ride.fromFirestore(it) } ?: emptyList()
            }
    }

    override fun cancelRide(reason: String) {
        val rideId = activeRideId ?: return
        firestore.collection("rides").document(rideId)
            .update(mapOf("status" to RideStatus.CANCELLED.name, "cancelReason" to reason))
    }

    override suspend fun submitRating(stars: Int, tipAmount: Double, feedbackTags: List<String>) {
        val rideId = activeRideId ?: return
        val current = _activeRide.value ?: return
        firestore.collection("rides").document(rideId).update(
            mapOf(
                "riderRating" to stars,
                "finalFare" to (current.finalFare ?: current.fare.totalFare) + tipAmount,
                "feedbackTags" to feedbackTags,
            ),
        ).await()
    }

    override fun clearActiveRide() {
        activeRideListener?.remove()
        activeRideListener = null
        activeRideId = null
        _activeRide.value = null
    }
}

@Singleton
class FakeRideRepository @Inject constructor() : RideRepository {

    private val scope = CoroutineScope(SupervisorJob())
    private var simulationJob: Job? = null

    private val _activeRide = MutableStateFlow<Ride?>(null)
    override val activeRide: StateFlow<Ride?> = _activeRide.asStateFlow()

    private val _rideHistory = MutableStateFlow<List<Ride>>(emptyList())
    override val rideHistory: StateFlow<List<Ride>> = _rideHistory.asStateFlow()

    private val driverNamePool = listOf(
        "Ganesh Pawar", "Ramesh Yadav", "Iqbal Shaikh", "Suresh Gaikwad",
        "Anthony D'Souza", "Vikram Chavan", "Mohammed Ali", "Sunil More",
        "Rajendra Jadhav", "Prakash Kamble",
    )

    override fun requestRide(
        pickup: PlaceSuggestion,
        drop: PlaceSuggestion,
        fare: FareEstimate,
        paymentMethod: PaymentMethod,
        routePolyline: List<GeoPoint>,
    ) {
        simulationJob?.cancel()

        val ride = Ride(
            status = RideStatus.SEARCHING_DRIVER,
            vehicleType = fare.vehicleType,
            pickup = pickup,
            drop = drop,
            fare = fare,
            paymentMethod = paymentMethod,
            routePolyline = routePolyline,
        )
        _activeRide.value = ride

        simulationJob = scope.launch {
            runSimulation(ride.id, pickup, drop)
        }
    }

    private suspend fun runSimulation(rideId: String, pickup: PlaceSuggestion, drop: PlaceSuggestion) {
        delay(Random.nextLong(2200, 3800))
        if (_activeRide.value?.id != rideId) return // cancelled meanwhile

        val driverFound = Random.nextDouble() > 0.08
        if (!driverFound) {
            updateRide(rideId) { it.copy(status = RideStatus.NO_DRIVER_FOUND) }
            return
        }

        val driver = Driver(
            name = driverNamePool.random(),
            rating = Random.nextDouble(4.3, 5.0).roundTo(1),
            totalTrips = Random.nextInt(120, 9800),
            vehicleNumber = "MH ${Random.nextInt(1, 5).toString().padStart(2, '0')} " +
                "${('A'..'Z').random()}${('A'..'Z').random()} ${Random.nextInt(1000, 9999)}",
            vehicleModel = vehicleModelFor(_activeRide.value?.vehicleType),
            phoneNumber = "98${Random.nextInt(10000000, 99999999)}",
            photoInitials = driverNamePool.random().split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
        )
        val otp = Random.nextInt(1000, 9999).toString()
        val driverStart = offsetPoint(pickup.point, km = Random.nextDouble(1.0, 3.0))

        updateRide(rideId) {
            it.copy(
                status = RideStatus.DRIVER_ASSIGNED,
                driver = driver,
                startOtp = otp,
                driverLocation = driverStart,
            )
        }

        // Driver drives from their start point to the pickup point.
        updateRide(rideId) { it.copy(status = RideStatus.DRIVER_ARRIVING) }
        animateBetween(rideId, from = driverStart, to = pickup.point, steps = 6, stepDelayMs = 700)
        if (_activeRide.value?.id != rideId) return
        updateRide(rideId) { it.copy(status = RideStatus.DRIVER_ARRIVED, driverLocation = pickup.point) }

        delay(1800) // rider gives the OTP, driver keys it in
        if (_activeRide.value?.id != rideId) return
        updateRide(rideId) { it.copy(status = RideStatus.ON_TRIP) }

        // Trip itself: pickup -> drop, compressed to a short demo animation regardless of real ETA.
        animateBetween(rideId, from = pickup.point, to = drop.point, steps = 10, stepDelayMs = 550)
        if (_activeRide.value?.id != rideId) return

        val completed = updateRide(rideId) {
            it.copy(
                status = RideStatus.COMPLETED,
                driverLocation = drop.point,
                completedAtMillis = System.currentTimeMillis(),
                finalFare = it.fare.totalFare,
            )
        }
        completed?.let { addToHistory(it) }
    }

    private suspend fun animateBetween(rideId: String, from: GeoPoint, to: GeoPoint, steps: Int, stepDelayMs: Long) {
        for (step in 1..steps) {
            if (_activeRide.value?.id != rideId) return
            val fraction = step.toDouble() / steps
            val point = GeoPoint(
                lat = from.lat + (to.lat - from.lat) * fraction,
                lng = from.lng + (to.lng - from.lng) * fraction,
            )
            updateRide(rideId) { it.copy(driverLocation = point) }
            delay(stepDelayMs)
        }
    }

    override fun cancelRide(reason: String) {
        simulationJob?.cancel()
        val cancelled = updateRide(_activeRide.value?.id) { it.copy(status = RideStatus.CANCELLED) }
        cancelled?.let { if (it.driver != null) addToHistory(it) }
    }

    override suspend fun submitRating(stars: Int, tipAmount: Double, feedbackTags: List<String>) {
        delay(400)
        val current = _activeRide.value ?: return
        val rated = current.copy(
            riderRating = stars,
            finalFare = (current.finalFare ?: current.fare.totalFare) + tipAmount,
        )
        _rideHistory.value = _rideHistory.value.map { if (it.id == rated.id) rated else it }
        _activeRide.value = rated
    }

    override fun clearActiveRide() {
        simulationJob?.cancel()
        _activeRide.value = null
    }

    private fun addToHistory(ride: Ride) {
        _rideHistory.value = listOf(ride) + _rideHistory.value.filterNot { it.id == ride.id }
    }

    private fun updateRide(rideId: String?, transform: (Ride) -> Ride): Ride? {
        if (rideId == null) return null
        var updated: Ride? = null
        _activeRide.update { current ->
            if (current?.id == rideId) transform(current).also { updated = it } else current
        }
        return updated
    }

    private fun vehicleModelFor(vehicleType: com.amchiyatri.rider.data.model.VehicleType?) = when (vehicleType) {
        com.amchiyatri.rider.data.model.VehicleType.AUTO -> "Bajaj RE Auto"
        com.amchiyatri.rider.data.model.VehicleType.BIKE -> "Honda Activa"
        com.amchiyatri.rider.data.model.VehicleType.SEDAN -> "Suzuki Dzire"
        com.amchiyatri.rider.data.model.VehicleType.SUV -> "Toyota Innova"
        null -> "Vehicle"
    }

    /** Nudges [point] by roughly [km] kilometres in a random direction, for a plausible driver start position. */
    private fun offsetPoint(point: GeoPoint, km: Double): GeoPoint {
        val degreesPerKm = 1.0 / 111.0
        val angle = Random.nextDouble(0.0, 2 * Math.PI)
        return GeoPoint(
            lat = point.lat + km * degreesPerKm * kotlin.math.cos(angle),
            lng = point.lng + km * degreesPerKm * kotlin.math.sin(angle),
        )
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(this * factor) / factor
    }
}
