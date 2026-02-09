package com.thunderstormhan.enersave.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thunderstormhan.enersave.ui.screens.auth.LoginScreen
import com.thunderstormhan.enersave.ui.screens.auth.RegisterScreen
import com.thunderstormhan.enersave.ui.screens.home.HomeScreen
import com.thunderstormhan.enersave.ui.screens.audit.AuditScreen
// Pastikan import ProfileScreen dan ShopScreen jika sudah dibuat

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        // --- AUTH ROUTES ---
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- MAIN ROUTES (Harus Sama dengan BottomNavItem.route) ---
        composable("home") {
            HomeScreen()
        }

        composable("audit") {
            AuditScreen()
        }

        composable("shop") {
            // Jika belum buat filenya, gunakan Placeholder dulu agar tidak FC
            Text("Halaman Toko (Shop)")
        }

        composable("profile") {
            // Jika belum buat filenya, gunakan Placeholder dulu agar tidak FC
            Text("Halaman Profil")
        }
    }
}
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Beranda")
    object Audit : BottomNavItem("audit", Icons.Default.Analytics, "Audit")
    object Shop : BottomNavItem("shop", Icons.Default.ShoppingCart, "Toko")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profil")
}