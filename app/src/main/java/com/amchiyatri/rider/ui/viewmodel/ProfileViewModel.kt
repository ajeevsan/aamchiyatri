package com.amchiyatri.rider.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.model.Gender
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.data.model.UserProfile
import com.amchiyatri.rider.data.repository.AuthRepository
import com.amchiyatri.rider.data.repository.LocationRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = profileRepository.profile

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> = locationRepository.search(query)

    fun updateBasicInfo(name: String, email: String?, gender: Gender?) {
        viewModelScope.launch { profileRepository.updateBasicInfo(name, email, gender) }
    }

    fun addEmergencyContact(name: String, phoneNumber: String) {
        viewModelScope.launch { profileRepository.addEmergencyContact(name, phoneNumber) }
    }

    fun removeEmergencyContact(contactId: String) {
        viewModelScope.launch { profileRepository.removeEmergencyContact(contactId) }
    }

    fun addSavedPlace(label: SavedPlaceLabel, customName: String?, address: String, point: GeoPoint) {
        viewModelScope.launch { profileRepository.addSavedPlace(label, customName, address, point) }
    }

    fun removeSavedPlace(placeId: String) {
        viewModelScope.launch { profileRepository.removeSavedPlace(placeId) }
    }

    fun logout() = authRepository.logout()
}
