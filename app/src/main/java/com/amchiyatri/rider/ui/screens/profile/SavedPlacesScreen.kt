package com.amchiyatri.rider.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.amchiyatri.rider.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
) {
    val profile by profileViewModel.profile.collectAsState()
    var editingLabel by remember { mutableStateOf<SavedPlaceLabel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved places") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        val savedPlaces = profile?.savedPlaces.orEmpty()
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            listOf(SavedPlaceLabel.HOME, SavedPlaceLabel.WORK).forEach { label ->
                val existing = savedPlaces.firstOrNull { it.label == label }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editingLabel = label }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (label == SavedPlaceLabel.HOME) Icons.Filled.Home else Icons.Filled.Work, contentDescription = null)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(if (label == SavedPlaceLabel.HOME) "Home" else "Work", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                existing?.address ?: "Tap to set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        if (existing != null) {
                            IconButton(onClick = { profileViewModel.removeSavedPlace(existing.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            val otherPlaces = savedPlaces.filter { it.label == SavedPlaceLabel.OTHER }
            if (otherPlaces.isNotEmpty()) {
                Text(
                    "Other saved places",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(otherPlaces) { place ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Place, contentDescription = null)
                                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(place.customName ?: "Saved place", style = MaterialTheme.typography.bodyLarge)
                                    Text(place.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { profileViewModel.removeSavedPlace(place.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingLabel?.let { label ->
        PickPlaceDialog(
            title = "Set ${if (label == SavedPlaceLabel.HOME) "Home" else "Work"} location",
            onDismiss = { editingLabel = null },
            onPicked = { place ->
                profileViewModel.addSavedPlace(label, null, "${place.title}, ${place.subtitle}", place.point)
                editingLabel = null
            },
            search = profileViewModel::searchPlaces,
        )
    }
}

@Composable
private fun PickPlaceDialog(
    title: String,
    onDismiss: () -> Unit,
    onPicked: (PlaceSuggestion) -> Unit,
    search: suspend (String) -> List<PlaceSuggestion>,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isQueryFocused by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        isLoading = true
        results = search(query)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = if (isQueryFocused) null else { { Text("Search a landmark or area") } },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { isQueryFocused = it.isFocused },
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                } else {
                    Column(modifier = Modifier.height(240.dp)) {
                        LazyColumn {
                            items(results) { place ->
                                Text(
                                    "${place.title}\n${place.subtitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPicked(place) }
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
