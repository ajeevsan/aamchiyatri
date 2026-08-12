package com.amchiyatri.rider.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.FareEstimate
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.ui.components.AmchiYatriMap
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.BookingViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideOptionsScreen(
    onBack: () -> Unit,
    onChangePaymentMethod: () -> Unit,
    onRideRequested: () -> Unit,
    bookingViewModel: BookingViewModel,
) {
    val state by bookingViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a ride") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChangePaymentMethod)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay with ${state.paymentMethod.displayName()}")
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary)
                }
                PrimaryButton(
                    text = state.selectedFare?.let { "Confirm ${it.vehicleType.displayName} · ₹${it.totalFare.toInt()}" } ?: "Select a ride",
                    enabled = state.readyToRequest,
                ) {
                    bookingViewModel.confirmBooking()
                    onRideRequested()
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AmchiYatriMap(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                pickup = state.pickup?.point,
                drop = state.drop?.point,
                routePoints = state.routePolyline,
            )

            RouteSummary(pickupTitle = state.pickup?.title, dropTitle = state.drop?.title)

            if (state.isLoadingFares) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(state.fareEstimates) { fare ->
                        VehicleOptionCard(
                            fare = fare,
                            selected = fare.vehicleType == state.selectedVehicle,
                            onClick = { bookingViewModel.selectVehicle(fare.vehicleType) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteSummary(pickupTitle: String?, dropTitle: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(pickupTitle ?: "Pickup", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(dropTitle ?: "Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VehicleOptionCard(fare: FareEstimate, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(fare.vehicleType.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(fare.vehicleType.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("Drops in ${fare.etaMin} min · ${fare.durationMin} min trip", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${fare.totalFare.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (fare.surgeMultiplier > 1.0) {
                    Text(
                        String.format(Locale.US, "%.1fx demand", fare.surgeMultiplier),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun PaymentMethod.displayName() = when (this) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.UPI -> "UPI"
    PaymentMethod.WALLET -> "Wallet"
}
