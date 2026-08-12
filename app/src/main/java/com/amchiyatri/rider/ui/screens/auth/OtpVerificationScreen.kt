package com.amchiyatri.rider.ui.screens.auth

import android.app.Activity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val activity = LocalContext.current as Activity
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Firebase can auto-retrieve the SMS and sign the rider in without them typing anything;
    // when that happens, move on immediately instead of waiting for a manual submit.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onVerified()
    }

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
            text = "Enter the code sent to +91 $phoneNumber. It may fill in automatically.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = state.otp,
            onValueChange = viewModel::onOtpChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("6-digit OTP") },
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
            enabled = state.otp.length in 4..6,
            isLoading = state.isLoading,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            viewModel.verifyOtp(onVerified)
        }

        TextButton(onClick = { viewModel.resendOtp(activity) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Resend OTP")
        }
    }
}
