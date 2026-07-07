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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.PhoneItem
import com.varidian.varidiansms.ui.components.ConfirmDialog
import com.varidian.varidiansms.ui.components.EmptyState
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import com.varidian.varidiansms.ui.components.OnlineBadge
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PhonesScreen(onOpenGateway: () -> Unit) {
    val context = LocalContext.current
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()

    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var phones by remember { mutableStateOf<List<PhoneItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<PhoneItem?>(null) }
    var deleting by remember { mutableStateOf<PhoneItem?>(null) }

    LaunchedEffect(refreshKey) {
        loading = phones == null
        error = null
        when (val result = api.phones()) {
            is ApiResult.Success -> phones = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    Scaffold { padding ->
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
                Text("Phones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { refreshKey++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }

            when {
                loading && phones == null -> LoadingBox()
                error != null && phones == null -> ErrorBox(error!!, onRetry = { refreshKey++ })
                phones.isNullOrEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        icon = Icons.Filled.Phone,
                        title = "No phones yet",
                        subtitle = "Register this device as an SMS gateway to get started.",
                    )
                    OutlinedButton(onClick = onOpenGateway) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("SMS Gateway Settings")
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(phones.orEmpty(), key = { it.id }) { phone ->
                        PhoneCard(
                            phone = phone,
                            onEdit = { editing = phone },
                            onDelete = { deleting = phone },
                        )
                    }
                }
            }
        }
    }

    editing?.let { phone ->
        EditPhoneDialog(
            phone = phone,
            onDismiss = { editing = null },
            onSave = { fields ->
                scope.launch {
                    when (val result = api.updatePhone(phone.id, fields)) {
                        is ApiResult.Success -> {
                            editing = null
                            refreshKey++
                            Toast.makeText(context, "Phone updated", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    deleting?.let { phone ->
        ConfirmDialog(
            title = "Delete ${phone.phoneNumber}?",
            text = "The phone and its pending messages will be removed from your account. The device will stop receiving work.",
            onConfirm = {
                scope.launch {
                    when (val result = api.deletePhone(phone.id)) {
                        is ApiResult.Success -> {
                            deleting = null
                            refreshKey++
                        }
                        is ApiResult.Error -> {
                            deleting = null
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PhoneCard(phone: PhoneItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(phone.phoneNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OnlineBadge(phone.isOnline)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append("${phone.messagesPerMinute} msg/min · ${phone.maxSendAttempts} attempts · ")
                    append("expires after ${phone.messageExpirationSeconds / 60} min")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (phone.scheduleActive) {
                Text(
                    "Sends only ${phone.scheduleStart ?: "?"}–${phone.scheduleEnd ?: "?"} ${phone.scheduleTimezone ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            phone.lastHeartbeatAt?.let {
                Text(
                    "Last heartbeat: ${it.replace("T", " ").substringBefore(".")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settings")
                }
                TextButton(onClick = onDelete) {
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

@Composable
private fun EditPhoneDialog(
    phone: PhoneItem,
    onDismiss: () -> Unit,
    onSave: (JSONObject) -> Unit,
) {
    var perMinute by rememberSaveable { mutableStateOf(phone.messagesPerMinute.toString()) }
    var maxAttempts by rememberSaveable { mutableStateOf(phone.maxSendAttempts.toString()) }
    var expirationMinutes by rememberSaveable { mutableStateOf((phone.messageExpirationSeconds / 60).toString()) }
    var scheduleActive by rememberSaveable { mutableStateOf(phone.scheduleActive) }
    var scheduleStart by rememberSaveable { mutableStateOf(phone.scheduleStart ?: "08:00") }
    var scheduleEnd by rememberSaveable { mutableStateOf(phone.scheduleEnd ?: "18:00") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(phone.phoneNumber) },
        text = {
            Column {
                OutlinedTextField(
                    value = perMinute,
                    onValueChange = { perMinute = it },
                    label = { Text("Messages per minute (1–29)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = maxAttempts,
                    onValueChange = { maxAttempts = it },
                    label = { Text("Max send attempts (1–5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = expirationMinutes,
                    onValueChange = { expirationMinutes = it },
                    label = { Text("Message expiration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sending schedule", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = scheduleActive, onCheckedChange = { scheduleActive = it })
                }
                if (scheduleActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = scheduleStart,
                            onValueChange = { scheduleStart = it },
                            label = { Text("From (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = scheduleEnd,
                            onValueChange = { scheduleEnd = it },
                            label = { Text("To (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
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
                val rate = perMinute.toIntOrNull()
                val attempts = maxAttempts.toIntOrNull()
                val expiration = expirationMinutes.toIntOrNull()
                if (rate == null || rate !in 1..29 || attempts == null || attempts !in 1..5 ||
                    expiration == null || expiration * 60 !in 60..86400
                ) {
                    validationError = "Check the numeric values and their allowed ranges."
                    return@TextButton
                }
                onSave(
                    JSONObject().apply {
                        put("messages_per_minute", rate)
                        put("max_send_attempts", attempts)
                        put("message_expiration_seconds", expiration * 60)
                        put("schedule_active", scheduleActive)
                        if (scheduleActive) {
                            put("schedule_start", scheduleStart)
                            put("schedule_end", scheduleEnd)
                        }
                    },
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
