package com.amchiyatri.rider.ui.screens.driver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.VehicleType
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    driverViewModel: DriverViewModel,
) {
    val state = driverViewModel.onboardingState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Become a driver") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                "Tell us about your vehicle. You can switch back to riding any time from Profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            Text("Vehicle type", style = MaterialTheme.typography.titleMedium)
            VehicleType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = state.vehicleType == type, onClick = { driverViewModel.onVehicleTypeChange(type) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.vehicleType == type, onClick = { driverViewModel.onVehicleTypeChange(type) })
                    Text(type.displayName)
                }
            }

            OutlinedTextField(
                value = state.vehicleNumber,
                onValueChange = driverViewModel::onVehicleNumberChange,
                label = { Text("Vehicle number (e.g. MH 02 AB 1234)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = state.vehicleModel,
                onValueChange = driverViewModel::onVehicleModelChange,
                label = { Text("Vehicle model (e.g. Bajaj RE Auto)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            driverViewModel.onboardingError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            PrimaryButton(
                text = "Start driving",
                isLoading = state.isSaving,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                driverViewModel.completeOnboarding(onDone)
            }
        }
    }
}
