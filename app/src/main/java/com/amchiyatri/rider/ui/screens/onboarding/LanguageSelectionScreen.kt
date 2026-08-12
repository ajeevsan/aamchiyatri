package com.amchiyatri.rider.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.data.model.AppLanguage
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.SettingsViewModel

@Composable
fun LanguageSelectionScreen(
    onLanguageConfirmed: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf(AppLanguage.ENGLISH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Choose your language",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "भाषा निवडा · भाषा चुनें",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLanguage.entries.forEach { language ->
                LanguageOption(
                    language = language,
                    selected = language == selected,
                    onSelect = { selected = language },
                )
            }
        }

        PrimaryButton(
            text = "Continue",
            modifier = Modifier.padding(top = 32.dp),
        ) {
            settingsViewModel.setLanguage(selected)
            onLanguageConfirmed()
        }
    }
}

@Composable
private fun LanguageOption(language: AppLanguage, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(language.nativeLabel, style = MaterialTheme.typography.titleMedium)
                if (language.nativeLabel != language.label) {
                    Text(language.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
