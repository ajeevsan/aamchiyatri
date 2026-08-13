package com.amchiyatri.rider.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.AuthViewModel

@Composable
fun PhoneEntryScreen(
    onOtpSent: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState
    val activity = LocalContext.current as Activity
    var isPhoneFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Enter your mobile number",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "We'll send a one-time password to verify it's you.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(modifier = Modifier.padding(end = 8.dp)) {
                Text("+91", modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChange,
                modifier = Modifier.fillMaxWidth().onFocusChanged { isPhoneFocused = it.isFocused },
                placeholder = if (isPhoneFocused) null else { { Text("10-digit mobile number") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
        }

        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        PrimaryButton(
            text = "Send OTP",
            enabled = state.phoneNumber.length == 10,
            isLoading = state.isLoading,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            viewModel.sendOtp(activity) { onOtpSent(state.phoneNumber) }
        }

        Text(
            text = "By continuing, you agree to Amchi Yatri's Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
