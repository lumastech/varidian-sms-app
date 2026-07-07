package com.varidian.varidiansms.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.services.PollingForegroundService
import com.varidian.varidiansms.ui.components.ConfirmDialog
import com.varidian.varidiansms.util.AppPrefs
import com.varidian.varidiansms.workers.HeartbeatWorker
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
    onOpenWebhooks: () -> Unit,
    onOpenPhoneKeys: () -> Unit,
    onOpenGateway: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("More", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(prefs.userName.ifEmpty { "Account" }, fontWeight = FontWeight.SemiBold)
                    Text(
                        prefs.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        prefs.serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column {
                MoreItem(Icons.Filled.Settings, "SMS Gateway Settings", "Run this phone as a gateway", onClick = onOpenGateway)
                HorizontalDivider()
                MoreItem(Icons.Filled.Share, "Webhooks", "Push events to your server", onClick = onOpenWebhooks)
                HorizontalDivider()
                MoreItem(Icons.Filled.Lock, "Phone API Keys", "Scoped keys for gateway devices", onClick = onOpenPhoneKeys)
                HorizontalDivider()
                MoreItem(Icons.Filled.Info, "API Documentation", "Open the HTTP API reference") {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "${prefs.serverUrl}/documentation".toUri()),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            MoreItem(
                Icons.AutoMirrored.Filled.ExitToApp,
                "Log out",
                "Revoke this device's access",
                tint = MaterialTheme.colorScheme.error,
            ) { confirmLogout = true }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Log out?",
            text = "The gateway on this phone will stop and the app's API key will be revoked.",
            confirmLabel = "Log out",
            onConfirm = {
                confirmLogout = false
                scope.launch {
                    api.logout() // best effort — revokes the key server-side
                    PollingForegroundService.stop(context)
                    HeartbeatWorker.cancel(context)
                    prefs.setGatewayEnabled(false)
                    prefs.clearAccount()
                    onLoggedOut()
                }
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun MoreItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
