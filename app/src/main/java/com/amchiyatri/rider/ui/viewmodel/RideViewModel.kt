package com.amchiyatri.rider.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.model.FareBreakdownLine
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.repository.FareRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import com.amchiyatri.rider.data.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val profileRepository: ProfileRepository,
    private val fareRepository: FareRepository,
) : ViewModel() {

    val activeRide: StateFlow<Ride?> = rideRepository.activeRide
    val rideHistory: StateFlow<List<Ride>> = rideRepository.rideHistory
    val emergencyContacts get() = profileRepository.profile.value?.emergencyContacts.orEmpty()

    fun fareBreakdown(fare: FareEstimate): List<FareBreakdownLine> = fareRepository.breakdown(fare)

    fun cancelRide(reason: String) = rideRepository.cancelRide(reason)

    fun clearActiveRide() = rideRepository.clearActiveRide()

    fun submitRating(stars: Int, tipAmount: Double, feedbackTags: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            rideRepository.submitRating(stars, tipAmount, feedbackTags)
            onDone()
        }
    }

    fun rideById(rideId: String): Ride? = rideHistory.value.firstOrNull { it.id == rideId }
}
