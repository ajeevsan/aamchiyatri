package com.amchiyatri.rider.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.ui.components.AmchiYatriBottomBar
import com.amchiyatri.rider.ui.components.AmchiYatriMap
import com.amchiyatri.rider.ui.navigation.Destinations
import com.amchiyatri.rider.ui.navigation.LocationField
import com.amchiyatri.rider.ui.viewmodel.BookingViewModel

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onOpenLocationSearch: (LocationField) -> Unit,
    onGoToRideOptions: () -> Unit,
    bookingViewModel: BookingViewModel,
) {
    val state by bookingViewModel.uiState.collectAsState()
    val currentLocation by bookingViewModel.currentLocation.collectAsState()
    val recents by bookingViewModel.recentSearches.collectAsState()
    val savedPlaces by bookingViewModel.savedPlaces.collectAsState()

    Scaffold(
        bottomBar = { AmchiYatriBottomBar(currentRoute = Destinations.HOME, onNavigate = onNavigate) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // A fixed 260.dp map ate almost the entire screen height in landscape (or on any
                // short/wide aspect ratio), squeezing the booking card below it down to a sliver.
                // Splitting the space by weight instead means the map shrinks along with the
                // screen instead of claiming an absolute amount that stops fitting.
                Box(modifier = Modifier.fillMaxWidth().weight(0.38f)) {
                    AmchiYatriMap(
                        modifier = Modifier.fillMaxSize(),
                        pickup = state.pickup?.point ?: currentLocation,
                    )
                    FloatingActionButton(
                        onClick = { /* Re-centers on current location; no-op in this demo. */ },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Recenter on my location")
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(0.62f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        WhereToCard(
                            pickupLabel = state.pickup?.title ?: "Set pickup location",
                            onPickupClick = { onOpenLocationSearch(LocationField.PICKUP) },
                            onDropClick = { onOpenLocationSearch(LocationField.DROP) },
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val home = savedPlaces.firstOrNull { it.label == SavedPlaceLabel.HOME }
                            val work = savedPlaces.firstOrNull { it.label == SavedPlaceLabel.WORK }
                            SavedPlaceChip(
                                icon = Icons.Filled.Home,
                                label = "Home",
                                hasPlace = home != null,
                                onClick = {
                                    val place = home
                                    if (place == null) {
                                        // Nothing saved yet - the chip's own label says "add in
                                        // Profile", so tapping it should actually take you there
                                        // instead of being a dead, disabled button.
                                        onNavigate(Destinations.SAVED_PLACES)
                                    } else {
                                        bookingViewModel.setPlace(
                                            LocationField.DROP,
                                            com.amchiyatri.rider.data.model.PlaceSuggestion(
                                                title = place.customName ?: "Home",
                                                subtitle = place.address,
                                                point = place.point,
                                                isSaved = true,
                                            ),
                                        )
                                        onGoToRideOptions()
                                    }
                                },
                            )
                            SavedPlaceChip(
                                icon = Icons.Filled.Work,
                                label = "Work",
                                hasPlace = work != null,
                                onClick = {
                                    val place = work
                                    if (place == null) {
                                        onNavigate(Destinations.SAVED_PLACES)
                                    } else {
                                        bookingViewModel.setPlace(
                                            LocationField.DROP,
                                            com.amchiyatri.rider.data.model.PlaceSuggestion(
                                                title = place.customName ?: "Work",
                                                subtitle = place.address,
                                                point = place.point,
                                                isSaved = true,
                                            ),
                                        )
                                        onGoToRideOptions()
                                    }
                                },
                            )
                        }
                    }

                    if (recents.isNotEmpty()) {
                        item {
                            Text(
                                "Recent",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(recents) { place ->
                            RecentPlaceRow(
                                title = place.title,
                                subtitle = place.subtitle,
                                onClick = {
                                    bookingViewModel.setPlace(LocationField.DROP, place)
                                    onGoToRideOptions()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhereToCard(pickupLabel: String, onPickupClick: () -> Unit, onDropClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPickupClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(pickupLabel, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onDropClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Where to?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SavedPlaceChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, hasPlace: Boolean, onClick: () -> Unit) {
    // Always enabled/clickable - even with nothing saved yet, tapping it should do something
    // (open Profile to add one) rather than sit there disabled just because the label mentions
    // where to go add it.
    AssistChip(
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null) },
        label = { Text(if (hasPlace) label else "$label · add in Profile") },
    )
}

@Composable
private fun RecentPlaceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
