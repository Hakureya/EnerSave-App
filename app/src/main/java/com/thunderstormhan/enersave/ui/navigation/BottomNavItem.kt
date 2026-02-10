package com.thunderstormhan.enersave.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Beranda")
    object Audit : BottomNavItem("audit", Icons.Default.Analytics, "Audit")
    object Shop : BottomNavItem("shop", Icons.Default.ShoppingCart, "Toko")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profil")
}