package com.thunderstormhan.enersave.ui.screens.audit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.model.Room
import com.thunderstormhan.enersave.viewmodel.AuditViewModel
import kotlin.math.roundToInt

@Composable
fun AuditScreen(viewModel: AuditViewModel) {
    var actionAppliance  by remember { mutableStateOf<Appliance?>(null) }
    var controlAppliance by remember { mutableStateOf<Appliance?>(null) }
    var showAddRoom      by remember { mutableStateOf(false) }

    val rooms         by viewModel.rooms.collectAsState()
    val currentRoomId by viewModel.currentRoomId.collectAsState()
    val isMainRoom    by viewModel.isMainRoom.collectAsState()
    val activeList    by viewModel.activeAppliancesFlow.collectAsState()
    val collection    by viewModel.availableCollection.collectAsState()

    val currentRoom = rooms.find { it.id == currentRoomId }

    val placedRoomIds     by viewModel.placedRoomIds.collectAsState()
    val mainRoomPositions by viewModel.mainRoomPositions.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val isOnline by remember {
        derivedStateOf {
            val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
    }

    // Keep action sheet in sync with live list
    val liveActionAppliance = actionAppliance?.let { a ->
        activeList.find { it.id == a.id }
    }

    // Wait for Firestore load before deciding whether to show dialog
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF4F8EF7))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Memuat data...", fontSize = 13.sp, color = Color.Gray)
            }
        }
        return
    }

    // Show setup dialog only after load is done and rooms are truly empty
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

            // --- CONNECTIVITY BANNER ---
            if (!isOnline) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color(0xFFEF4444)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = "Tidak ada koneksi internet. Perubahan tidak akan tersimpan.",
                            fontSize   = 11.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- LAYER 1: Canvas ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 170.dp)
            ) {
                if (isMainRoom) {
                    MainRoomCanvas(
                        rooms         = rooms,
                        placements    = placedRoomIds,
                        savedPositions = mainRoomPositions,
                        onPositionChanged = { roomId, x, y ->
                            viewModel.updateMainRoomPosition(roomId, x, y)
                        }
                    )
                } else {
                    currentRoom?.let { room ->
                        val roomConfig = RoomConfig(room.name, room.widthMeters, room.heightMeters)
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
            }

            // --- LAYER 2: Room selector dropdown (top of canvas) ---
            RoomSelectorBar(
                rooms             = rooms,
                currentRoom       = currentRoom,
                isMainRoom        = isMainRoom,
                onSelectMainRoom  = { viewModel.switchToMainRoom() },
                onSelectRoom      = { viewModel.switchRoom(it.id) },
                onAddRoom         = { showAddRoom = true },
                onDeleteRoom      = { viewModel.deleteRoom(it.id) },
                modifier          = Modifier
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

            // --- LAYER 4: Bottom panel (rooms panel in main room, appliances otherwise) ---
            Surface(
                modifier        = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp),
                color           = Color.White,
                shadowElevation = 16.dp,
                shape           = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                if (isMainRoom) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text       = "RUANGAN",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.LightGray,
                            modifier   = Modifier.padding(horizontal = 24.dp)
                        )
                        Text(
                            text     = "Ketuk untuk menambah atau melepas ruangan",
                            fontSize = 9.sp,
                            color    = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding        = PaddingValues(horizontal = 24.dp)
                        ) {
                            items(items = rooms, key = { it.id }) { room ->
                                val isPlaced = placedRoomIds.contains(room.id)
                                RoomChip(
                                    room     = room,
                                    isPlaced = isPlaced,
                                    onClick  = { viewModel.toggleRoomOnMainCanvas(room.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Appliance panel
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
    isMainRoom: Boolean,
    onSelectMainRoom: () -> Unit,
    onSelectRoom: (Room) -> Unit,
    onAddRoom: () -> Unit,
    onDeleteRoom: (Room) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Display label for the trigger button
    val displayName = when {
        isMainRoom   -> "Rumah Utama"
        currentRoom != null -> currentRoom.name
        else         -> "Pilih Ruangan"
    }
    val displaySub = when {
        isMainRoom   -> "${rooms.size} ruangan terhubung"
        currentRoom != null -> "${currentRoom.widthMeters}m × ${currentRoom.heightMeters}m"
        else         -> ""
    }

    Box(modifier = modifier) {
        // Trigger button
        Surface(
            onClick         = { expanded = true },
            shape           = RoundedCornerShape(12.dp),
            color           = Color.White,
            shadowElevation = 4.dp,
            modifier        = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isMainRoom) "🏘️" else "🏠",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text       = displayName,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF1A1A2E)
                        )
                        if (displaySub.isNotEmpty()) {
                            Text(text = displaySub, fontSize = 11.sp, color = Color.Gray)
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
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier.fillMaxWidth(0.9f)
        ) {
            // Main room option (always first)
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏘️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(
                                text       = "Rumah Utama",
                                fontWeight = if (isMainRoom) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isMainRoom) Color(0xFF4F8EF7) else Color(0xFF1A1A2E)
                            )
                            Text(
                                text     = "Semua ruangan terhubung",
                                fontSize = 11.sp,
                                color    = Color.Gray
                            )
                        }
                    }
                },
                onClick = {
                    expanded = false
                    onSelectMainRoom()
                }
            )

            Divider()

            // Individual rooms
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
private fun RoomChip(room: com.thunderstormhan.enersave.data.model.Room, isPlaced: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(14.dp),
        color   = if (isPlaced) Color(0xFF4F8EF7).copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        border  = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isPlaced) Color(0xFF4F8EF7) else Color.Transparent
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("🏠", fontSize = 26.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = room.name,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                color      = if (isPlaced) Color(0xFF4F8EF7) else Color.DarkGray
            )
            Text(
                text     = "${room.widthMeters}×${room.heightMeters}m",
                fontSize = 8.sp,
                color    = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = if (isPlaced) "✓ Ditambahkan" else "+ Tambahkan",
                fontSize = 8.sp,
                color    = if (isPlaced) Color(0xFF4F8EF7) else Color.Gray
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