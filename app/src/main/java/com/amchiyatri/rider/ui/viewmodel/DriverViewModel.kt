package com.amchiyatri.rider.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.model.DriverDetails
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.UserRole
import com.amchiyatri.rider.data.model.VehicleType
import com.amchiyatri.rider.data.repository.DriverRepository
import com.amchiyatri.rider.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverOnboardingUiState(
    val vehicleType: VehicleType = VehicleType.AUTO,
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val isSaving: Boolean = false,
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = driverRepository.isOnline
    val pendingRides: StateFlow<List<Ride>> = driverRepository.pendingRides
    val activeDriverRide: StateFlow<Ride?> = driverRepository.activeDriverRide
    val activeDriverRideError: StateFlow<String?> = driverRepository.activeDriverRideError
    val profile = profileRepository.profile

    var onboardingState by mutableStateOf(DriverOnboardingUiState())
        private set
    var onboardingError by mutableStateOf<String?>(null)
        private set

    var otpEntry by mutableStateOf("")
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    fun onVehicleTypeChange(type: VehicleType) {
        onboardingState = onboardingState.copy(vehicleType = type)
    }

    fun onVehicleNumberChange(value: String) {
        onboardingState = onboardingState.copy(vehicleNumber = value.uppercase())
    }

    fun onVehicleModelChange(value: String) {
        onboardingState = onboardingState.copy(vehicleModel = value)
    }

    fun onOtpEntryChange(value: String) {
        otpEntry = value.filter { it.isDigit() }.take(4)
        actionError = null
    }

    /** Saves vehicle details, switches this account into driver mode, and calls [onDone]. */
    fun completeOnboarding(onDone: () -> Unit) {
        val state = onboardingState
        if (state.vehicleNumber.isBlank() || state.vehicleModel.isBlank()) {
            onboardingError = "Fill in your vehicle number and model"
            return
        }
        viewModelScope.launch {
            onboardingState = state.copy(isSaving = true)
            profileRepository.updateDriverDetails(
                DriverDetails(
                    vehicleType = state.vehicleType,
                    vehicleNumber = state.vehicleNumber,
                    vehicleModel = state.vehicleModel,
                ),
            )
            profileRepository.setActiveRole(UserRole.DRIVER)
            onboardingState = state.copy(isSaving = false)
            onDone()
        }
    }

    fun switchToRiderMode(onDone: () -> Unit) {
        driverRepository.goOffline()
        viewModelScope.launch {
            profileRepository.setActiveRole(UserRole.RIDER)
            onDone()
        }
    }

    fun switchToDriverMode(onNeedsOnboarding: () -> Unit, onDone: () -> Unit) {
        viewModelScope.launch {
            if (profileRepository.profile.value?.driverDetails == null) {
                onNeedsOnboarding()
            } else {
                profileRepository.setActiveRole(UserRole.DRIVER)
                onDone()
            }
        }
    }

    fun goOnline() = driverRepository.goOnline()
    fun goOffline() = driverRepository.goOffline()

    fun acceptRide(rideId: String, onAccepted: () -> Unit) {
        actionError = null
        viewModelScope.launch {
            driverRepository.acceptRide(rideId)
                .onSuccess { onAccepted() }
                .onFailure { actionError = it.message ?: "Couldn't accept this ride - it may already be taken" }
        }
    }

    fun markArrived() {
        actionError = null
        viewModelScope.launch {
            driverRepository.markArrived().onFailure { actionError = it.message }
        }
    }

    fun startTrip(onStarted: () -> Unit) {
        actionError = null
        viewModelScope.launch {
            driverRepository.startTrip(otpEntry)
                .onSuccess { onStarted() }
                .onFailure { actionError = it.message ?: "Incorrect OTP" }
        }
    }

    fun completeTrip(onCompleted: () -> Unit) {
        actionError = null
        viewModelScope.launch {
            driverRepository.completeTrip()
                .onSuccess { onCompleted() }
                .onFailure { actionError = it.message }
        }
    }

    fun leaveActiveRide() {
        otpEntry = ""
        actionError = null
        driverRepository.leaveActiveRide()
    }
}
