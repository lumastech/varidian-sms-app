package com.varidian.varidiansms.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.data.PhoneItem
import kotlinx.coroutines.launch

/**
 * Compose an SMS. "From" is one of the account's registered gateway
 * phones; "To" accepts one number, or several separated by commas
 * (sent via bulk-send).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()

    var phones by remember { mutableStateOf<List<PhoneItem>>(emptyList()) }
    var phonesError by remember { mutableStateOf<String?>(null) }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var dropdownOpen by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val result = api.phones()) {
            is ApiResult.Success -> {
                phones = result.data
                if (from.isEmpty()) from = result.data.firstOrNull()?.phoneNumber.orEmpty()
            }
            is ApiResult.Error -> phonesError = result.message
        }
    }

    fun send() {
        if (sending) return
        val recipients = to.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (from.isEmpty() || recipients.isEmpty() || content.isBlank()) {
            error = "Pick a sender, at least one recipient, and write a message."
            return
        }
        sending = true
        error = null
        scope.launch {
            val result = if (recipients.size == 1) {
                api.sendMessage(from, recipients.first(), content).let {
                    if (it is ApiResult.Error) it else ApiResult.Success(1)
                }
            } else {
                api.bulkSendMessage(from, recipients, content)
            }
            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(
                        context,
                        if (recipients.size == 1) "Message queued" else "${recipients.size} messages queued",
                        Toast.LENGTH_SHORT,
                    ).show()
                    onBack()
                }
                is ApiResult.Error -> {
                    sending = false
                    error = result.message
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New message") },
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
                .imePadding()
                .padding(16.dp),
        ) {
            if (phonesError != null) {
                Text(phonesError!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            } else if (phones.isEmpty() && from.isEmpty()) {
                Text(
                    "No gateway phones registered yet. Set up a phone under More → SMS Gateway Settings first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }

            ExposedDropdownMenuBox(expanded = dropdownOpen, onExpandedChange = { dropdownOpen = it }) {
                OutlinedTextField(
                    value = from,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("From (gateway phone)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                    phones.forEach { phone ->
                        DropdownMenuItem(
                            text = { Text("${phone.phoneNumber}  ${if (phone.isOnline) "• online" else "• offline"}") },
                            onClick = {
                                from = phone.phoneNumber
                                dropdownOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = to,
                onValueChange = { to = it },
                label = { Text("To") },
                placeholder = { Text("+260977654321, +260966…") },
                supportingText = { Text("Separate multiple recipients with commas") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Message") },
                minLines = 4,
                supportingText = { Text("${content.length} characters") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = ::send, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Queue message")
                }
            }
        }
    }
}
