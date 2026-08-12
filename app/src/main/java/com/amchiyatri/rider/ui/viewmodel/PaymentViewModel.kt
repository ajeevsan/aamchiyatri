package com.amchiyatri.rider.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PaymentStatus {
    IDLE,
    /** QR/intent ready; waiting for the rider to actually pay in their UPI app. */
    AWAITING_PAYMENT,
    /** The UPI app didn't return a parseable result - ask the rider to confirm themselves. */
    NEEDS_CONFIRMATION,
    SUCCEEDED,
    FAILED,
}

data class PaymentUiState(
    val status: PaymentStatus = PaymentStatus.IDLE,
    val upiUri: String? = null,
    val qrBitmap: Bitmap? = null,
    val errorMessage: String? = null,
)

/**
 * Drives the UPI payment flow from FareSummaryScreen: build the request (QR + intent URI), let
 * the rider either scan the QR or tap through to their own UPI app, then reconcile whatever comes
 * back. Cash payments never go through here.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    var uiState by mutableStateOf(PaymentUiState())
        private set

    fun preparePayment(rideId: String, amountRupees: Double) {
        val request = paymentRepository.preparePayment(rideId, amountRupees)
        uiState = PaymentUiState(
            status = PaymentStatus.AWAITING_PAYMENT,
            upiUri = request.upiUri,
            qrBitmap = request.qrBitmap,
        )
    }

    /**
     * Called with whatever the UPI app handed back after [android.content.Intent.ACTION_VIEW] on
     * the UPI uri returns (see FareSummaryScreen). Not every UPI app returns a parseable result,
     * so a null/unrecognised response falls back to asking the rider directly rather than
     * silently failing.
     */
    fun onUpiAppResult(rideId: String, rawResponse: String?) {
        when {
            rawResponse == null -> uiState = uiState.copy(status = PaymentStatus.NEEDS_CONFIRMATION)
            rawResponse.contains("SUCCESS", ignoreCase = true) -> confirmOutcome(rideId, succeeded = true, rawResponse)
            rawResponse.contains("FAILURE", ignoreCase = true) || rawResponse.contains("FAILED", ignoreCase = true) ->
                confirmOutcome(rideId, succeeded = false, rawResponse)
            else -> uiState = uiState.copy(status = PaymentStatus.NEEDS_CONFIRMATION, errorMessage = rawResponse)
        }
    }

    /** The rider's own "yes I paid" / "no it failed" answer, used when the UPI app gave no usable result. */
    fun confirmOutcome(rideId: String, succeeded: Boolean, rawResponse: String? = null) {
        viewModelScope.launch {
            paymentRepository.recordPaymentOutcome(rideId, succeeded, rawResponse)
                .onSuccess { uiState = uiState.copy(status = if (succeeded) PaymentStatus.SUCCEEDED else PaymentStatus.FAILED) }
                .onFailure { error -> uiState = uiState.copy(status = PaymentStatus.FAILED, errorMessage = error.message) }
        }
    }

    fun reset() {
        uiState = PaymentUiState()
    }
}
