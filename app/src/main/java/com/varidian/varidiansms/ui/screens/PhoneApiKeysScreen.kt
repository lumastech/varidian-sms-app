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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.PhoneApiKeyItem
import com.varidian.varidiansms.ui.components.ConfirmDialog
import com.varidian.varidiansms.ui.components.EmptyState
import com.varidian.varidiansms.ui.components.ErrorBox
import com.varidian.varidiansms.ui.components.LoadingBox
import kotlinx.coroutines.launch

/**
 * Device-scoped API keys (vpk_…): safer credentials to run other
 * phones as gateways without exposing the master account key.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneApiKeysScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var keys by remember { mutableStateOf<List<PhoneApiKeyItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var createdKey by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<PhoneApiKeyItem?>(null) }

    LaunchedEffect(refreshKey) {
        loading = keys == null
        error = null
        when (val result = api.phoneApiKeys()) {
            is ApiResult.Success -> keys = result.data
            is ApiResult.Error -> error = result.message
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone API Keys") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create key")
            }
        },
    ) { padding ->
        when {
            loading && keys == null -> LoadingBox(Modifier.padding(padding))
            error != null && keys == null -> ErrorBox(error!!, onRetry = { refreshKey++ }, Modifier.padding(padding))
            keys.isNullOrEmpty() -> Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.Lock,
                    title = "No phone API keys",
                    subtitle = "Create a key to connect another device as a gateway without sharing your account credentials.",
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(keys.orEmpty(), key = { it.id }) { key ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(key.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${key.keyPreview}…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                key.attachedPhoneNumber?.let { "Attached to $it" } ?: "Not attached to a phone yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            key.lastUsedAt?.let {
                                Text(
                                    "Last used: ${it.replace("T", " ").substringBefore(".")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (key.attachedPhoneNumber != null) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            when (val result = api.detachPhoneApiKey(key.id)) {
                                                is ApiResult.Success -> refreshKey++
                                                is ApiResult.Error ->
                                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }) { Text("Detach phone") }
                                }
                                TextButton(onClick = { deleting = key }) {
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
        CreateKeyDialog(
            onDismiss = { showCreate = false },
            onCreate = { name ->
                scope.launch {
                    when (val result = api.createPhoneApiKey(name)) {
                        is ApiResult.Success -> {
                            showCreate = false
                            createdKey = result.data.plainKey
                            refreshKey++
                        }
                        is ApiResult.Error ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    createdKey?.let { plain ->
        AlertDialog(
            onDismissRequest = { createdKey = null },
            title = { Text("Key created") },
            text = {
                Column {
                    Text("Copy this key now — it is shown only once. Paste it in the gateway settings of the other device.")
                    Spacer(Modifier.height(12.dp))
                    Text(plain, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(plain))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { createdKey = null }) { Text("Done") }
            },
        )
    }

    deleting?.let { key ->
        ConfirmDialog(
            title = "Delete \"${key.name}\"?",
            text = "Any gateway using this key will stop working and must log in again with a new key.",
            onConfirm = {
                scope.launch {
                    api.deletePhoneApiKey(key.id)
                    deleting = null
                    refreshKey++
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CreateKeyDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New phone API key") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Backup Samsung") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
