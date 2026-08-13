package com.amchiyatri.rider.data.remote

import com.amchiyatri.rider.data.model.Driver
import com.amchiyatri.rider.data.model.DriverDetails
import com.amchiyatri.rider.data.model.EmergencyContact
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.Gender
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.data.model.SavedPlace
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.data.model.UserProfile
import com.amchiyatri.rider.data.model.UserRole
import com.amchiyatri.rider.data.model.VehicleType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp

/**
 * Hand-written (de)serialization between our domain models and Firestore documents.
 *
 * Firestore's automatic POJO mapping needs every property to have a default value to build via
 * reflection, which our domain models deliberately don't (a [EmergencyContact] with a blank name
 * shouldn't type-check as valid) - so we map explicitly instead, once, here.
 *
 * These field names/shapes are a contract shared with functions/dispatch.js and
 * functions/payments.js - change one side, change the other.
 */

// ---- GeoPoint --------------------------------------------------------------------------------

fun GeoPoint.toFirestoreMap(): Map<String, Any?> = mapOf("lat" to lat, "lng" to lng)

private fun Map<*, *>.toGeoPoint(): GeoPoint? {
    val lat = (this["lat"] as? Number)?.toDouble() ?: return null
    val lng = (this["lng"] as? Number)?.toDouble() ?: return null
    return GeoPoint(lat, lng)
}

// ---- UserProfile ------------------------------------------------------------------------------

fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "phoneNumber" to phoneNumber,
    "email" to email,
    "gender" to gender?.name,
    "emergencyContacts" to emergencyContacts.map { it.toFirestoreMap() },
    "savedPlaces" to savedPlaces.map { it.toFirestoreMap() },
    "activeRole" to activeRole.name,
    "driverDetails" to driverDetails?.toFirestoreMap(),
)

fun UserProfile.Companion.fromFirestore(doc: DocumentSnapshot): UserProfile? {
    if (!doc.exists()) return null
    return UserProfile(
        id = doc.id,
        name = doc.getString("name") ?: "",
        phoneNumber = doc.getString("phoneNumber") ?: "",
        email = doc.getString("email"),
        gender = doc.getString("gender")?.let { runCatching { Gender.valueOf(it) }.getOrNull() },
        emergencyContacts = (doc.get("emergencyContacts") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toEmergencyContact() }
            ?: emptyList(),
        savedPlaces = (doc.get("savedPlaces") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toSavedPlace() }
            ?: emptyList(),
        activeRole = doc.getString("activeRole")?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.RIDER,
        driverDetails = (doc.get("driverDetails") as? Map<*, *>)?.toDriverDetails(),
    )
}

fun DriverDetails.toFirestoreMap(): Map<String, Any?> = mapOf(
    "vehicleType" to vehicleType.name,
    "vehicleNumber" to vehicleNumber,
    "vehicleModel" to vehicleModel,
    "rating" to rating,
    "totalTrips" to totalTrips,
    "isOnline" to isOnline,
)

private fun Map<*, *>.toDriverDetails(): DriverDetails? {
    val vehicleType = (this["vehicleType"] as? String)?.let { runCatching { VehicleType.valueOf(it) }.getOrNull() } ?: return null
    return DriverDetails(
        vehicleType = vehicleType,
        vehicleNumber = this["vehicleNumber"] as? String ?: "",
        vehicleModel = this["vehicleModel"] as? String ?: "",
        rating = (this["rating"] as? Number)?.toDouble() ?: 5.0,
        totalTrips = (this["totalTrips"] as? Number)?.toInt() ?: 0,
        isOnline = this["isOnline"] as? Boolean ?: false,
    )
}

fun EmergencyContact.toFirestoreMap(): Map<String, Any?> = mapOf("id" to id, "name" to name, "phoneNumber" to phoneNumber)

private fun Map<*, *>.toEmergencyContact(): EmergencyContact? {
    return EmergencyContact(
        id = this["id"] as? String ?: return null,
        name = this["name"] as? String ?: return null,
        phoneNumber = this["phoneNumber"] as? String ?: return null,
    )
}

fun SavedPlace.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "label" to label.name,
    "customName" to customName,
    "address" to address,
    "lat" to point.lat,
    "lng" to point.lng,
)

private fun Map<*, *>.toSavedPlace(): SavedPlace? {
    val label = (this["label"] as? String)?.let { runCatching { SavedPlaceLabel.valueOf(it) }.getOrNull() } ?: return null
    val lat = (this["lat"] as? Number)?.toDouble() ?: return null
    val lng = (this["lng"] as? Number)?.toDouble() ?: return null
    return SavedPlace(
        id = this["id"] as? String ?: return null,
        label = label,
        customName = this["customName"] as? String,
        address = this["address"] as? String ?: return null,
        point = GeoPoint(lat, lng),
    )
}

// ---- Ride ---------------------------------------------------------------------------------

fun PlaceSuggestion.toFirestoreMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "subtitle" to subtitle,
    "lat" to point.lat,
    "lng" to point.lng,
)

