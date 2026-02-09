package com.thunderstormhan.enersave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.thunderstormhan.enersave.ui.theme.EnerSaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnerSaveTheme {
                // Panggil MainApp sebagai root UI
                MainApp()
            }
        }
    }
}