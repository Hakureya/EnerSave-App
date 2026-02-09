package com.thunderstormhan.enersave.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thunderstormhan.enersave.ui.screens.auth.LoginScreen
import com.thunderstormhan.enersave.ui.screens.auth.RegisterScreen
import com.thunderstormhan.enersave.ui.screens.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    // NavHost adalah wadah yang mengatur perpindahan antar layar
    NavHost(
        navController = navController,
        startDestination = "login" // Layar pertama saat app dibuka
    ) {
        // Rute untuk Layar Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    // Pindah ke Home dan hapus history login agar tidak bisa back ke login lagi
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Rute untuk Layar Register
        composable("register") {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Rute untuk Layar Beranda (Home)
        composable("home") {
            HomeScreen()
        }
    }
}