private fun Map<*, *>.toPlaceSuggestion(): PlaceSuggestion? {
    val point = toGeoPoint() ?: return null
    return PlaceSuggestion(
        title = this["title"] as? String ?: return null,
        subtitle = this["subtitle"] as? String ?: "",
        point = point,
    )
}

fun FareEstimate.toFirestoreMap(): Map<String, Any?> = mapOf(
    "vehicleType" to vehicleType.name,
    "distanceKm" to distanceKm,
    "durationMin" to durationMin,
    "etaMin" to etaMin,
    "totalFare" to totalFare,
    "surgeMultiplier" to surgeMultiplier,
)

private fun Map<*, *>.toFareEstimate(): FareEstimate? {
    val vehicleType = (this["vehicleType"] as? String)?.let { runCatching { VehicleType.valueOf(it) }.getOrNull() } ?: return null
    return FareEstimate(
        vehicleType = vehicleType,
        distanceKm = (this["distanceKm"] as? Number)?.toDouble() ?: return null,
        durationMin = (this["durationMin"] as? Number)?.toInt() ?: return null,
        etaMin = (this["etaMin"] as? Number)?.toInt() ?: 0,
        totalFare = (this["totalFare"] as? Number)?.toDouble() ?: return null,
        surgeMultiplier = (this["surgeMultiplier"] as? Number)?.toDouble() ?: 1.0,
    )
}

fun Driver.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "rating" to rating,
    "totalTrips" to totalTrips,
    "vehicleNumber" to vehicleNumber,
    "vehicleModel" to vehicleModel,
    "phoneNumber" to phoneNumber,
    "photoInitials" to photoInitials,
)

private fun Map<*, *>.toDriver(): Driver? {
    return Driver(
        id = this["id"] as? String ?: return null,
        name = this["name"] as? String ?: return null,
        rating = (this["rating"] as? Number)?.toDouble() ?: return null,
        totalTrips = (this["totalTrips"] as? Number)?.toInt() ?: 0,
        vehicleNumber = this["vehicleNumber"] as? String ?: return null,
        vehicleModel = this["vehicleModel"] as? String ?: "",
        phoneNumber = this["phoneNumber"] as? String ?: return null,
        photoInitials = this["photoInitials"] as? String ?: "",
    )
}

/** Fields set when the rider first requests a ride; the dispatch simulator/driver fills in the rest. */
fun ridePlaceholderMap(
    riderId: String,
    riderName: String,
    riderPhone: String,
    vehicleType: VehicleType,
    pickup: PlaceSuggestion,
    drop: PlaceSuggestion,
    fare: FareEstimate,
    paymentMethod: PaymentMethod,
    routePolyline: List<GeoPoint>,
): Map<String, Any?> = mapOf(
    "riderId" to riderId,
    "riderName" to riderName,
    "riderPhone" to riderPhone,
    "status" to RideStatus.SEARCHING_DRIVER.name,
    "vehicleType" to vehicleType.name,
    "pickup" to pickup.toFirestoreMap(),
    "drop" to drop.toFirestoreMap(),
    "fare" to fare.toFirestoreMap(),
    "paymentMethod" to paymentMethod.name,
    "routePolyline" to routePolyline.map { it.toFirestoreMap() },
    "requestedAt" to Timestamp.now(),
)

fun Ride.Companion.fromFirestore(doc: DocumentSnapshot): Ride? {
    if (!doc.exists()) return null
    val status = (doc.getString("status"))?.let { runCatching { RideStatus.valueOf(it) }.getOrNull() } ?: return null
    val vehicleType = (doc.getString("vehicleType"))?.let { runCatching { VehicleType.valueOf(it) }.getOrNull() } ?: return null
    val pickup = (doc.get("pickup") as? Map<*, *>)?.toPlaceSuggestion() ?: return null
    val drop = (doc.get("drop") as? Map<*, *>)?.toPlaceSuggestion() ?: return null
    val fare = (doc.get("fare") as? Map<*, *>)?.toFareEstimate() ?: return null
    val paymentMethod = (doc.getString("paymentMethod"))?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() } ?: PaymentMethod.CASH

    return Ride(
        id = doc.id,
        status = status,
        vehicleType = vehicleType,
        pickup = pickup,
        drop = drop,
        fare = fare,
        paymentMethod = paymentMethod,
        routePolyline = (doc.get("routePolyline") as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.toGeoPoint() } ?: emptyList(),
        driverId = doc.getString("driverId"),
        driver = (doc.get("driver") as? Map<*, *>)?.toDriver(),
        startOtp = doc.getString("startOtp"),
        driverLocation = (doc.get("driverLocation") as? Map<*, *>)?.toGeoPoint(),
        requestedAtMillis = doc.getTimestamp("requestedAt")?.toDate()?.time ?: System.currentTimeMillis(),
        completedAtMillis = doc.getTimestamp("completedAt")?.toDate()?.time,
        finalFare = doc.getDouble("finalFare"),
        riderRating = doc.getLong("riderRating")?.toInt(),
        riderName = doc.getString("riderName") ?: "",
        riderPhone = doc.getString("riderPhone") ?: "",
    )
}
