package com.varidian.varidiansms.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.MessageItem
import com.varidian.varidiansms.ui.components.EmptyState
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import com.varidian.varidiansms.ui.components.StatusChip

private data class MessageFilter(val label: String, val status: String? = null, val type: String? = null)

private val FILTERS = listOf(
    MessageFilter("All"),
    MessageFilter("Sent", type = "mobile-terminated"),
    MessageFilter("Received", type = "mobile-originated"),
    MessageFilter("Queued", status = "pending"),
    MessageFilter("Delivered", status = "delivered"),
    MessageFilter("Failed", status = "failed"),
)

@Composable
fun MessagesScreen(onCompose: () -> Unit) {
    val context = LocalContext.current
    val api = remember { PortalApi(context) }

    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var messages by remember { mutableStateOf<List<MessageItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<MessageItem?>(null) }

    LaunchedEffect(filterIndex, refreshKey) {
        loading = true
        error = null
        val filter = FILTERS[filterIndex]
        when (val result = api.messages(status = filter.status, type = filter.type)) {
            is ApiResult.Success -> messages = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCompose,
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                text = { Text("New message") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { refreshKey++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEachIndexed { index, filter ->
                    FilterChip(
                        selected = filterIndex == index,
                        onClick = { filterIndex = index },
                        label = { Text(filter.label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                loading && messages == null -> LoadingBox()
                error != null -> ErrorBox(error!!, onRetry = { refreshKey++ })
                messages.isNullOrEmpty() -> EmptyState(
                    icon = Icons.Filled.Email,
                    title = "No messages",
                    subtitle = "Messages matching this filter will show up here.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages.orEmpty(), key = { it.id }) { message ->
                        MessageRow(message, onClick = { selected = message })
                    }
                }
            }
        }
    }

    selected?.let { message ->
        MessageDetailDialog(message, onDismiss = { selected = null })
    }
}

@Composable
private fun MessageDetailDialog(message: MessageItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (message.isOutgoing) "Outgoing message" else "Incoming message") },
        text = {
            Column {
                DetailRow("Status") { StatusChip(message.status) }
                DetailText("Gateway phone", message.owner)
                DetailText(if (message.isOutgoing) "To" else "From", message.contact)
                message.sim?.let { DetailText("SIM", it) }
                message.createdAt?.let { DetailText("Created", it.replace("T", " ").substringBefore(".")) }
                message.failureReason?.let { DetailText("Failure reason", it) }
                Spacer(Modifier.height(8.dp))
                Text("Content", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    DetailRow(label) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
