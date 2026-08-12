package com.amchiyatri.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.amchiyatri.rider.ui.navigation.AmchiYatriNavGraph
import com.amchiyatri.rider.ui.theme.AmchiYatriTheme
import com.amchiyatri.rider.util.RazorpayResultBridge
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject lateinit var razorpayResultBridge: RazorpayResultBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmchiYatriTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmchiYatriNavGraph()
                }
            }
        }
    }

    // Razorpay Checkout requires the hosting Activity to implement this directly - there's no
    // ActivityResultContract for it. We just forward the result into RazorpayResultBridge so the
    // Compose side (PaymentViewModel) can react to it like any other flow.
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        razorpayResultBridge.emitSuccess(
            orderId = paymentData?.orderId.orEmpty(),
            paymentId = razorpayPaymentId.orEmpty(),
            signature = paymentData?.signature.orEmpty(),
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        razorpayResultBridge.emitError(code, response)
    }
}
