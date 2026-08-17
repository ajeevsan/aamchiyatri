package com.amchiyatri.rider.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.ui.components.AmchiYatriMap
import com.amchiyatri.rider.ui.viewmodel.RideViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    rideId: String,
    onBack: () -> Unit,
    rideViewModel: RideViewModel,
) {
    val ride = rideViewModel.rideById(rideId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (ride == null) {
            Text("Ride not found", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            AmchiYatriMap(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                pickup = ride.pickup.point,
                drop = ride.drop.point,
                routePoints = ride.routePolyline,
            )

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    SimpleDateFormat("EEEE, dd MMM yyyy · hh:mm a", Locale.getDefault()).format(Date(ride.requestedAtMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(ride.pickup.title, style = MaterialTheme.typography.bodyLarge)
                Text(ride.pickup.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text(ride.drop.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(ride.drop.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

                Spacer(modifier = Modifier.height(20.dp))
                ride.driver?.let { driver ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(driver.name, style = MaterialTheme.typography.titleMedium)
                                Text("${driver.vehicleModel} · ${driver.vehicleNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (ride.riderRating != null) {
                                Row {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(" ${ride.riderRating}/5", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                if (ride.status == RideStatus.CANCELLED) {
                    // A cancelled ride was never charged - fare.totalFare is only ever the pre-ride
                    // estimate, so showing a fare breakdown here would read as money that changed
                    // hands when none did. Show why it was cancelled instead of what it "cost".
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Ride cancelled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    ride.cancelReason?.takeIf { it.isNotBlank() } ?: "No reason given",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${ride.vehicleType.displayName} · ${ride.fare.distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            rideViewModel.fareBreakdown(ride.fare).forEach { line ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(line.label, style = MaterialTheme.typography.bodyLarge)
                                    Text("₹${line.amount.toInt()}", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            // Tip is submitted separately from the fare estimate (RateDriverScreen,
                            // after the ride's already priced) - shown here as its own line, same as
                            // any other adjustment, so "Total paid" is never an unexplained number.
                            if ((ride.tipAmount ?: 0.0) > 0) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tip", style = MaterialTheme.typography.bodyLarge)
                                    Text("₹${ride.tipAmount!!.toInt()}", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "₹${(ride.finalFare ?: ride.fare.totalFare).toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${ride.vehicleType.displayName} · ${ride.fare.distanceKm} km · Paid via ${ride.paymentMethod.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
