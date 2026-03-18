package com.customdialer.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Login : Screen("login", "Login", Icons.Filled.Lock)
    data object Signup : Screen("signup", "Sign Up", Icons.Filled.PersonAdd)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    data object Dialer : Screen("dialer", "Dialer", Icons.Filled.Dialpad)
    data object CallHistory : Screen("call_history", "History", Icons.Filled.History)
    data object Contacts : Screen("contacts", "Contacts", Icons.Filled.Contacts)
    data object PowerDialer : Screen("power_dialer", "Power Dialer", Icons.Filled.PlayCircle)
    data object Store : Screen("store", "Store", Icons.Filled.ShoppingCart)
    data object Email : Screen("email", "Email", Icons.Filled.Email)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Dialer,
    Screen.Store,
    Screen.Contacts,
    Screen.Profile
)

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Dialer,
    Screen.Store,
    Screen.PowerDialer,
    Screen.CallHistory,
    Screen.Contacts,
    Screen.Email,
    Screen.Profile,
    Screen.Settings
)
