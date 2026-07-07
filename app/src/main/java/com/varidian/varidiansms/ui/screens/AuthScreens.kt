package com.varidian.varidiansms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.varidian.varidiansms.api.ApiResult
import com.varidian.varidiansms.api.PortalApi
import com.varidian.varidiansms.util.AppPrefs
import kotlinx.coroutines.launch

/**
 * Sign in with the email/password of your web portal account.
 * A successful login stores the issued master API key locally.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onGoToSignUp: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf(prefs.userEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        if (email.isBlank() || password.isBlank()) {
            error = "All fields are required."
            return
        }
        loading = true
        error = null
        scope.launch {
            when (val result = api.login(email.trim(), password)) {
                is ApiResult.Success -> {
                    prefs.saveAccount(
                        result.data.apiKey,
                        result.data.user.name,
                        result.data.user.email,
                    )
                    onLoggedIn()
                }
                is ApiResult.Error -> {
                    loading = false
                    error = result.message
                }
            }
        }
    }

    AuthScaffold(title = "Welcome back", subtitle = "Sign in to manage your SMS gateway") {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visible = showPassword,
            onToggleVisibility = { showPassword = !showPassword },
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = ::submit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign in")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("New here?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onGoToSignUp) { Text("Create an account") }
        }
    }
}

@Composable
fun SignUpScreen(onRegistered: () -> Unit, onGoToLogin: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val api = remember { PortalApi(context) }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            error = "All fields are required."
            return
        }
        if (password != confirm) {
            error = "Passwords do not match."
            return
        }
        loading = true
        error = null
        scope.launch {
            when (val result = api.register(name.trim(), email.trim(), password, confirm)) {
                is ApiResult.Success -> {
                    prefs.saveAccount(
                        result.data.apiKey,
                        result.data.user.name,
                        result.data.user.email,
                    )
                    onRegistered()
                }
                is ApiResult.Error -> {
                    loading = false
                    error = result.message
                }
            }
        }
    }

    AuthScaffold(title = "Create account", subtitle = "Set up your Varidian SMS account") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visible = showPassword,
            onToggleVisibility = { showPassword = !showPassword },
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = confirm,
            onValueChange = { confirm = it },
            label = "Confirm password",
            visible = showPassword,
            onToggleVisibility = { showPassword = !showPassword },
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = ::submit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Create account")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onGoToLogin) { Text("Sign in") }
        }
    }
}

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Varidian SMS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onToggleVisibility) {
                Text(if (visible) "Hide" else "Show", style = MaterialTheme.typography.labelMedium)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
