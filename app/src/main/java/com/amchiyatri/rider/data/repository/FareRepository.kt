package com.amchiyatri.rider.data.repository

import android.content.Context
import com.amchiyatri.rider.data.model.FareBreakdownLine
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.Route
import com.amchiyatri.rider.data.model.VehicleType
import com.amchiyatri.rider.data.remote.DirectionsApi
import com.amchiyatri.rider.util.ApiKeys
import com.amchiyatri.rider.util.decodePolyline
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Route lookup + fare calculation. Callers fetch [getRoute] once (the only network/I-O call) and
 * derive fares from it with [estimateFares], instead of hitting the routing backend once per
 * vehicle type.
 *
 * [DirectionsFareRepository] calls the real Directions API for road distance, duration and a
 * turn-by-turn polyline. [FakeFareRepository] approximates the same shape from a straight-line
 * (haversine) distance with a fixed detour factor, for offline dev / before you've added a Maps
 * API key - see SETUP.md.
 */
interface FareRepository {
    suspend fun getRoute(pickup: GeoPoint, drop: GeoPoint): Route
    fun estimateFares(route: Route): List<FareEstimate>
    fun breakdown(fare: FareEstimate): List<FareBreakdownLine>
}

private fun fareFor(vehicle: VehicleType, route: Route, surge: Double): FareEstimate {
    val rawFare = vehicle.baseFare + vehicle.perKmRate * route.distanceKm + vehicle.perMinRate * route.durationMin
    val totalFare = round(rawFare * surge / 5.0) * 5.0 // round to nearest ₹5, like most fare meters
    return FareEstimate(
        vehicleType = vehicle,
        distanceKm = round(route.distanceKm * 10) / 10.0,
        durationMin = route.durationMin,
        etaMin = vehicle.etaPaddingMin + Random.nextInt(0, 4),
        totalFare = totalFare,
        surgeMultiplier = surge,
    )
}

/** A gentle, deterministic-ish surge so longer/odd-hour-feeling trips occasionally cost a bit more. */
private fun surgeMultiplierFor(distanceKm: Double): Double {
    val chance = Random.nextDouble()
    return when {
        chance < 0.7 -> 1.0
        chance < 0.9 -> 1.15
        else -> 1.3
    }.let { base -> if (distanceKm > 15) base + 0.1 else base }
}

private fun estimateFaresFor(route: Route): List<FareEstimate> {
    val surge = surgeMultiplierFor(route.distanceKm)
    return VehicleType.entries.map { fareFor(it, route, surge) }
}

private fun breakdownFor(fare: FareEstimate): List<FareBreakdownLine> {
    val vehicle = fare.vehicleType
    val base = vehicle.baseFare
    val distanceCharge = round(vehicle.perKmRate * fare.distanceKm)
    val timeCharge = round(vehicle.perMinRate * fare.durationMin)
    val subtotal = base + distanceCharge + timeCharge
    val surgeAmount = round(fare.totalFare - subtotal)
    val lines = mutableListOf(
        FareBreakdownLine("Base fare", base),
        FareBreakdownLine("Distance (${fare.distanceKm} km)", distanceCharge),
        FareBreakdownLine("Time (${fare.durationMin} min)", timeCharge),
    )
    if (surgeAmount > 0) lines += FareBreakdownLine("High-demand adjustment", surgeAmount)
    return lines
}

private fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)

    val h = sin(dLat / 2).let { it * it } +
        cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return earthRadiusKm * c
}

private fun straightLineRoute(pickup: GeoPoint, drop: GeoPoint): Route {
    val straightLineKm = haversineKm(pickup, drop)
    val roadDistanceKm = (straightLineKm * 1.35).coerceAtLeast(0.8) // Mumbai roads rarely run straight
    val durationMin = round((roadDistanceKm / 20.0) * 60).toInt().coerceAtLeast(3) // mid-traffic average speed
    return Route(distanceKm = roadDistanceKm, durationMin = durationMin, polyline = listOf(pickup, drop))
}

@Singleton
class DirectionsFareRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directionsApi: DirectionsApi,
) : FareRepository {

    override suspend fun getRoute(pickup: GeoPoint, drop: GeoPoint): Route {
        val response = runCatching {
            directionsApi.getDirections(
                origin = "${pickup.lat},${pickup.lng}",
                destination = "${drop.lat},${drop.lng}",
                apiKey = ApiKeys.mapsApiKey(context),
            )
        }.getOrNull()

        val route = response?.routes?.firstOrNull()
        val leg = route?.legs?.firstOrNull()
        if (response?.status != "OK" || leg == null) {
            // Directions call failed (bad/missing key, no network, no route) - fall back to a
            // straight-line estimate rather than blocking the whole booking flow.
            return straightLineRoute(pickup, drop)
        }

        return Route(
            distanceKm = (leg.distance?.value ?: 0) / 1000.0,
            durationMin = ((leg.duration?.value ?: 0) / 60.0).let { round(it) }.toInt().coerceAtLeast(1),
            polyline = route.overviewPolyline?.points?.let { decodePolyline(it) } ?: listOf(pickup, drop),
        )
    }

    override fun estimateFares(route: Route): List<FareEstimate> = estimateFaresFor(route)

    override fun breakdown(fare: FareEstimate): List<FareBreakdownLine> = breakdownFor(fare)
}

@Singleton
class FakeFareRepository @Inject constructor() : FareRepository {

    override suspend fun getRoute(pickup: GeoPoint, drop: GeoPoint): Route = straightLineRoute(pickup, drop)

    override fun estimateFares(route: Route): List<FareEstimate> = estimateFaresFor(route)

    override fun breakdown(fare: FareEstimate): List<FareBreakdownLine> = breakdownFor(fare)
}
