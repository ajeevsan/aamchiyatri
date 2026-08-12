package com.amchiyatri.rider.ui.screens.ride

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.Driver
import com.amchiyatri.rider.data.model.EmergencyContact
import com.amchiyatri.rider.data.model.RideStatus
import com.amchiyatri.rider.ui.components.AmchiYatriMap
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.components.SecondaryButton
import com.amchiyatri.rider.ui.viewmodel.RideViewModel
import com.amchiyatri.rider.util.dialNumber
import com.amchiyatri.rider.util.smsIntent

@Composable
fun RideTrackingScreen(
    onNoDriverFound: () -> Unit,
    onCancelled: () -> Unit,
    onCompleted: () -> Unit,
    rideViewModel: RideViewModel,
) {
    val ride by rideViewModel.activeRide.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ride?.status) {
        when (ride?.status) {
            RideStatus.NO_DRIVER_FOUND -> onNoDriverFound()
            RideStatus.CANCELLED -> onCancelled()
            RideStatus.COMPLETED -> onCompleted()
            else -> Unit
        }
    }

    val currentRide = ride ?: return

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                AmchiYatriMap(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    pickup = currentRide.pickup.point,
                    drop = if (currentRide.status == RideStatus.ON_TRIP) currentRide.drop.point else null,
                    driver = currentRide.driverLocation,
                    routePoints = currentRide.routePolyline,
                )

                Surface(tonalElevation = 4.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        when (currentRide.status) {
                            RideStatus.SEARCHING_DRIVER -> SearchingDriverContent(onCancel = { showCancelDialog = true })
                            RideStatus.DRIVER_ASSIGNED,
                            RideStatus.DRIVER_ARRIVING,
                            RideStatus.DRIVER_ARRIVED,
                            RideStatus.ON_TRIP -> DriverAssignedContent(
                                status = currentRide.status,
                                driver = currentRide.driver,
                                otp = currentRide.startOtp,
                                destinationTitle = currentRide.drop.title,
                                onCancel = { showCancelDialog = true },
                            )
                            else -> Unit
                        }
                    }
                }
            }

            if (currentRide.status != RideStatus.SEARCHING_DRIVER) {
                FloatingActionButton(
                    onClick = { showSosDialog = true },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                ) {
                    Icon(Icons.Filled.Sos, contentDescription = "Emergency SOS", tint = Color.White)
                }
            }
        }
    }

    if (showCancelDialog) {
        CancelRideDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                showCancelDialog = false
                rideViewModel.cancelRide(reason)
            },
        )
    }

    if (showSosDialog) {
        SosDialog(
            emergencyContacts = rideViewModel.emergencyContacts,
            onDismiss = { showSosDialog = false },
        )
    }
}

@Composable
private fun SearchingDriverContent(onCancel: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Finding you a nearby driver…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("This usually takes under a minute", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    SecondaryButton(text = "Cancel search", onClick = onCancel)
}

@Composable
private fun DriverAssignedContent(
    status: RideStatus,
    driver: Driver?,
    otp: String?,
    destinationTitle: String,
    onCancel: () -> Unit,
) {
    if (driver == null) return
    val context = LocalContext.current

    val statusLine = when (status) {
        RideStatus.DRIVER_ASSIGNED -> "${driver.name.substringBefore(" ")} is on the way"
        RideStatus.DRIVER_ARRIVING -> "Arriving at your pickup point"
        RideStatus.DRIVER_ARRIVED -> "Your driver has arrived"
        RideStatus.ON_TRIP -> "On the way to $destinationTitle"
        else -> ""
    }

    Text(statusLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(driver.photoInitials, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(driver.name, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(" ${driver.rating}  ·  ${driver.vehicleNumber}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        FloatingActionButton(
            onClick = { context.dialNumber(driver.phoneNumber) },
            modifier = Modifier.padding(end = 8.dp),
        ) { Icon(Icons.Filled.Call, contentDescription = "Call driver") }
        FloatingActionButton(onClick = { context.startActivity(smsIntent(driver.phoneNumber)) }) {
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message driver")
        }
    }

    if (status == RideStatus.DRIVER_ASSIGNED || status == RideStatus.DRIVER_ARRIVING) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Share this OTP with your driver to start the ride", style = MaterialTheme.typography.bodyMedium)
                Text(otp.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    if (status != RideStatus.ON_TRIP) {
        SecondaryButton(text = "Cancel ride", onClick = onCancel)
    }
}

@Composable
private fun CancelRideDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf(
        "Driver is taking too long",
        "Booked by mistake",
        "Change of plans",
        "Found another ride",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this ride?") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(reason) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep ride") } },
    )
}

@Composable
private fun SosDialog(emergencyContacts: List<EmergencyContact>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency assistance") },
        text = {
            Column {
                Text(
                    "Your live location and trip details can be shared instantly. Use this only in a genuine emergency.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PrimaryButton(text = "Call Police (112)", onClick = { context.dialNumber("112"); onDismiss() })
                Spacer(modifier = Modifier.height(8.dp))
                emergencyContacts.forEach { contact ->
                    SecondaryButton(
                        text = "Call ${contact.name}",
                        onClick = { context.dialNumber(contact.phoneNumber); onDismiss() },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (emergencyContacts.isEmpty()) {
                    Text(
                        "Add emergency contacts from Profile to reach them here in one tap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
