package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.model.Room
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditScreen(viewModel: AuditViewModel) {
    var actionAppliance  by remember { mutableStateOf<Appliance?>(null) }
    var controlAppliance by remember { mutableStateOf<Appliance?>(null) }
    var showAddRoom      by remember { mutableStateOf(false) }

    val rooms         by viewModel.rooms.collectAsState()
    val currentRoomId by viewModel.currentRoomId.collectAsState()
    val activeList    by viewModel.activeAppliancesFlow.collectAsState()
    val collection    by viewModel.availableCollection.collectAsState()

    val currentRoom = rooms.find { it.id == currentRoomId }

    // Keep action sheet in sync with live list
    val liveActionAppliance = actionAppliance?.let { a ->
        activeList.find { it.id == a.id }
    }

    // Show setup dialog if no rooms exist yet
    if (rooms.isEmpty() || showAddRoom) {
        RoomSetupDialog(
            onConfirm = { config ->
                viewModel.addRoom(config.name, config.widthMeters, config.heightMeters)
                showAddRoom = false
            }
        )
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
            currentRoom?.let { room ->
                val roomConfig = RoomConfig(room.name, room.widthMeters, room.heightMeters)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 170.dp)
                ) {
                    IsometricRoomCanvas(
                        roomConfig       = roomConfig,
                        activeAppliances = activeList,
                        onApplianceClick = { appliance ->
                            actionAppliance = activeList.find { it.id == appliance.id } ?: appliance
                        },
                        onApplianceDrag  = { id, dx, dy -> viewModel.updateAppliancePosition(id, dx, dy) },
                        onDelete         = { id -> viewModel.removeApplianceFromCanvas(id) }
                    )
                }
            }

            // --- LAYER 2: Room selector dropdown (top of canvas) ---
            RoomSelectorBar(
                rooms         = rooms,
                currentRoom   = currentRoom,
                onSelectRoom  = { viewModel.switchRoom(it.id) },
                onAddRoom     = { showAddRoom = true },
                onDeleteRoom  = { viewModel.deleteRoom(it.id) },
                modifier      = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )

            // --- LAYER 3: Empty state ---
            AnimatedVisibility(
                visible  = activeList.isEmpty(),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 200.dp),
                enter    = fadeIn(),
                exit     = fadeOut()
            ) {
                Text(
                    text     = "Ketuk alat di bawah untuk menambahkan ke ruangan",
                    color    = Color.Gray,
                    fontSize = 13.sp
                )
            }

            // --- LAYER 4: Appliance collection panel ---
            Surface(
                modifier        = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp),
                color           = Color.White,
                shadowElevation = 16.dp,
                shape           = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text       = "KOLEKSI ALAT",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.LightGray,
                        modifier   = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding        = PaddingValues(horizontal = 24.dp)
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

            // --- LAYER 5: Action sheet ---
            liveActionAppliance?.let { appliance ->
                ApplianceActionSheet(
                    appliance   = appliance,
                    onOk        = { actionAppliance = null },
                    onRotate    = { viewModel.rotateAppliance(appliance.id) },
                    onRemove    = {
                        viewModel.removeApplianceFromCanvas(appliance.id)
                        actionAppliance = null
                    },
                    onEditHours = {
                        controlAppliance = appliance
                        actionAppliance  = null
                    },
                    onDismiss   = { actionAppliance = null }
                )
            }

            // --- LAYER 6: Hour usage editor ---
            controlAppliance?.let { appliance ->
                ApplianceControlSheet(
                    appliance = appliance,
                    onSave    = { newHours ->
                        viewModel.updateApplianceUsage(appliance.id, newHours)
                        controlAppliance = null
                    },
                    onDismiss = { controlAppliance = null }
                )
            }
        }
    }
}

// ── Room selector dropdown bar ────────────────────────────────────────────────
@Composable
private fun RoomSelectorBar(
    rooms: List<Room>,
    currentRoom: Room?,
    onSelectRoom: (Room) -> Unit,
    onAddRoom: () -> Unit,
    onDeleteRoom: (Room) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Trigger button
        Surface(
            onClick      = { expanded = true },
            shape        = RoundedCornerShape(12.dp),
            color        = Color.White,
            shadowElevation = 4.dp,
            modifier     = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏠", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            text       = currentRoom?.name ?: "Pilih Ruangan",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1A1A2E)
                        )
                        currentRoom?.let {
                            Text(
                                text     = "${it.widthMeters}m × ${it.heightMeters}m",
                                fontSize = 11.sp,
                                color    = Color.Gray
                            )
                        }
                    }
                }
                Icon(
                    imageVector        = Icons.Default.ArrowDropDown,
                    contentDescription = "Pilih ruangan",
                    tint               = Color.Gray
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded        = expanded,
            onDismissRequest = { expanded = false },
            modifier        = Modifier.fillMaxWidth(0.9f)
        ) {
            // Existing rooms
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier             = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text       = room.name,
                                    fontWeight = if (room.id == currentRoom?.id) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (room.id == currentRoom?.id) Color(0xFF4F8EF7) else Color(0xFF1A1A2E)
                                )
                                Text(
                                    text     = "${room.widthMeters}m × ${room.heightMeters}m  •  ${room.appliances.size} alat",
                                    fontSize = 11.sp,
                                    color    = Color.Gray
                                )
                            }
                            // Delete button (only if more than 1 room)
                            if (rooms.size > 1) {
                                IconButton(
                                    onClick = {
                                        expanded = false
                                        onDeleteRoom(room)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Delete,
                                        contentDescription = "Hapus ruangan",
                                        tint               = Color(0xFFEF4444),
                                        modifier           = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectRoom(room)
                    }
                )
            }

            Divider()

            // Add new room button
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.Add,
                            contentDescription = "Tambah ruangan",
                            tint               = Color(0xFF4F8EF7),
                            modifier           = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text  = "Tambah Ruangan Baru",
                            color = Color(0xFF4F8EF7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onAddRoom()
                }
            )
        }
    }
}

@Composable
private fun ApplianceCollectionItem(item: Appliance, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape    = RoundedCornerShape(12.dp),
            color    = Color(0xFFF1F5F9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = getEmojiForIcon(item.iconName), fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.name, fontSize = 9.sp, color = Color.DarkGray)
    }
}