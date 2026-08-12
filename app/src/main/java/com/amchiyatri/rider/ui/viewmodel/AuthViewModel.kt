package com.amchiyatri.rider.ui.viewmodel

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val otpSentTo: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn

    var uiState by mutableStateOf(AuthUiState())
        private set

    fun onPhoneNumberChange(value: String) {
        uiState = uiState.copy(phoneNumber = value.filter { it.isDigit() }.take(10), errorMessage = null)
    }

    fun onOtpChange(value: String) {
        uiState = uiState.copy(otp = value.filter { it.isDigit() }.take(6), errorMessage = null)
    }

    fun sendOtp(activity: Activity, onSent: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            authRepository.sendOtp(uiState.phoneNumber, activity)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false, otpSentTo = uiState.phoneNumber)
                    onSent()
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun resendOtp(activity: Activity) {
        viewModelScope.launch {
            authRepository.resendOtp(uiState.phoneNumber, activity)
        }
    }

    fun verifyOtp(onVerified: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            authRepository.verifyOtp(uiState.phoneNumber, uiState.otp)
                .onSuccess {
                    uiState = uiState.copy(isLoading = false)
                    onVerified()
                }
                .onFailure { error ->
                    uiState = uiState.copy(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun logout() = authRepository.logout()
}
