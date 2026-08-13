package com.amchiyatri.rider.ui.screens.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.PlaceSuggestion
import com.amchiyatri.rider.data.model.SavedPlaceLabel
import com.amchiyatri.rider.ui.navigation.LocationField
import com.amchiyatri.rider.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    field: LocationField,
    onBack: () -> Unit,
    onPlaceSelected: () -> Unit,
    bookingViewModel: BookingViewModel,
) {
    val state by bookingViewModel.uiState.collectAsState()
    val savedPlaces by bookingViewModel.savedPlaces.collectAsState()
    var isSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bookingViewModel.onSearchQueryChange("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (field == LocationField.PICKUP) "Set pickup point" else "Where are you going?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = bookingViewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().onFocusChanged { isSearchFocused = it.isFocused },
                placeholder = if (isSearchFocused) null else { { Text("Search a landmark, area or address in Mumbai") } },
                singleLine = true,
                trailingIcon = { if (state.isSearching) CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp) },
            )

            Spacer(modifier = Modifier.padding(top = 12.dp))

            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.searchQuery.isBlank()) {
                    if (field == LocationField.DROP) {
                        val home = savedPlaces.firstOrNull { it.label == SavedPlaceLabel.HOME }
                        val work = savedPlaces.firstOrNull { it.label == SavedPlaceLabel.WORK }
                        if (home != null) {
                            item {
                                PlaceRow(
                                    icon = Icons.Filled.Home,
                                    title = home.customName ?: "Home",
                                    subtitle = home.address,
                                ) {
                                    bookingViewModel.setPlace(
                                        field,
                                        PlaceSuggestion(title = home.customName ?: "Home", subtitle = home.address, point = home.point, isSaved = true),
                                    )
                                    onPlaceSelected()
                                }
                            }
                        }
                        if (work != null) {
                            item {
                                PlaceRow(
                                    icon = Icons.Filled.Work,
                                    title = work.customName ?: "Work",
                                    subtitle = work.address,
                                ) {
                                    bookingViewModel.setPlace(
                                        field,
                                        PlaceSuggestion(title = work.customName ?: "Work", subtitle = work.address, point = work.point, isSaved = true),
                                    )
                                    onPlaceSelected()
                                }
                            }
                        }
                    }
                }

                items(state.searchResults) { place ->
                    PlaceRow(
                        icon = Icons.Outlined.Place,
                        title = place.title,
                        subtitle = place.subtitle,
                    ) {
                        bookingViewModel.setPlace(field, place)
                        onPlaceSelected()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
