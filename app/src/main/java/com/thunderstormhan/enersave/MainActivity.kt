package com.thunderstormhan.enersave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.thunderstormhan.enersave.ui.navigation.NavGraph
import com.thunderstormhan.enersave.ui.theme.EnerSaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnerSaveTheme {
                val navController = rememberNavController()
                // Panggil NavGraph di sini
                NavGraph(navController = navController)
            }
        }
    }
}