package com.varidian.varidiansms.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.varidian.varidiansms.api.ApiClient
import com.varidian.varidiansms.services.PollingForegroundService
import com.varidian.varidiansms.ui.components.OnlineBadge
import com.varidian.varidiansms.ui.theme.StatusColors
import com.varidian.varidiansms.util.AppPrefs
import com.varidian.varidiansms.workers.HeartbeatWorker
import java.util.concurrent.Executors

private val REQUIRED_PERMISSIONS: Array<String> = buildList {
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_CALL_LOG)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

/**
 * SMS Gateway settings — the device-side console (formerly the app's
 * main page). Registers THIS phone against the server, then keeps the
 * gateway alive with the polling service + heartbeat worker.
 *
 * The gateway authenticates with the account key by default; a
 * dedicated phone API key (vpk_…) can be pasted for tighter scoping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewaySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }

    var phoneNumber by rememberSaveable { mutableStateOf(prefs.phoneNumber) }
    var dedicatedKey by rememberSaveable { mutableStateOf(prefs.gatewayApiKey) }
    var forwardIncoming by rememberSaveable { mutableStateOf(prefs.isForwardIncomingEnabled) }
    var reportMissedCalls by rememberSaveable { mutableStateOf(prefs.isReportMissedCallsEnabled) }
    var running by rememberSaveable { mutableStateOf(prefs.isGatewayEnabled) }
    var busy by remember { mutableStateOf(false) }
    var statusText by rememberSaveable {
        mutableStateOf(
            if (prefs.isGatewayEnabled) {
                "Gateway is running as ${prefs.phoneNumber}."
            } else {
                "Gateway is stopped."
            }
        )
    }

    fun hasAllPermissions() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var permissionsGranted by remember { mutableStateOf(hasAllPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted) {
            Toast.makeText(context, "SMS permissions are required to run the gateway", Toast.LENGTH_LONG).show()
        }
    }

    fun startGateway() {
        if (busy) return
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Enter this phone's number", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasAllPermissions()) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }

        prefs.saveGateway(dedicatedKey, phoneNumber)
        busy = true
        statusText = "Registering with ${prefs.serverUrl}…"

        Executors.newSingleThreadExecutor().execute {
            val ok = ApiClient(context).register(null) // FCM token sent later via onNewToken
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                if (ok) {
                    prefs.setGatewayEnabled(true)
                    HeartbeatWorker.schedule(context)
                    PollingForegroundService.start(context)
                    running = true
                    statusText = "Gateway ONLINE as ${phoneNumber.trim()}. " +
                        "Polling every 20 s, heartbeat every 15 min."
                } else {
                    statusText = "Registration failed. Check your connection and API key."
                }
            }
        }
    }

    fun stopGateway() {
        PollingForegroundService.stop(context)
        HeartbeatWorker.cancel(context)
        prefs.setGatewayEnabled(false)
        running = false
        statusText = "Gateway stopped."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Gateway Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("This device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            prefs.serverUrl.ifEmpty { "No server configured" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OnlineBadge(running)
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "Turn this phone into an SMS gateway: it sends the messages you queue from the portal or API. Forwarding received SMS and reporting missed calls are optional and off by default.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("This phone's number") },
                placeholder = { Text("+260971234567") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = dedicatedKey,
                onValueChange = { dedicatedKey = it },
                label = { Text("Phone API key (optional)") },
                supportingText = { Text("Leave empty to use your account key. Create scoped vpk_ keys under More → Phone API Keys.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Forward received SMS", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Send SMS received on this phone to the server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = forwardIncoming,
                    onCheckedChange = {
                        forwardIncoming = it
                        prefs.isForwardIncomingEnabled = it
                    },
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Report missed calls", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Send missed calls on this phone to the server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = reportMissedCalls,
                    onCheckedChange = {
                        reportMissedCalls = it
                        prefs.isReportMissedCallsEnabled = it
                    },
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (permissionsGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (permissionsGranted) StatusColors.green else StatusColors.amber,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (permissionsGranted) "SMS & phone permissions granted" else "SMS & phone permissions needed",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!permissionsGranted) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { permissionLauncher.launch(REQUIRED_PERMISSIONS) }) {
                        Text("Grant")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = ::startGateway,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (running) "Re-register & restart gateway" else "Start gateway")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = ::stopGateway,
                enabled = running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Stop gateway")
            }
            Spacer(Modifier.height(16.dp))

            Text(statusText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
