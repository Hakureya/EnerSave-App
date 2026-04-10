package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditScreen(viewModel: AuditViewModel) {
    var selectedAppliance by remember { mutableStateOf<Appliance?>(null) }
    var roomConfig        by remember { mutableStateOf<RoomConfig?>(null) }

    val activeList by viewModel.activeAppliances.collectAsState()
    val collection by viewModel.availableCollection.collectAsState()

    // Show room setup dialog until user confirms
    if (roomConfig == null) {
        RoomSetupDialog(onConfirm = { roomConfig = it })
        return
    }

    Scaffold(
        topBar = { AuditTopBar(viewModel = viewModel) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // --- LAYER 1: Isometric Room Canvas ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 170.dp)
            ) {
                IsometricRoomCanvas(
                    roomConfig         = roomConfig!!,
                    activeAppliances   = activeList,
                    onApplianceClick   = { selectedAppliance = it },
                    onApplianceDrag    = { id, dx, dy -> viewModel.updateAppliancePosition(id, dx, dy) },
                    onDelete           = { id -> viewModel.removeApplianceFromCanvas(id) }
                )
            }

            // --- LAYER 2: Empty state hint ---
            AnimatedVisibility(
                visible = activeList.isEmpty(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 200.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Ketuk alat di bawah untuk menambahkan ke ruangan",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            // --- LAYER 3: Appliance collection panel ---
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(170.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "KOLEKSI ALAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        items(items = collection, key = { it.id }) { item ->
                            ApplianceCollectionItem(
                                item    = item,
                                onClick = { viewModel.addApplianceToCanvas(item) }
                            )
                        }
                    }
                }
            }

            // --- LAYER 4: Control sheet ---
            selectedAppliance?.let { appliance ->
                ApplianceControlSheet(
                    appliance = appliance,
                    onSave = { newHours ->
                        viewModel.updateApplianceUsage(appliance.id, newHours)
                        selectedAppliance = null
                    },
                    onDismiss = { selectedAppliance = null }
                )
            }
        }
    }
}

@Composable
private fun ApplianceCollectionItem(item: Appliance, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = getEmojiForIcon(item.iconName), fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.name, fontSize = 9.sp, color = Color.DarkGray)
    }
}