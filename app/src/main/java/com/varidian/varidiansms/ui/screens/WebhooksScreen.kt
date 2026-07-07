package com.varidian.varidiansms.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.WebhookItem
import com.varidian.varidiansms.ui.components.ConfirmDialog
import com.varidian.varidiansms.ui.components.EmptyState
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import kotlinx.coroutines.launch

/**
 * Webhook subscriptions: get notified at your own URL when messages
 * are received, delivered, fail, or when a phone goes on/offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var webhooks by remember { mutableStateOf<List<WebhookItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<WebhookItem?>(null) }
    var createdSigningKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        loading = webhooks == null
        error = null
        when (val result = api.webhooks()) {
            is ApiResult.Success -> webhooks = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Webhooks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add webhook")
            }
        },
    ) { padding ->
        when {
            loading && webhooks == null -> LoadingBox(Modifier.padding(padding))
            error != null && webhooks == null -> ErrorBox(error!!, onRetry = { refreshKey++ }, Modifier.padding(padding))
            webhooks.isNullOrEmpty() -> Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.Share,
                    title = "No webhooks",
                    subtitle = "Add a webhook to get events (received SMS, delivery reports, phone status) pushed to your server.",
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(webhooks.orEmpty(), key = { it.id }) { webhook ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                webhook.callbackUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${webhook.events.size} event(s): ${webhook.events.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (webhook.phoneNumbers.isNotEmpty()) {
                                Text(
                                    "Only for: ${webhook.phoneNumbers.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = webhook.isActive,
                                        onCheckedChange = { active ->
                                            scope.launch {
                                                when (val result = api.updateWebhook(webhook.id, isActive = active)) {
                                                    is ApiResult.Success -> refreshKey++
                                                    is ApiResult.Error ->
                                                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (webhook.isActive) "Active" else "Paused",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                TextButton(onClick = { deleting = webhook }) {
                                    Icon(
                                        Icons.Filled.Delete, contentDescription = null,
                                        Modifier.width(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateWebhookDialog(
            onDismiss = { showCreate = false },
            onCreate = { url, events ->
                scope.launch {
                    when (val result = api.createWebhook(url, events, emptyList())) {
                        is ApiResult.Success -> {
                            showCreate = false
                            createdSigningKey = result.data.signingKey
                            refreshKey++
                        }
                        is ApiResult.Error ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    createdSigningKey?.let { key ->
        AlertDialog(
            onDismissRequest = { createdSigningKey = null },
            title = { Text("Webhook created") },
            text = {
                Column {
                    Text("Save this signing key — it is shown only once. Use it to verify webhook payload signatures.")
                    Spacer(Modifier.height(12.dp))
                    Text(key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(key))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { createdSigningKey = null }) { Text("Done") }
            },
        )
    }

    deleting?.let { webhook ->
        ConfirmDialog(
            title = "Delete webhook?",
            text = "Events will no longer be delivered to ${webhook.callbackUrl}.",
            onConfirm = {
                scope.launch {
                    api.deleteWebhook(webhook.id)
                    deleting = null
                    refreshKey++
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CreateWebhookDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var url by rememberSaveable { mutableStateOf("") }
    val selectedEvents = remember { WebhookItem.ALL_EVENTS.map { it == "message.phone.received" }.toMutableStateList() }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New webhook") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Callback URL") },
                    placeholder = { Text("https://example.com/webhooks/sms") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Events", style = MaterialTheme.typography.labelLarge)
                WebhookItem.ALL_EVENTS.forEachIndexed { index, event ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedEvents[index],
                            onCheckedChange = { selectedEvents[index] = it },
                        )
                        Text(event, style = MaterialTheme.typography.bodySmall)
                    }
                }
                validationError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val events = WebhookItem.ALL_EVENTS.filterIndexed { i, _ -> selectedEvents[i] }
                if (!url.startsWith("http") || events.isEmpty()) {
                    validationError = "Enter a valid URL and pick at least one event."
                    return@TextButton
                }
                onCreate(url.trim(), events)
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
