package com.varidian.varidiansms.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.BillingData
import com.varidian.varidiansms.data.UsageMetric
import com.varidian.varidiansms.data.WalletTransactionItem
import com.varidian.varidiansms.data.formatKwacha
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import com.varidian.varidiansms.ui.components.StatusChip
import com.varidian.varidiansms.ui.theme.StatusColors
import com.varidian.varidiansms.util.AppPrefs
import java.util.Locale

/**
 * Read-only view of the account's plan, monthly usage and wallet. Plan
 * changes and wallet top-ups are deliberately web-only — this screen links
 * out to the portal for anything that moves money.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val api = remember { PortalApi(context) }

    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<BillingData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = data == null
        error = null
        when (val result = api.billing()) {
            is ApiResult.Success -> data = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    fun openPortalBilling() {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "${prefs.serverUrl.trimEnd('/')}/billing".toUri()),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing & Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && data == null -> LoadingBox(Modifier.padding(padding))
            error != null && data == null ->
                ErrorBox(error!!, onRetry = { refreshKey++ }, Modifier.padding(padding))

            else -> data?.let { billing ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { PlanCard(billing) }
                    item { UsageCard(billing) }
                    item { WalletCard(billing) }
                    item {
                        OutlinedButton(
                            onClick = ::openPortalBilling,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Manage billing in the portal")
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp),
                            )
                        }
                    }
                    item {
                        Text(
                            "Plan changes and wallet top-ups are handled on the web portal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(billing: BillingData) {
    val subscription = billing.subscription

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Current plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        subscription.planName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusChip(subscription.status)
            }

            if (subscription.price > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatKwacha(subscription.price, subscription.currency)} / month",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            subscription.expiresAt?.let { expiry ->
                Spacer(Modifier.height(4.dp))
                Text(
                    if (subscription.isActive) {
                        "Renews ${expiry.substringBefore("T")}"
                    } else {
                        "Expired ${expiry.substringBefore("T")}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!subscription.isActive) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sending is paused until the subscription is renewed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.red,
                )
            }

            subscription.pendingPlan?.let { pending ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Scheduled change: switches to $pending on " +
                        "${subscription.expiresAt?.substringBefore("T") ?: "the next renewal"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.amber,
                )
            }
        }
    }
}

@Composable
private fun UsageCard(billing: BillingData) {
    val usage = billing.usage

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("This month", fontWeight = FontWeight.SemiBold)
            usage.period.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            UsageRow("Messages", usage.messages)
            Spacer(Modifier.height(12.dp))
            UsageRow("Phones", usage.phones)
            Spacer(Modifier.height(12.dp))
            UsageRow("Webhooks", usage.webhooks)

            usage.resetsAt?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Message quota resets ${it.substringBefore("T")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UsageRow(label: String, metric: UsageMetric) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (metric.isUnlimited) {
                    String.format(Locale.US, "%,d · Unlimited", metric.used)
                } else {
                    String.format(Locale.US, "%,d / %,d", metric.used, metric.limit)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!metric.isUnlimited) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { metric.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    metric.fraction >= 1f -> StatusColors.red
                    metric.fraction >= 0.8f -> StatusColors.amber
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun WalletCard(billing: BillingData) {
    val wallet = billing.wallet

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Wallet balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatKwacha(wallet.balance, wallet.currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (wallet.balance > 0) StatusColors.green else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(16.dp))
            Text("Recent transactions", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))

            if (wallet.transactions.isEmpty()) {
                Text(
                    "No transactions yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                wallet.transactions.forEachIndexed { index, transaction ->
                    if (index > 0) HorizontalDivider()
                    TransactionRow(transaction, wallet.currency)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: WalletTransactionItem, currency: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                transaction.type.replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
            )
            val subtitle = listOfNotNull(
                transaction.createdAt?.substringBefore("T"),
                transaction.reference,
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (transaction.amount >= 0) "+" else "-") +
                formatKwacha(kotlin.math.abs(transaction.amount), currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (transaction.amount >= 0) StatusColors.green else MaterialTheme.colorScheme.onSurface,
        )
    }
}
