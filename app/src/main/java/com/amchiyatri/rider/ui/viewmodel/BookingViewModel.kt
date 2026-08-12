package com.amchiyatri.rider.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.amchiyatri.rider.data.model.SavedPlace
import com.amchiyatri.rider.data.model.VehicleType
import com.amchiyatri.rider.data.repository.FareRepository
import com.amchiyatri.rider.data.repository.LocationRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import com.amchiyatri.rider.data.repository.RideRepository
import com.amchiyatri.rider.ui.navigation.LocationField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val pickup: PlaceSuggestion? = null,
    val drop: PlaceSuggestion? = null,
    val searchQuery: String = "",
    val searchResults: List<PlaceSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val fareEstimates: List<FareEstimate> = emptyList(),
    val isLoadingFares: Boolean = false,
    val selectedVehicle: VehicleType? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
) {
    val readyToRequest: Boolean get() = pickup != null && drop != null && selectedVehicle != null
    val selectedFare: FareEstimate? get() = fareEstimates.firstOrNull { it.vehicleType == selectedVehicle }
}

/**
 * Drives the whole "where to?" -> fare estimate -> confirm flow (Home, LocationSearch,
 * RideOptions screens). Booking a ride hands off to [RideRepository], which owns everything
 * that happens after the request is placed.
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val fareRepository: FareRepository,
    private val rideRepository: RideRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    val currentLocation = locationRepository.currentLocation

    val recentSearches = locationRepository.recentSearches

    val savedPlaces: StateFlow<List<SavedPlace>> = profileRepository.profile
        .map { it?.savedPlaces.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        setPlace(LocationField.PICKUP, locationRepository.reverseGeocodeCurrentLocation())
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = locationRepository.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun setPlace(field: LocationField, place: PlaceSuggestion) {
        locationRepository.rememberRecent(place)
        _uiState.update {
            when (field) {
                LocationField.PICKUP -> it.copy(pickup = place)
                LocationField.DROP -> it.copy(drop = place)
            }
        }
        maybeLoadFares()
    }

    fun swapPickupAndDrop() {
        _uiState.update { it.copy(pickup = it.drop, drop = it.pickup) }
        maybeLoadFares()
    }

    private fun maybeLoadFares() {
        val state = _uiState.value
        val pickup = state.pickup
        val drop = state.drop
        if (pickup == null || drop == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFares = true) }
            val estimates = fareRepository.estimateFares(pickup.point, drop.point)
            _uiState.update {
                it.copy(
                    fareEstimates = estimates,
                    isLoadingFares = false,
                    selectedVehicle = it.selectedVehicle ?: estimates.firstOrNull()?.vehicleType,
                )
            }
        }
    }

    fun selectVehicle(type: VehicleType) {
        _uiState.update { it.copy(selectedVehicle = type) }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    /** Places the ride request and resets the drop/fare selection so the next booking starts clean. */
    fun confirmBooking() {
        val state = _uiState.value
        val pickup = state.pickup ?: return
        val drop = state.drop ?: return
        val fare = state.selectedFare ?: return
        rideRepository.requestRide(pickup, drop, fare, state.paymentMethod)
        _uiState.update {
            it.copy(
                drop = null,
                fareEstimates = emptyList(),
                selectedVehicle = null,
                searchQuery = "",
                searchResults = emptyList(),
            )
        }
    }
}
