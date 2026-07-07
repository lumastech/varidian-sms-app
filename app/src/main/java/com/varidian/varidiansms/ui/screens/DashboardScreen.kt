package com.varidian.varidiansms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.DashboardData
import com.varidian.varidiansms.data.MessageItem
import com.varidian.varidiansms.ui.components.EmptyState
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import com.varidian.varidiansms.ui.components.OnlineBadge
import com.varidian.varidiansms.ui.components.StatusChip
import com.varidian.varidiansms.ui.theme.StatusColors
import com.varidian.varidiansms.util.AppPrefs

@Composable
fun DashboardScreen(onSendMessage: () -> Unit, onOpenGateway: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val api = remember { PortalApi(context) }

    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<DashboardData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = data == null
        error = null
        when (val result = api.dashboard()) {
            is ApiResult.Success -> data = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSendMessage,
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                text = { Text("Send SMS") },
            )
        },
    ) { padding ->
        when {
            loading && data == null -> LoadingBox(Modifier.padding(padding))
            error != null && data == null -> ErrorBox(error!!, onRetry = { refreshKey++ }, Modifier.padding(padding))
            else -> data?.let { dashboard ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Hi, ${prefs.userName.substringBefore(' ').ifEmpty { "there" }}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Here's your gateway at a glance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { refreshKey++ }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                "Phones online",
                                "${dashboard.stats.phonesOnline}/${dashboard.stats.phonesTotal}",
                                if (dashboard.stats.phonesOnline > 0) StatusColors.green else StatusColors.red,
                                Modifier.weight(1f),
                            )
                            StatCard("Queued", "${dashboard.stats.queued}", StatusColors.amber, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Sent today", "${dashboard.stats.sentToday}", StatusColors.blue, Modifier.weight(1f))
                            StatCard("Received today", "${dashboard.stats.receivedToday}", StatusColors.blue, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Delivered", "${dashboard.stats.delivered}", StatusColors.green, Modifier.weight(1f))
                            StatCard("Failed", "${dashboard.stats.failed}", StatusColors.red, Modifier.weight(1f))
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Phones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }

                    if (dashboard.phones.isEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("No phones registered yet", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Turn this device into an SMS gateway to start sending and receiving messages.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick = onOpenGateway) {
                                        Icon(Icons.Filled.Settings, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Set up this phone")
                                    }
                                }
                            }
                        }
                    } else {
                        items(dashboard.phones, key = { it.id }) { phone ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(phone.phoneNumber, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${phone.messagesPerMinute} msg/min",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    OnlineBadge(phone.isOnline)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Recent messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }

                    if (dashboard.recentMessages.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.Email,
                                title = "No messages yet",
                                subtitle = "Messages you send or receive will appear here.",
                            )
                        }
                    } else {
                        items(dashboard.recentMessages, key = { it.id }) { message ->
                            MessageRow(message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MessageRow(message: MessageItem, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (message.isOutgoing) "To ${message.contact}" else "From ${message.contact}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                StatusChip(message.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            message.createdAt?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it.replace("T", " ").substringBefore("."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
