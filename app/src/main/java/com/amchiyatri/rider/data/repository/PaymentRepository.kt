package com.amchiyatri.rider.data.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class RazorpayOrder(
    val orderId: String,
    val amountPaise: Long,
    val currency: String,
    val keyId: String,
)

/**
 * UPI/card/wallet payments via Razorpay. Order creation and signature verification both happen
 * in Cloud Functions (functions/payments.js) - the secret key must never be embedded in the app,
 * so this repository is a thin client over two `onCall` functions.
 *
 * Cash payments never touch this at all; see FareSummaryScreen.
 */
interface PaymentRepository {
    suspend fun createOrder(rideId: String, amountRupees: Double): Result<RazorpayOrder>
    suspend fun verifyPayment(rideId: String, orderId: String, paymentId: String, signature: String): Result<Unit>
}

@Singleton
class RazorpayPaymentRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : PaymentRepository {

    override suspend fun createOrder(rideId: String, amountRupees: Double): Result<RazorpayOrder> = runCatching {
        val data = mapOf("rideId" to rideId, "amountRupees" to amountRupees)
        val result = functions.getHttpsCallable("createRazorpayOrder").call(data).await()
        @Suppress("UNCHECKED_CAST")
        val map = result.data as Map<String, Any?>
        RazorpayOrder(
            orderId = map["orderId"] as String,
            amountPaise = (map["amountPaise"] as Number).toLong(),
            currency = map["currency"] as String,
            keyId = map["keyId"] as String,
        )
    }

    override suspend fun verifyPayment(rideId: String, orderId: String, paymentId: String, signature: String): Result<Unit> = runCatching {
        val data = mapOf(
            "rideId" to rideId,
            "razorpayOrderId" to orderId,
            "razorpayPaymentId" to paymentId,
            "razorpaySignature" to signature,
        )
        functions.getHttpsCallable("verifyRazorpayPayment").call(data).await()
        Unit
    }
}

/** Offline dev fallback: "succeeds" instantly with a fake order, no gateway involved. */
@Singleton
class FakePaymentRepository @Inject constructor() : PaymentRepository {

    override suspend fun createOrder(rideId: String, amountRupees: Double): Result<RazorpayOrder> {
        delay(400)
        return Result.success(
            RazorpayOrder(
                orderId = "order_fake${Random.nextInt(100000, 999999)}",
                amountPaise = (amountRupees * 100).toLong(),
                currency = "INR",
                keyId = "rzp_test_fake",
            ),
        )
    }

    override suspend fun verifyPayment(rideId: String, orderId: String, paymentId: String, signature: String): Result<Unit> {
        delay(300)
        return Result.success(Unit)
    }
}
