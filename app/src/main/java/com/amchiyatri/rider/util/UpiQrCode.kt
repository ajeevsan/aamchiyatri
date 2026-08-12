package com.amchiyatri.rider.util

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLEncoder
import java.util.Locale

/**
 * Builds a standard NPCI UPI deep link ("upi://pay?...") and renders it as a QR bitmap - the same
 * mechanism every "scan to pay" counter in India uses, and the approach in
 * https://github.com/Dhruvil45/upiqr (which targets JS; this is the same parameter scheme ported
 * to Kotlin/ZXing, since that library isn't usable from an Android/Gradle build directly).
 *
 * No payment gateway or merchant account involved: the money moves bank-to-bank via UPI itself.
 * The trade-off is there's no server-side proof of payment the way Razorpay's signature gives you
 * - see PaymentViewModel's manual-confirmation fallback.
 */
object UpiPayment {

    /**
     * @param payeeVpa the receiving UPI ID (`pa`) - e.g. "yourname@upi". There's no per-driver
     *   payout without a real payment aggregator, so every ride pays this one fixed VPA; see
     *   SETUP.md.
     */
    fun buildUri(
        payeeVpa: String,
        payeeName: String,
        amountRupees: Double,
        transactionRef: String,
        transactionNote: String,
        currency: String = "INR",
    ): String {
        fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
        val amount = String.format(Locale.US, "%.2f", amountRupees)
        return "upi://pay?pa=${enc(payeeVpa)}&pn=${enc(payeeName)}&am=$amount&cu=$currency" +
            "&tr=${enc(transactionRef)}&tn=${enc(transactionNote)}"
    }

    /** Renders [content] (typically the URI from [buildUri]) as a black-on-white QR bitmap. */
    fun toQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * UPI apps that bother to return a result put it in the launched Activity's result extras,
     * but every app uses different key names (`Status`, `response`, `txnStatus`, ...) and some
     * return nothing at all. Rather than guess one key, flatten everything that came back into a
     * single string PaymentViewModel can substring-search for SUCCESS/FAILURE - null (not just
     * empty) means "nothing usable came back," which is the signal to fall back to asking the
     * rider directly.
     */
    @Suppress("DEPRECATION") // Bundle.get(key) is untyped-deprecated; there's no typed replacement when the value's type is unknown.
    fun describeActivityResult(data: Intent?): String? {
        val extras = data?.extras ?: return null
        return extras.keySet()
            .joinToString("&") { key -> "$key=${extras.get(key)}" }
            .ifBlank { null }
    }
}
