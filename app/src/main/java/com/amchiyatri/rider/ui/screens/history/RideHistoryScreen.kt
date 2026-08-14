package com.amchiyatri.rider.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.ui.components.AmchiYatriBottomBar
import com.amchiyatri.rider.ui.components.vehicleIconFor
import com.amchiyatri.rider.ui.navigation.Destinations
import com.amchiyatri.rider.ui.viewmodel.RideViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(
    onNavigate: (String) -> Unit,
    onOpenRide: (String) -> Unit,
    rideViewModel: RideViewModel,
) {
    val history by rideViewModel.rideHistory.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your rides") }) },
        bottomBar = { AmchiYatriBottomBar(currentRoute = Destinations.RIDE_HISTORY, onNavigate = onNavigate) },
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    Text("No rides yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("Book your first ride from the Home tab", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history) { ride ->
                    RideHistoryRow(ride = ride, onClick = { onOpenRide(ride.id) })
                }
            }
        }
    }
}

@Composable
private fun RideHistoryRow(ride: Ride, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val (vehicleIcon, vehicleColor) = vehicleIconFor(ride.vehicleType)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(vehicleIcon, contentDescription = ride.vehicleType.displayName, tint = vehicleColor, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${ride.pickup.title} → ${ride.drop.title}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(dateFormat.format(Date(ride.requestedAtMillis)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                Text(statusLabel(ride.status), style = MaterialTheme.typography.bodyMedium, color = statusColor(ride.status))
            }
            // A cancelled ride was never actually charged - fare.totalFare is only ever the
            // pre-ride estimate, so showing it here would read as money that changed hands.
            if (ride.status != RideStatus.CANCELLED) {
                Text(
                    "₹${(ride.finalFare ?: ride.fare.totalFare).toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun statusLabel(status: RideStatus) = when (status) {
    RideStatus.COMPLETED -> "Completed"
    RideStatus.CANCELLED -> "Cancelled"
    else -> status.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun statusColor(status: RideStatus) = when (status) {
    RideStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    RideStatus.CANCELLED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}
