package com.amchiyatri.rider.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RazorpayResult {
    data class Success(val orderId: String, val paymentId: String, val signature: String) : RazorpayResult
    data class Error(val code: Int, val description: String?) : RazorpayResult
}

/**
 * Razorpay's Android SDK delivers its result via callback methods (`PaymentResultWithDataListener`)
 * that the hosting *Activity* must implement directly - there's no `ActivityResultContract` for
 * it. [MainActivity] implements that interface and forwards results here; Compose screens (via
 * PaymentViewModel) just collect [results] like any other flow, so the Activity-callback wart
 * doesn't leak past this one bridge.
 */
@Singleton
class RazorpayResultBridge @Inject constructor() {
    private val _results = MutableSharedFlow<RazorpayResult>(extraBufferCapacity = 1)
    val results: SharedFlow<RazorpayResult> = _results.asSharedFlow()

    fun emitSuccess(orderId: String, paymentId: String, signature: String) {
        _results.tryEmit(RazorpayResult.Success(orderId, paymentId, signature))
    }

    fun emitError(code: Int, description: String?) {
        _results.tryEmit(RazorpayResult.Error(code, description))
    }
}
