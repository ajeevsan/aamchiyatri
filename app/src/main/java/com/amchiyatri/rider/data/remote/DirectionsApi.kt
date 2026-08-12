package com.amchiyatri.rider.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Thin wrapper over the Directions API's plain REST endpoint (no first-party Android SDK exists). */
interface DirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("mode") mode: String = "driving",
        @Query("region") region: String = "in",
    ): DirectionsResponse
}

@Serializable
data class DirectionsResponse(
    val status: String,
    val routes: List<DirectionsRoute> = emptyList(),
)

@Serializable
data class DirectionsRoute(
    val legs: List<DirectionsLeg> = emptyList(),
    @kotlinx.serialization.SerialName("overview_polyline") val overviewPolyline: OverviewPolyline? = null,
)

@Serializable
data class DirectionsLeg(
    val distance: DirectionsValue? = null,
    val duration: DirectionsValue? = null,
)

@Serializable
data class DirectionsValue(val value: Int, val text: String)

@Serializable
data class OverviewPolyline(val points: String)
