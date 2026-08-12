package com.amchiyatri.rider.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Place search + "my current location".
 *
 * [GoogleLocationRepository] is the real implementation: Fused Location Provider for the
 * rider's live position, Places Autocomplete + Place Details for search, and Android's Geocoder
 * for reverse-geocoding "current location" into a readable address. [FakeLocationRepository]
 * remains for offline development / before you've added a Maps API key - see SETUP.md.
 */
interface LocationRepository {
    /** Rider's live position. Starts at a Mumbai default until [startLocationUpdates] reports a fix. */
    val currentLocation: StateFlow<GeoPoint>

    val recentSearches: StateFlow<List<PlaceSuggestion>>

    suspend fun search(query: String): List<PlaceSuggestion>

    fun rememberRecent(place: PlaceSuggestion)

    suspend fun reverseGeocodeCurrentLocation(): PlaceSuggestion

    /** Starts live GPS updates. No-ops until ACCESS_FINE_LOCATION is granted; safe to call repeatedly. */
    fun startLocationUpdates()

    fun stopLocationUpdates()
}

@Singleton
class GoogleLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val placesClient: PlacesClient,
) : LocationRepository {

    private val _currentLocation = MutableStateFlow(GeoPoint(19.0760, 72.8777)) // Mumbai (CST) default
    override val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    override val recentSearches: StateFlow<List<PlaceSuggestion>> = _recentSearches.asStateFlow()

    private var locationCallback: LocationCallback? = null

    // Mumbai metropolitan region, used to bias/restrict autocomplete results.
    private val mumbaiBounds = RectangularBounds.newInstance(
        LatLng(18.85, 72.75),
        LatLng(19.30, 73.05),
    )

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    override fun startLocationUpdates() {
        if (!hasLocationPermission() || locationCallback != null) return

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { _currentLocation.value = GeoPoint(it.latitude, it.longitude) }
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { _currentLocation.value = GeoPoint(it.latitude, it.longitude) }
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L).build()
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            locationCallback = null
        }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    override suspend fun search(query: String): List<PlaceSuggestion> {
        if (query.isBlank()) return emptyList()

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(AutocompleteSessionToken.newInstance())
            .setLocationRestriction(mumbaiBounds)
            .setQuery(query)
            .build()

        val predictions = runCatching {
            placesClient.findAutocompletePredictions(request).await().autocompletePredictions
        }.getOrElse { return emptyList() }.take(6)

        // Autocomplete predictions don't carry lat/lng - resolve each with a Place Details call,
        // in parallel, so one search doesn't take N sequential round-trips.
        return coroutineScope {
            predictions.map { prediction ->
                async {
                    val place = runCatching {
                        placesClient.fetchPlace(
                            FetchPlaceRequest.newInstance(prediction.placeId, listOf(Place.Field.LAT_LNG)),
                        ).await().place
                    }.getOrNull()
                    val point = place?.latLng?.let { GeoPoint(it.latitude, it.longitude) } ?: return@async null
                    PlaceSuggestion(
                        title = prediction.getPrimaryText(null).toString(),
                        subtitle = prediction.getSecondaryText(null).toString(),
                        point = point,
                    )
                }
            }.awaitAll().filterNotNull()
        }
    }

    override fun rememberRecent(place: PlaceSuggestion) {
        val recent = place.copy(isRecent = true)
        _recentSearches.value = (listOf(recent) + _recentSearches.value.filterNot { it.title == place.title }).take(5)
    }

    override suspend fun reverseGeocodeCurrentLocation(): PlaceSuggestion = withContext(Dispatchers.IO) {
        val point = _currentLocation.value
        val address = runCatching {
            @Suppress("DEPRECATION") // The async Geocoder overload needs API 33+; this still works fine below that.
            Geocoder(context, Locale.getDefault()).getFromLocation(point.lat, point.lng, 1)?.firstOrNull()
        }.getOrNull()

        PlaceSuggestion(
            title = "Current location",
            subtitle = address?.getAddressLine(0) ?: "Mumbai",
            point = point,
        )
    }
}

