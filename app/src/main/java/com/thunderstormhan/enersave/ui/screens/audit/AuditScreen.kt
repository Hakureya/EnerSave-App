package com.thunderstormhan.enersave.ui.screens.audit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.model.Room
import com.thunderstormhan.enersave.viewmodel.AuditViewModel
import com.thunderstormhan.enersave.viewmodel.ShopViewModel

@Composable
fun AuditScreen(viewModel: AuditViewModel, shopViewModel: ShopViewModel) {
    // State untuk mengontrol sheet mana yang muncul
    var actionAppliance  by remember { mutableStateOf<Appliance?>(null) }
    var controlAppliance by remember { mutableStateOf<Appliance?>(null) }
    var showAddRoom      by remember { mutableStateOf(false) }

    val userState by shopViewModel.user.collectAsState()

    // Observasi State dari ViewModel
    val rooms             by viewModel.rooms.collectAsState()
    val currentRoomId     by viewModel.currentRoomId.collectAsState()
    val isMainRoom        by viewModel.isMainRoom.collectAsState()
    val activeList        by viewModel.activeAppliancesFlow.collectAsState()
    val collection        by viewModel.availableCollection.collectAsState()
    val placedRoomIds     by viewModel.placedRoomIds.collectAsState()
    val mainRoomPositions by viewModel.mainRoomPositions.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()

    // Data simulasi skin yang sudah dibeli dari toko
    // (Dalam aplikasi nyata, ini diambil dari UserState/ViewModel)
    val ownedSkins = userState.ownedSkins

    val currentRoom = rooms.find { it.id == currentRoomId }
    val context = LocalContext.current

    // Cek koneksi internet
    val isOnline by remember {
        derivedStateOf {
            val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
    }

    // Pastikan Action Sheet sinkron dengan data alat terbaru (misal setelah ganti skin)
    val liveActionAppliance = actionAppliance?.let { a ->
        activeList.find { it.id == a.id }
    }

    // 1. Loading State
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

    // 2. Setup Awal (Jika belum ada ruangan)
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

            // --- KONEKSI INTERNET BANNER ---
            if (!isOnline) {
                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Color(0xFFEF4444)
                ) {
                    Text(
                        text = "⚠️ Mode Offline - Perubahan mungkin tidak tersinkron.",
                        fontSize = 11.sp, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // --- LAYER 1: Canvas (Isometric / Main) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 170.dp) // Beri ruang untuk panel bawah
            ) {
                if (isMainRoom) {
                    MainRoomCanvas(
                        rooms          = rooms,
                        placements     = placedRoomIds,
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

            // --- LAYER 2: Dropdown Pemilih Ruangan ---
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
                    .padding(top = 24.dp, start = 12.dp, end = 12.dp)
            )

            // --- LAYER 3: Hint jika ruangan kosong ---
            AnimatedVisibility(
                visible  = activeList.isEmpty() && !isMainRoom,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 200.dp),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Text("Ketuk alat di bawah untuk menambah ke ruangan", color = Color.Gray, fontSize = 13.sp)
            }

            // --- LAYER 4: Panel Bawah (Koleksi Alat / List Ruangan) ---
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val label = if (isMainRoom) "DAFTAR RUANGAN" else "KOLEKSI ALAT"
                    Text(
                        text = label,
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        if (isMainRoom) {
                            items(items = rooms, key = { it.id }) { room ->
                                RoomChip(
                                    room = room,
                                    isPlaced = placedRoomIds.contains(room.id),
                                    onClick = { viewModel.toggleRoomOnMainCanvas(room.id) }
                                )
                            }
                        } else {
                            items(items = collection, key = { it.id }) { item ->
                                ApplianceCollectionItem(
                                    item = item,
                                    onClick = { viewModel.addApplianceToCanvas(item) }
                                )
                            }
                        }
                    }
                }
            }

            // --- LAYER 5: Action Sheet (Ganti Skin & Rotasi) ---
            liveActionAppliance?.let { appliance ->
                ApplianceActionSheet(
                    appliance    = appliance,
                    ownedSkins   = ownedSkins, // Mengirim daftar skin yang dibeli
                    onOk         = { actionAppliance = null },
                    onRotate     = { viewModel.rotateAppliance(appliance.id) },
                    onRemove     = {
                        viewModel.removeApplianceFromCanvas(appliance.id)
                        actionAppliance = null
                    },
                    onEditHours  = {
                        controlAppliance = appliance
                        actionAppliance  = null
                    },
                    onUpdateSkin = { newModel ->
                        viewModel.updateApplianceSkin(appliance.id, newModel)
                    },
                    onDismiss    = { actionAppliance = null }
                )
            }

            // --- LAYER 6: Kontrol Jam Penggunaan ---
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

// ── Komponen UI Kecil ─────────────────────────────────────────────────────────

@Composable
private fun RoomChip(room: Room, isPlaced: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isPlaced) Color(0xFF4F8EF7).copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaced) Color(0xFF4F8EF7) else Color.Transparent)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("🏠", fontSize = 24.sp)
            Text(room.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (isPlaced) "Terpasang" else "Gunakan", fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ApplianceCollectionItem(item: Appliance, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(modifier = Modifier.size(60.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F5F9)) {
            Box(contentAlignment = Alignment.Center) {
                Text(getEmojiForIcon(item.iconName), fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.name, fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RoomSelectorBar(
    rooms: List<Room>, currentRoom: Room?, isMainRoom: Boolean,
    onSelectMainRoom: () -> Unit, onSelectRoom: (Room) -> Unit,
    onAddRoom: () -> Unit, onDeleteRoom: (Room) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (isMainRoom) "🏘️ Rumah Utama" else "🏠 ${currentRoom?.name}"

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            DropdownMenuItem(text = { Text("🏘️ Rumah Utama") }, onClick = { onSelectMainRoom(); expanded = false })
            Divider()
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(room.name)
                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp).clickable { onDeleteRoom(room) })
                        }
                    },
                    onClick = { onSelectRoom(room); expanded = false }
                )
            }
            Divider()
            DropdownMenuItem(text = { Text("➕ Tambah Ruangan", color = Color(0xFF4F8EF7)) }, onClick = { onAddRoom(); expanded = false })
        }
    }
}