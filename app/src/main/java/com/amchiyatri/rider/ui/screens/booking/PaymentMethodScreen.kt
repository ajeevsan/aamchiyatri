package com.amchiyatri.rider.ui.screens.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.PaymentMethod
import com.amchiyatri.rider.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    onBack: () -> Unit,
    bookingViewModel: BookingViewModel,
) {
    val state by bookingViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment method") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PaymentRow(
                icon = Icons.Filled.Money,
                title = "Cash",
                subtitle = "Pay the driver directly at the end of the trip",
                selected = state.paymentMethod == PaymentMethod.CASH,
            ) { bookingViewModel.selectPaymentMethod(PaymentMethod.CASH); onBack() }

            PaymentRow(
                icon = Icons.Filled.CreditCard,
                title = "UPI",
                subtitle = "Pay via any UPI app once the trip ends",
                selected = state.paymentMethod == PaymentMethod.UPI,
            ) { bookingViewModel.selectPaymentMethod(PaymentMethod.UPI); onBack() }

            PaymentRow(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "Amchi Yatri Wallet",
                subtitle = "Load money once, ride without cash",
                selected = state.paymentMethod == PaymentMethod.WALLET,
            ) { bookingViewModel.selectPaymentMethod(PaymentMethod.WALLET); onBack() }
        }
    }
}

@Composable
private fun PaymentRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
