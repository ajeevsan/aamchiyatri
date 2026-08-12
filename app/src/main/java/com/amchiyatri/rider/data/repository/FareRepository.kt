package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.FareBreakdownLine
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.VehicleType
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Distance + fare estimation. Real distance/ETA would come from a routing API (Google Directions,
 * OSRM, Mapbox Directions); here we approximate road distance from straight-line (haversine)
 * distance with a fixed detour factor, which is good enough to make every downstream screen
 * (fare cards, trip summary) behave realistically.
 */
interface FareRepository {
    suspend fun estimateFares(pickup: GeoPoint, drop: GeoPoint): List<FareEstimate>
    fun breakdown(fare: FareEstimate): List<FareBreakdownLine>
}

@Singleton
class DefaultFareRepository @Inject constructor() : FareRepository {

    override suspend fun estimateFares(pickup: GeoPoint, drop: GeoPoint): List<FareEstimate> {
        delay(500)
        val straightLineKm = haversineKm(pickup, drop)
        // Mumbai roads rarely run straight; pad for a realistic on-road distance.
        val roadDistanceKm = (straightLineKm * 1.35).coerceAtLeast(0.8)
        val surge = surgeMultiplierFor(roadDistanceKm)

        return VehicleType.entries.map { vehicle ->
            val avgSpeedKmh = when (vehicle) {
                VehicleType.BIKE -> 24.0
                VehicleType.AUTO -> 19.0
                VehicleType.SEDAN, VehicleType.SUV -> 21.0
            }
            val durationMin = round((roadDistanceKm / avgSpeedKmh) * 60).toInt().coerceAtLeast(3)
            val rawFare = vehicle.baseFare +
                vehicle.perKmRate * roadDistanceKm +
                vehicle.perMinRate * durationMin
            val totalFare = round(rawFare * surge / 5.0) * 5.0 // round to nearest ₹5, like most fare meters

            FareEstimate(
                vehicleType = vehicle,
                distanceKm = round(roadDistanceKm * 10) / 10.0,
                durationMin = durationMin,
                etaMin = vehicle.etaPaddingMin + Random.nextInt(0, 4),
                totalFare = totalFare,
                surgeMultiplier = surge,
            )
        }
    }

    override fun breakdown(fare: FareEstimate): List<FareBreakdownLine> {
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

    /** A gentle, deterministic-ish surge so longer/odd-hour-feeling trips occasionally cost a bit more. */
    private fun surgeMultiplierFor(distanceKm: Double): Double {
        val chance = Random.nextDouble()
        return when {
            chance < 0.7 -> 1.0
            chance < 0.9 -> 1.15
            else -> 1.3
        }.let { base -> if (distanceKm > 15) base + 0.1 else base }
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
}
