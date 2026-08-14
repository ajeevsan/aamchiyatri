package com.amchiyatri.rider.ui.screens.driver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.Ride
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.ui.components.AmchiYatriMap
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.components.SecondaryButton
import com.amchiyatri.rider.ui.viewmodel.DriverViewModel
import com.amchiyatri.rider.util.dialNumber

@Composable
fun DriverActiveTripScreen(
    onDone: () -> Unit,
    driverViewModel: DriverViewModel,
) {
    val ride by driverViewModel.activeDriverRide.collectAsState()
    val error by driverViewModel.activeDriverRideError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ride?.status) {
        if (ride?.status == RideStatus.CANCELLED) {
            driverViewModel.leaveActiveRide()
            onDone()
        }
    }

    val currentRide = ride
    if (currentRide == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (error == null) {
                CircularProgressIndicator()
            } else {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
            }
        }
        return
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AmchiYatriMap(
                modifier = Modifier.fillMaxWidth().weight(1f),
                pickup = currentRide.pickup.point,
                drop = if (currentRide.status == RideStatus.ON_TRIP) currentRide.drop.point else null,
                driver = currentRide.driverLocation,
                vehicleType = currentRide.vehicleType,
                routePoints = currentRide.routePolyline,
            )

            Surface(tonalElevation = 4.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentRide.riderName.ifBlank { "Rider" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (currentRide.status == RideStatus.ON_TRIP) "Heading to ${currentRide.drop.title}" else "Pick up at ${currentRide.pickup.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        if (currentRide.riderPhone.isNotBlank()) {
                            SecondaryButton(text = "Call rider", modifier = Modifier.weight(1f)) {
                                context.dialNumber(currentRide.riderPhone)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    DriverActionSection(ride = currentRide, driverViewModel = driverViewModel, onDone = onDone)

                    driverViewModel.actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverActionSection(ride: Ride, driverViewModel: DriverViewModel, onDone: () -> Unit) {
    when (ride.status) {
        RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVING ->
            PrimaryButton(text = "I've arrived at pickup", onClick = driverViewModel::markArrived)

        RideStatus.DRIVER_ARRIVED -> {
            var isOtpFocused by remember { mutableStateOf(false) }
            Text("Ask the rider for their OTP to start the trip", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = driverViewModel.otpEntry,
                onValueChange = driverViewModel::onOtpEntryChange,
                placeholder = if (isOtpFocused) null else { { Text("4-digit OTP") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().onFocusChanged { isOtpFocused = it.isFocused },
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(text = "Start trip", enabled = driverViewModel.otpEntry.length == 4) {
                driverViewModel.startTrip {}
            }
        }

        RideStatus.ON_TRIP ->
            PrimaryButton(text = "Complete trip") {
                driverViewModel.completeTrip {
                    driverViewModel.leaveActiveRide()
                    onDone()
                }
            }

        RideStatus.COMPLETED -> {
            Text("Trip completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(text = "Back to ride requests") {
                driverViewModel.leaveActiveRide()
                onDone()
            }
        }

        else -> Unit
    }
}
