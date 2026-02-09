package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AuditScreen() {
    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Simulasi Ruangan", style = MaterialTheme.typography.titleSmall)
                        Text("LIVE AUDIT MODE", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Rp 1.200/hari", color = Color.Blue, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Room Canvas (Representasi Visual Ruangan)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                RoomCanvas()
            }

            // Bottom Sheet Placeholder (Koleksi Alat)
            ApplianceControlSheet()
        }
    }
}