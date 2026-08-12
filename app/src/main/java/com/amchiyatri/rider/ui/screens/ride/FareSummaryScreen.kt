package com.amchiyatri.rider.ui.screens.ride

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.components.SecondaryButton
import com.amchiyatri.rider.ui.viewmodel.PaymentStatus
import com.amchiyatri.rider.ui.viewmodel.PaymentViewModel
import com.amchiyatri.rider.ui.viewmodel.RideViewModel
import com.amchiyatri.rider.util.UpiPayment

@Composable
fun FareSummaryScreen(
    onContinueToRating: () -> Unit,
    rideViewModel: RideViewModel,
    paymentViewModel: PaymentViewModel = hiltViewModel(),
) {
    val ride by rideViewModel.activeRide.collectAsState()
    val currentRide = ride ?: return
    val breakdown = rideViewModel.fareBreakdown(currentRide.fare)
    val paymentState = paymentViewModel.uiState
    val amount = currentRide.finalFare ?: currentRide.fare.totalFare

    val upiAppLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        paymentViewModel.onUpiAppResult(currentRide.id, UpiPayment.describeActivityResult(result.data))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Trip completed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${currentRide.pickup.title}  →  ${currentRide.drop.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    breakdown.forEach { line ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(line.label, style = MaterialTheme.typography.bodyLarge)
                            Text("₹${line.amount.toInt()}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total fare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "₹${amount.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Pay via ${currentRide.paymentMethod.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (currentRide.paymentMethod == PaymentMethod.CASH) {
                Text(
                    "Pay the driver directly, then continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PrimaryButton(text = "Rate your trip", onClick = onContinueToRating)
            } else {
                UpiPaymentSection(
                    status = paymentState.status,
                    qrBitmap = paymentState.qrBitmap,
                    errorMessage = paymentState.errorMessage,
                    onStartPayment = { paymentViewModel.preparePayment(currentRide.id, amount) },
                    onOpenUpiApp = {
                        val uri = paymentState.upiUri ?: return@UpiPaymentSection
                        try {
                            upiAppLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        } catch (e: ActivityNotFoundException) {
                            paymentViewModel.onUpiAppResult(currentRide.id, null)
                        }
                    },
                    onConfirm = { succeeded -> paymentViewModel.confirmOutcome(currentRide.id, succeeded) },
                    onRetry = { paymentViewModel.preparePayment(currentRide.id, amount) },
                    onContinueToRating = onContinueToRating,
                )
            }
        }
    }
}

@Composable
private fun UpiPaymentSection(
    status: PaymentStatus,
    qrBitmap: Bitmap?,
    errorMessage: String?,
    onStartPayment: () -> Unit,
    onOpenUpiApp: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onContinueToRating: () -> Unit,
) = Column {
    when (status) {
        PaymentStatus.IDLE -> {
            PrimaryButton(text = "Pay with UPI", onClick = onStartPayment)
        }

        PaymentStatus.AWAITING_PAYMENT -> {
            Text(
                "Scan this QR in any UPI app, or tap below to pay from this device.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            qrBitmap?.let {
                Image(
                    bitmap = remember(it) { it.asImageBitmap() },
                    contentDescription = "UPI payment QR code",
                    modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Pay with a UPI app", onClick = onOpenUpiApp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Already paid? Confirm below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            ManualConfirmRow(onConfirm = onConfirm)
        }

        PaymentStatus.NEEDS_CONFIRMATION -> {
            Text(
                "We couldn't confirm the payment automatically. Did it go through?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            ManualConfirmRow(onConfirm = onConfirm)
        }

        PaymentStatus.FAILED -> {
            Text(
                errorMessage ?: "Payment didn't go through",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SecondaryButton(text = "Try again", onClick = onRetry)
        }

        PaymentStatus.SUCCEEDED -> {
            Text(
                "Payment received",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PrimaryButton(text = "Rate your trip", onClick = onContinueToRating)
        }
    }
}

@Composable
private fun ManualConfirmRow(onConfirm: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(text = "Yes, paid", modifier = Modifier.weight(1f), onClick = { onConfirm(true) })
        SecondaryButton(text = "It failed", modifier = Modifier.weight(1f), onClick = { onConfirm(false) })
    }
}
