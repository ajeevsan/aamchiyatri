package com.amchiyatri.rider.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.amchiyatri.rider.ui.components.MockMap
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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MockMap(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                pickup = ride.pickup.point,
                drop = ride.drop.point,
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
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total paid", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "₹${(ride.finalFare ?: ride.fare.totalFare).toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "${ride.vehicleType.displayName} · ${ride.fare.distanceKm} km · Paid via ${ride.paymentMethod.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
