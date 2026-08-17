package com.amchiyatri.rider.ui.screens.ride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.RideViewModel

private val positiveTags = listOf("Great driving", "On time", "Clean vehicle", "Friendly", "Followed route")
private val negativeTags = listOf("Late pickup", "Rash driving", "Unclean vehicle", "Rude behaviour", "Took long route")

@Composable
fun RateDriverScreen(
    onDone: () -> Unit,
    rideViewModel: RideViewModel,
) {
    val ride by rideViewModel.activeRide.collectAsState()
    val currentRide = ride ?: return

    var stars by remember { mutableStateOf(0) }
    var tip by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateOf(setOf<String>()) }
    val tagOptions = if (stars >= 4) positiveTags else negativeTags

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Rate your trip", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(end = 12.dp)) {
                    Text(
                        currentRide.driver?.photoInitials ?: "?",
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text(currentRide.driver?.name ?: "Your driver", style = MaterialTheme.typography.titleMedium)
                    Text(currentRide.driver?.vehicleNumber.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                (1..5).forEach { star ->
                    IconButton(onClick = { stars = star; selectedTags.value = emptySet() }) {
                        Icon(
                            // Icons.Outlined.Star is (as of material-icons-core 1.6.8) the exact
                            // same solid path as Icons.Filled.Star, not a hollow outline - the real
                            // hollow star lives under a different icon name, StarBorder.
                            imageVector = if (star <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "$star star",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (stars > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow {
                    items(tagOptions) { tag ->
                        FilterChip(
                            selected = tag in selectedTags.value,
                            onClick = {
                                selectedTags.value = if (tag in selectedTags.value) selectedTags.value - tag else selectedTags.value + tag
                            },
                            label = { Text(tag) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }

            if (stars == 5) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Add a tip?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf("0", "10", "20", "50").forEach { amount ->
                        Card(
                            onClick = { tip = amount },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tip == amount) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Text(
                                if (amount == "0") "No tip" else "₹$amount",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = tip.filter { it.isDigit() },
                onValueChange = { tip = it },
                label = { Text("Or enter a custom tip amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Submit") {
                val tipAmount = tip.toDoubleOrNull() ?: 0.0
                rideViewModel.submitRating(stars, tipAmount, selectedTags.value.toList()) {
                    onDone()
                }
            }
        }
    }
}
