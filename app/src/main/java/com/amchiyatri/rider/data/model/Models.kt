package com.amchiyatri.rider.data.model

import java.util.UUID

/** Plain lat/lng pair. Swappable 1:1 with com.google.android.gms.maps.model.LatLng later. */
data class GeoPoint(val lat: Double, val lng: Double)

data class PlaceSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val point: GeoPoint,
    val isSaved: Boolean = false,
    val isRecent: Boolean = false,
)

enum class SavedPlaceLabel { HOME, WORK, OTHER }

data class SavedPlace(
    val id: String = UUID.randomUUID().toString(),
    val label: SavedPlaceLabel,
    val customName: String? = null,
    val address: String,
    val point: GeoPoint,
)

enum class VehicleType(
    val displayName: String,
    val description: String,
    val capacity: Int,
    val baseFare: Double,
    val perKmRate: Double,
    val perMinRate: Double,
    val etaPaddingMin: Int,
) {
    AUTO("Auto", "Metered auto-rickshaw", 3, 30.0, 17.5, 1.0, 2),
    BIKE("Bike", "Quick two-wheeler rides", 1, 20.0, 8.5, 0.5, 1),
    SEDAN("Sedan", "AC cab · 4 seater", 4, 60.0, 22.0, 1.5, 4),
    SUV("SUV", "AC cab · 6 seater, extra space", 6, 90.0, 28.0, 2.0, 5),
}

enum class PaymentMethod { CASH, UPI, WALLET }

data class FareEstimate(
    val vehicleType: VehicleType,
    val distanceKm: Double,
    val durationMin: Int,
    val etaMin: Int,
    val totalFare: Double,
    val surgeMultiplier: Double = 1.0,
)

data class FareBreakdownLine(val label: String, val amount: Double)

enum class RideStatus {
    IDLE,
    REQUESTED,
    SEARCHING_DRIVER,
    NO_DRIVER_FOUND,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVING,
    DRIVER_ARRIVED,
    ON_TRIP,
    COMPLETED,
    CANCELLED,
}

data class Driver(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rating: Double,
    val totalTrips: Int,
    val vehicleNumber: String,
    val vehicleModel: String,
    val phoneNumber: String,
    val photoInitials: String,
)

data class Ride(
    val id: String = UUID.randomUUID().toString(),
    val status: RideStatus,
    val vehicleType: VehicleType,
    val pickup: PlaceSuggestion,
    val drop: PlaceSuggestion,
    val fare: FareEstimate,
    val paymentMethod: PaymentMethod,
    val driver: Driver? = null,
    val startOtp: String? = null,
    val driverLocation: GeoPoint? = null,
    val requestedAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null,
    val finalFare: Double? = null,
    val riderRating: Int? = null,
)

data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
)

enum class Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val gender: Gender? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val savedPlaces: List<SavedPlace> = emptyList(),
)

enum class AppLanguage(val code: String, val label: String, val nativeLabel: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    MARATHI("mr", "Marathi", "मराठी"),
}
