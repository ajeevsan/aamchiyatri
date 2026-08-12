package com.amchiyatri.rider.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.ui.components.PrimaryButton
import com.amchiyatri.rider.util.dialNumber

// TODO: replace with your real rider-support line before shipping.
private const val SUPPORT_PHONE_NUMBER = "+911800000000"

private data class FaqItem(val question: String, val answer: String)

private val faqs = listOf(
    FaqItem("How is my fare calculated?", "Fare = base fare + distance charge + time charge, shown upfront before you confirm a ride. It can only change if your route changes mid-trip."),
    FaqItem("What if I feel unsafe during a ride?", "Tap the red SOS button on the tracking screen any time. It can call the police, ring an emergency contact, or share your live trip."),
    FaqItem("Can I change my drop location mid-ride?", "Not yet in this build — cancel and re-book if your destination changes. Live re-routing support is planned."),
    FaqItem("How do I pay my driver?", "Choose Cash, UPI, or Amchi Yatri Wallet before requesting a ride from the ride-options screen."),
    FaqItem("How do refunds work for a cancelled ride?", "No fare is charged if you cancel before a driver is assigned. After that, a small cancellation fee may apply."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & support") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PrimaryButton(text = "Call rider support") { context.dialNumber(SUPPORT_PHONE_NUMBER) }
            }
            item {
                Text("Frequently asked questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(faqs) { faq -> FaqCard(faq) }
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(faq.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(faq.answer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
