package com.amchiyatri.rider.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.amchiyatri.rider.ui.navigation.Destinations

private data class BottomTab(val route: String, val label: String, val filledIcon: androidx.compose.ui.graphics.vector.ImageVector, val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    BottomTab(Destinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Destinations.RIDE_HISTORY, "Rides", Icons.Filled.History, Icons.Outlined.History),
    BottomTab(Destinations.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun AmchiYatriBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}
