package com.amchiyatri.rider.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.ui.components.AmchiYatriBottomBar
import com.amchiyatri.rider.ui.navigation.Destinations
import com.amchiyatri.rider.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenEmergencyContacts: () -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onLoggedOut: () -> Unit,
    profileViewModel: ProfileViewModel,
) {
    val profile by profileViewModel.profile.collectAsState()

    Scaffold(
        bottomBar = { AmchiYatriBottomBar(currentRoute = Destinations.PROFILE, onNavigate = onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenEditProfile)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            (profile?.name?.firstOrNull() ?: '?').uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile?.name ?: "Add your name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("+91 ${profile?.phoneNumber.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
                Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
            }

            Spacer(modifier = Modifier.padding(top = 24.dp))
            HorizontalDivider()

            ProfileRow(icon = Icons.Filled.Place, title = "Saved places", subtitle = "Home, Work and more", onClick = onOpenSavedPlaces)
            ProfileRow(icon = Icons.Filled.Contacts, title = "Emergency contacts", subtitle = "For SOS during a ride", onClick = onOpenEmergencyContacts)
            ProfileRow(icon = Icons.Filled.Language, title = "App language", subtitle = "English, Hindi, Marathi", onClick = onOpenLanguageSettings)
            ProfileRow(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Help & support", subtitle = "FAQs and contact us", onClick = onOpenHelp)

            HorizontalDivider()
            ProfileRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log out",
                subtitle = null,
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    profileViewModel.logout()
                    onLoggedOut()
                },
            )
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
