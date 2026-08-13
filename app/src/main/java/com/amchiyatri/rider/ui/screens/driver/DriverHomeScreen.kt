package com.amchiyatri.rider.ui.screens.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.ui.components.AmchiYatriBottomBar
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.navigation.Destinations
import com.amchiyatri.rider.ui.viewmodel.DriverViewModel

@Composable
fun DriverHomeScreen(
    onNavigate: (String) -> Unit,
    onRideAccepted: () -> Unit,
    driverViewModel: DriverViewModel,
) {
    val isOnline by driverViewModel.isOnline.collectAsState()
    val pendingRides by driverViewModel.pendingRides.collectAsState()
    val profile by driverViewModel.profile.collectAsState()
    val details = profile?.driverDetails

    Scaffold(
        bottomBar = { AmchiYatriBottomBar(currentRoute = Destinations.HOME, onNavigate = onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Namaste, ${profile?.name.orEmpty()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(details?.vehicleType?.displayName, details?.vehicleNumber.takeIf { !it.isNullOrBlank() })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isOnline) "Online" else "Offline", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { online -> if (online) driverViewModel.goOnline() else driverViewModel.goOffline() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                !isOnline -> EmptyState("You're offline. Go online to start seeing ride requests.")
                pendingRides.isEmpty() -> EmptyState("No ride requests nearby right now - hang tight.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pendingRides) { ride ->
                        PendingRideCard(ride = ride, onAccept = { driverViewModel.acceptRide(ride.id, onRideAccepted) })
                    }
                }
            }

            driverViewModel.actionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun PendingRideCard(ride: Ride, onAccept: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ride.pickup.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("→ ${ride.drop.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
                Text("₹${ride.fare.totalFare.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "${ride.vehicleType.displayName} · ${ride.fare.distanceKm} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PrimaryButton(text = "Accept", onClick = onAccept)
        }
    }
}
