package com.thunderstormhan.enersave.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thunderstormhan.enersave.ui.screens.auth.LoginScreen
import com.thunderstormhan.enersave.ui.screens.auth.RegisterScreen
import com.thunderstormhan.enersave.ui.screens.home.HomeScreen
import com.thunderstormhan.enersave.ui.screens.audit.AuditScreen
import com.thunderstormhan.enersave.ui.screens.profile.ProfileScreen // Impor UI Profil
import com.thunderstormhan.enersave.ui.screens.shop.ShopScreen
import com.thunderstormhan.enersave.viewmodel.AuditViewModel
import com.thunderstormhan.enersave.viewmodel.ProfileViewModel // Impor ViewModel Profil

import com.thunderstormhan.enersave.ui.screens.shop.ShopScreen
import com.thunderstormhan.enersave.viewmodel.ShopViewModel



@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // 1. Inisialisasi ViewModel di sini agar tetap hidup selama NavHost ada (Shared ViewModel)
    // Ini mencegah data Audit & Angka Biaya ter-reset saat pindah tab
    val auditViewModel: AuditViewModel = viewModel()
    val shopViewModel: ShopViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login", // Mulai dari login
        modifier = modifier
    ) {
        // --- AUTH ROUTES ---
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        // Hapus stack login agar user tidak bisa kembali ke login setelah masuk
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

        // --- MAIN ROUTES (Harus sesuai dengan BottomNavItem.route) ---
        composable("home") {
            HomeScreen()
        }

        composable("audit") {
            // 2. Kirimkan instance auditViewModel yang sama ke AuditScreen
            AuditScreen(viewModel = auditViewModel, shopViewModel = shopViewModel)
        }

        composable("shop") {
            // Placeholder untuk halaman Toko
            ShopScreen(viewModel = shopViewModel)
        }

        composable("profile") {
            // Inisialisasi ProfileViewModel
            val profileViewModel: ProfileViewModel = viewModel()

            // Panggil ProfileScreen dan berikan fungsi navigasi untuk logout
            ProfileScreen(
                viewModel = profileViewModel,
                onLogoutSuccess = {
                    navController.navigate("login") {
                        // Hapus seluruh backstack (riwayat layar) agar aman
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}