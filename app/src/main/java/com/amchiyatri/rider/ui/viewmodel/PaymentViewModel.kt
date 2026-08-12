package com.amchiyatri.rider.ui.viewmodel

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.repository.PaymentRepository
import com.amchiyatri.rider.data.repository.RazorpayOrder
import com.amchiyatri.rider.util.RazorpayResult
import com.amchiyatri.rider.util.RazorpayResultBridge
import com.razorpay.Checkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

enum class PaymentStatus { IDLE, PROCESSING, SUCCEEDED, FAILED }

data class PaymentUiState(
    val status: PaymentStatus = PaymentStatus.IDLE,
    val errorMessage: String? = null,
)

/**
 * Drives the Razorpay Checkout flow from FareSummaryScreen: create an order server-side, open
 * Checkout, verify the result server-side. Cash payments never go through here.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val razorpayResultBridge: RazorpayResultBridge,
) : ViewModel() {

    var uiState by mutableStateOf(PaymentUiState())
        private set

    private var pendingRideId: String? = null

    init {
        viewModelScope.launch {
            razorpayResultBridge.results.collect { result ->
                when (result) {
                    is RazorpayResult.Success -> onCheckoutSucceeded(result.orderId, result.paymentId, result.signature)
                    is RazorpayResult.Error -> uiState = PaymentUiState(
                        status = PaymentStatus.FAILED,
                        errorMessage = result.description ?: "Payment was cancelled or failed",
                    )
                }
            }
        }
    }

    fun startPayment(activity: Activity, rideId: String, amountRupees: Double, riderPhone: String) {
        pendingRideId = rideId
        uiState = PaymentUiState(status = PaymentStatus.PROCESSING)
        viewModelScope.launch {
            paymentRepository.createOrder(rideId, amountRupees)
                .onSuccess { order -> openCheckout(activity, order, riderPhone) }
                .onFailure { error ->
                    uiState = PaymentUiState(status = PaymentStatus.FAILED, errorMessage = error.message)
                }
        }
    }

    private fun openCheckout(activity: Activity, order: RazorpayOrder, riderPhone: String) {
        val checkout = Checkout().apply { setKeyID(order.keyId) }
        val options = JSONObject().apply {
            put("name", "Amchi Yatri")
            put("description", "Ride payment")
            put("order_id", order.orderId)
            put("currency", order.currency)
            put("amount", order.amountPaise)
            put("prefill", JSONObject().apply { put("contact", riderPhone) })
        }
        runCatching { checkout.open(activity, options) }
            .onFailure { uiState = PaymentUiState(status = PaymentStatus.FAILED, errorMessage = it.message) }
    }

    private fun onCheckoutSucceeded(orderId: String, paymentId: String, signature: String) {
        val rideId = pendingRideId ?: return
        viewModelScope.launch {
            paymentRepository.verifyPayment(rideId, orderId, paymentId, signature)
                .onSuccess { uiState = PaymentUiState(status = PaymentStatus.SUCCEEDED) }
                .onFailure { error ->
                    uiState = PaymentUiState(status = PaymentStatus.FAILED, errorMessage = error.message ?: "Could not verify payment")
                }
        }
    }

    fun reset() {
        uiState = PaymentUiState()
    }
}
