package com.amchiyatri.rider.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.ui.viewmodel.AuthViewModel

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Verify your number",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Enter the 4-digit code sent to +91 $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(
            text = "Demo build: the code is 1234",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        OutlinedTextField(
            value = state.otp,
            onValueChange = viewModel::onOtpChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("4-digit OTP") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )

        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        PrimaryButton(
            text = "Verify & Continue",
            enabled = state.otp.length == 4,
            isLoading = state.isLoading,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            viewModel.verifyOtp(onVerified)
        }

        TextButton(onClick = viewModel::resendOtp, modifier = Modifier.padding(top = 8.dp)) {
            Text("Resend OTP")
        }
    }
}
