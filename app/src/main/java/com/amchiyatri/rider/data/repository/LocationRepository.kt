package com.amchiyatri.rider.data.repository

import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.PlaceSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Place search + "my current location". The real implementation would call the Places
 * Autocomplete API (or an open alternative like Mapbox/OSM Nominatim) and the Fused Location
 * Provider; this fake serves a fixed list of well-known Mumbai landmarks so the whole booking
 * flow is exercised without any Maps API key.
 */
interface LocationRepository {
    /** Rider's live position, simulated near Bandra, Mumbai. */
    val currentLocation: StateFlow<GeoPoint>

    val recentSearches: StateFlow<List<PlaceSuggestion>>

    suspend fun search(query: String): List<PlaceSuggestion>

    fun rememberRecent(place: PlaceSuggestion)

    fun reverseGeocodeCurrentLocation(): PlaceSuggestion
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

    override fun reverseGeocodeCurrentLocation(): PlaceSuggestion {
        val point = _currentLocation.value
        return PlaceSuggestion(
            title = "Current location",
            subtitle = "Bandra West, Mumbai",
            point = point,
        )
    }
}
