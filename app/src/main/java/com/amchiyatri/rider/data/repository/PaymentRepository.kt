package com.amchiyatri.rider.data.repository

import android.graphics.Bitmap
import com.amchiyatri.rider.BuildConfig
import com.amchiyatri.rider.util.UpiPayment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UpiPaymentRequest(
    val upiUri: String,
    val qrBitmap: Bitmap,
)

/**
 * Direct UPI payment: no gateway, no merchant account, no Play Developer account - just a QR
 * code / UPI-app intent addressed to a single fixed VPA (`BuildConfig.UPI_PAYEE_VPA`, from
 * app/secrets.properties). See util/UpiQrCode.kt for how the request is built.
 *
 * There's no signature to verify server-side the way a real gateway gives you, so
 * [recordPaymentOutcome] just records what the UPI app (or the rider, manually) reported - see
 * PaymentViewModel for the fallback confirmation flow.
 */
interface PaymentRepository {
    fun preparePayment(rideId: String, amountRupees: Double): UpiPaymentRequest
    suspend fun recordPaymentOutcome(rideId: String, succeeded: Boolean, rawResponse: String?): Result<Unit>
}

@Singleton
class UpiPaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PaymentRepository {

    override fun preparePayment(rideId: String, amountRupees: Double): UpiPaymentRequest {
        val uri = UpiPayment.buildUri(
            payeeVpa = BuildConfig.UPI_PAYEE_VPA,
            payeeName = BuildConfig.UPI_PAYEE_NAME,
            amountRupees = amountRupees,
            transactionRef = rideId,
            transactionNote = "Amchi Yatri ride",
        )
        return UpiPaymentRequest(upiUri = uri, qrBitmap = UpiPayment.toQrBitmap(uri))
    }

    override suspend fun recordPaymentOutcome(rideId: String, succeeded: Boolean, rawResponse: String?): Result<Unit> = runCatching {
        firestore.collection("rides").document(rideId).update(
            mapOf(
                "paymentStatus" to if (succeeded) "PAID" else "FAILED",
                "upiResponse" to rawResponse,
            ),
        ).await()
    }
}

/** Offline dev fallback: builds the same real QR/URI (no network involved either way) but never touches Firestore. */
@Singleton
class FakePaymentRepository @Inject constructor() : PaymentRepository {

    override fun preparePayment(rideId: String, amountRupees: Double): UpiPaymentRequest {
        val uri = UpiPayment.buildUri(
            payeeVpa = BuildConfig.UPI_PAYEE_VPA,
            payeeName = BuildConfig.UPI_PAYEE_NAME,
            amountRupees = amountRupees,
            transactionRef = rideId,
            transactionNote = "Amchi Yatri ride (demo)",
        )
        return UpiPaymentRequest(upiUri = uri, qrBitmap = UpiPayment.toQrBitmap(uri))
    }

    override suspend fun recordPaymentOutcome(rideId: String, succeeded: Boolean, rawResponse: String?): Result<Unit> = Result.success(Unit)
}
