package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditScreen(viewModel: AuditViewModel = viewModel()) {
    var selectedAppliance by remember { mutableStateOf<Appliance?>(null) }
    val activeList by viewModel.activeAppliances.collectAsState()

    Scaffold(
        // 1. Letakkan AuditTopBar di sini
        topBar = {
            AuditTopBar(viewModel = viewModel)
        },

        // 2. BottomBar untuk Koleksi Alat
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("KOLEKSI ALAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Menampilkan daftar alat yang bisa dipilih (pancingan data)
                        items(5) { index ->
                            IconButton(onClick = { /* Logic tambah alat ke canvas */ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            RoomCanvas(activeList) { appliance ->
                selectedAppliance = appliance
            }

            // Tampilkan Sheet jika ada alat yang diklik
            selectedAppliance?.let {
                ApplianceControlSheet(
                    appliance = it,
                    onSave = { newHour ->
                        // Update data di Firestore
                        selectedAppliance = null
                    },
                    onDismiss = { selectedAppliance = null }
                )
            }
        }
    }
}