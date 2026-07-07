package com.varidian.varidiansms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.ui.theme.StatusColors

/** Colored pill for message/phone statuses. */
@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun statusColor(status: String): Color = when (status.lowercase()) {
    "delivered", "sent", "received", "online" -> StatusColors.green
    "pending", "queued", "scheduled", "sending" -> StatusColors.amber
    "failed", "expired", "offline" -> StatusColors.red
    else -> StatusColors.blue
}

/** Small green/red dot + label for phone online state. */
@Composable
fun OnlineBadge(isOnline: Boolean) {
    val color = if (isOnline) StatusColors.green else StatusColors.red
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
