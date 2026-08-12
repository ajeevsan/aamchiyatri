package com.amchiyatri.rider.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.Gender
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
) {
    val profile by profileViewModel.profile.collectAsState()
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var email by remember(profile) { mutableStateOf(profile?.email.orEmpty()) }
    var gender by remember(profile) { mutableStateOf(profile?.gender) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(20.dp))
            Text("Gender (optional)", style = MaterialTheme.typography.titleMedium)
            Gender.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(selected = gender == option, onClick = { gender = option }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = gender == option, onClick = { gender = option })
                    Text(option.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Save", enabled = name.isNotBlank()) {
                profileViewModel.updateBasicInfo(name.trim(), email.trim().ifBlank { null }, gender)
                onBack()
            }
        }
    }
}
