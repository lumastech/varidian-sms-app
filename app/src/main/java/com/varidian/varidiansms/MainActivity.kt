package com.varidian.varidiansms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.varidian.varidiansms.ui.screens.DashboardScreen
import com.varidian.varidiansms.ui.screens.GatewaySettingsScreen
import com.varidian.varidiansms.ui.screens.LoginScreen
import com.varidian.varidiansms.ui.screens.MessagesScreen
import com.varidian.varidiansms.ui.screens.MoreScreen
import com.varidian.varidiansms.ui.screens.PhoneApiKeysScreen
import com.varidian.varidiansms.ui.screens.PhonesScreen
import com.varidian.varidiansms.ui.screens.SendMessageScreen
import com.varidian.varidiansms.ui.screens.SignUpScreen
import com.varidian.varidiansms.ui.screens.WebhooksScreen
import com.varidian.varidiansms.ui.theme.VaridianSMSTheme
import com.varidian.varidiansms.util.AppPrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaridianSMSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val MAIN = "main"
    const val SEND = "send"
    const val WEBHOOKS = "webhooks"
    const val PHONE_KEYS = "phone_keys"
    const val GATEWAY = "gateway"

    /** Gateway settings shown instead of login when no account is signed in. */
    const val GATEWAY_STANDALONE = "gateway_standalone"
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val nav = rememberNavController()

    val start = if (prefs.isAccountLoggedIn) Routes.MAIN else Routes.GATEWAY_STANDALONE

    fun goToMain() {
        nav.navigate(Routes.MAIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun goToStandaloneGateway() {
        nav.navigate(Routes.GATEWAY_STANDALONE) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.GATEWAY_STANDALONE) {
            GatewaySettingsScreen(
                onGoToLogin = { nav.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = ::goToMain,
                onGoToSignUp = { nav.navigate(Routes.SIGNUP) },
            )
        }
        composable(Routes.SIGNUP) {
            SignUpScreen(
                onRegistered = ::goToMain,
                onGoToLogin = { nav.popBackStack() },
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                onOpenSend = { nav.navigate(Routes.SEND) },
                onOpenWebhooks = { nav.navigate(Routes.WEBHOOKS) },
                onOpenPhoneKeys = { nav.navigate(Routes.PHONE_KEYS) },
                onOpenGateway = { nav.navigate(Routes.GATEWAY) },
                onLoggedOut = ::goToStandaloneGateway,
            )
        }
        composable(Routes.SEND) {
            SendMessageScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.WEBHOOKS) {
            WebhooksScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.PHONE_KEYS) {
            PhoneApiKeysScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.GATEWAY) {
            GatewaySettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val TABS = listOf(
    BottomTab("tab_dashboard", "Dashboard", Icons.Filled.Home),
    BottomTab("tab_messages", "Messages", Icons.Filled.Email),
    BottomTab("tab_phones", "Phones", Icons.Filled.Phone),
    BottomTab("tab_more", "More", Icons.Filled.Menu),
)

@Composable
fun MainShell(
    onOpenSend: () -> Unit,
    onOpenWebhooks: () -> Unit,
    onOpenPhoneKeys: () -> Unit,
    onOpenGateway: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val tabNav: NavHostController = rememberNavController()
    val backStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNav.navigate(tab.route) {
                                popUpTo(tabNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNav,
            startDestination = "tab_dashboard",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("tab_dashboard") {
                DashboardScreen(
                    onSendMessage = onOpenSend,
                    onOpenGateway = onOpenGateway,
                )
            }
            composable("tab_messages") {
                MessagesScreen(onCompose = onOpenSend)
            }
            composable("tab_phones") {
                PhonesScreen(onOpenGateway = onOpenGateway)
            }
            composable("tab_more") {
                MoreScreen(
                    onOpenWebhooks = onOpenWebhooks,
                    onOpenPhoneKeys = onOpenPhoneKeys,
                    onOpenGateway = onOpenGateway,
                    onLoggedOut = onLoggedOut,
                )
            }
        }
    }
}
