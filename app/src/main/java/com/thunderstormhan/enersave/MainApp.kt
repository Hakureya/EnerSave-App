package com.thunderstormhan.enersave

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thunderstormhan.enersave.ui.navigation.EnerSaveBottomBar
import com.thunderstormhan.enersave.ui.navigation.NavGraph

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Daftar layar yang tidak butuh Bottom Bar
    val authRoutes = listOf("login", "register")

    Scaffold(
        bottomBar = {
            if (currentRoute !in authRoutes) {
                EnerSaveBottomBar(navController)
            }
        }
    ) { innerPadding ->
        // NavGraph yang kita buat sebelumnya, masukkan padding agar tidak tertutup bar
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(navController = navController)
        }
    }
}