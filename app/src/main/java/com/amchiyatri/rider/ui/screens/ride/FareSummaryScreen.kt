package com.amchiyatri.rider.ui.screens.ride

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.components.SecondaryButton
import com.amchiyatri.rider.ui.viewmodel.PaymentStatus
import com.amchiyatri.rider.ui.viewmodel.PaymentViewModel
import com.amchiyatri.rider.ui.viewmodel.ProfileViewModel
import com.amchiyatri.rider.ui.viewmodel.RideViewModel

@Composable
fun FareSummaryScreen(
    onContinueToRating: () -> Unit,
    rideViewModel: RideViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel(),
) {
    val ride by rideViewModel.activeRide.collectAsState()
    val currentRide = ride ?: return
    val breakdown = rideViewModel.fareBreakdown(currentRide.fare)
    val profile by profileViewModel.profile.collectAsState()
    val paymentState = paymentViewModel.uiState
    val activity = LocalContext.current as Activity
    val amount = currentRide.finalFare ?: currentRide.fare.totalFare

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

            Spacer(modifier = Modifier.weight(1f))

            if (currentRide.paymentMethod == PaymentMethod.CASH) {
                Text(
                    "Pay the driver directly, then continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PrimaryButton(text = "Rate your trip", onClick = onContinueToRating)
            } else {
                when (paymentState.status) {
                    PaymentStatus.IDLE, PaymentStatus.PROCESSING -> {
                        PrimaryButton(
                            text = "Pay ₹${amount.toInt()}",
                            isLoading = paymentState.status == PaymentStatus.PROCESSING,
                        ) {
                            paymentViewModel.startPayment(activity, currentRide.id, amount, profile?.phoneNumber.orEmpty())
                        }
                    }
                    PaymentStatus.FAILED -> {
                        Text(
                            paymentState.errorMessage ?: "Payment failed",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        SecondaryButton(text = "Try again") {
                            paymentViewModel.startPayment(activity, currentRide.id, amount, profile?.phoneNumber.orEmpty())
                        }
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
        }
    }
}