@Singleton
class FakeLocationRepository @Inject constructor() : LocationRepository {

    private val _currentLocation = MutableStateFlow(GeoPoint(19.0596, 72.8295)) // Bandra West
    override val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    override val recentSearches: StateFlow<List<PlaceSuggestion>> = _recentSearches.asStateFlow()

    private val mumbaiLandmarks = listOf(
        PlaceSuggestion(title = "Chhatrapati Shivaji Maharaj Terminus", subtitle = "Fort, Mumbai", point = GeoPoint(18.9398, 72.8355)),
        PlaceSuggestion(title = "Gateway of India", subtitle = "Apollo Bandar, Colaba", point = GeoPoint(18.9220, 72.8347)),
        PlaceSuggestion(title = "Chhatrapati Shivaji Maharaj Int'l Airport - T2", subtitle = "Andheri East", point = GeoPoint(19.0896, 72.8656)),
        PlaceSuggestion(title = "Bandra-Kurla Complex (BKC)", subtitle = "Bandra East", point = GeoPoint(19.0669, 72.8679)),
        PlaceSuggestion(title = "Marine Drive", subtitle = "Netaji Subhash Chandra Bose Rd", point = GeoPoint(18.9440, 72.8236)),
        PlaceSuggestion(title = "Bandra-Worli Sea Link", subtitle = "Western Express Hwy", point = GeoPoint(19.0330, 72.8203)),
        PlaceSuggestion(title = "Juhu Beach", subtitle = "Juhu, Mumbai", point = GeoPoint(19.0990, 72.8265)),
        PlaceSuggestion(title = "Andheri Station", subtitle = "Andheri West", point = GeoPoint(19.1197, 72.8464)),
        PlaceSuggestion(title = "Powai Lake", subtitle = "Powai, Mumbai", point = GeoPoint(19.1234, 72.9058)),
        PlaceSuggestion(title = "Dadar Station", subtitle = "Dadar West", point = GeoPoint(19.0186, 72.8440)),
        PlaceSuggestion(title = "Lower Parel", subtitle = "Lower Parel, Mumbai", point = GeoPoint(19.0018, 72.8302)),
        PlaceSuggestion(title = "Colaba Causeway", subtitle = "Colaba, Mumbai", point = GeoPoint(18.9151, 72.8258)),
        PlaceSuggestion(title = "Worli Sea Face", subtitle = "Worli, Mumbai", point = GeoPoint(19.0176, 72.8168)),
        PlaceSuggestion(title = "Ghatkopar Station", subtitle = "Ghatkopar West", point = GeoPoint(19.0864, 72.9081)),
        PlaceSuggestion(title = "Thane Station", subtitle = "Thane West", point = GeoPoint(19.1868, 72.9750)),
        PlaceSuggestion(title = "Vashi Station", subtitle = "Vashi, Navi Mumbai", point = GeoPoint(19.0770, 72.9986)),
        PlaceSuggestion(title = "Borivali National Park", subtitle = "Sanjay Gandhi National Park", point = GeoPoint(19.2147, 72.9107)),
        PlaceSuggestion(title = "Versova", subtitle = "Versova, Andheri West", point = GeoPoint(19.1317, 72.8137)),
    )

    override suspend fun search(query: String): List<PlaceSuggestion> {
        delay(300)
        if (query.isBlank()) return mumbaiLandmarks.take(6)
        return mumbaiLandmarks.filter {
            it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
        }
    }

    override fun rememberRecent(place: PlaceSuggestion) {
        val recent = place.copy(isRecent = true)
        _recentSearches.value = (listOf(recent) + _recentSearches.value.filterNot { it.title == place.title }).take(5)
    }

    override suspend fun reverseGeocodeCurrentLocation(): PlaceSuggestion {
        val point = _currentLocation.value
        return PlaceSuggestion(
            title = "Current location",
            subtitle = "Bandra West, Mumbai",
            point = point,
        )
    }

    override fun startLocationUpdates() = Unit

    override fun stopLocationUpdates() = Unit
}
