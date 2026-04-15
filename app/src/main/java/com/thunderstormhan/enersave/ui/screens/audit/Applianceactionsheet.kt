package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.thunderstormhan.enersave.data.model.Appliance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceActionSheet(
    appliance: Appliance,
    ownedSkins: List<String> = listOf("default"), // Ambil dari data User
    onOk: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onEditHours: () -> Unit,
    onUpdateSkin: (String) -> Unit, // Callback baru untuk ganti model
    onDismiss: () -> Unit
) {
    val dailyKwh  = appliance.calculateDailyKwh()
    val dailyCost = appliance.calculateDailyCost()
    var showSkinPicker by remember { mutableStateOf(false) }

    // --- DIALOG PEMILIH SKIN ---
    if (showSkinPicker) {
        SkinSelectorDialog(
            appliance = appliance,
            ownedSkins = ownedSkins,
            onSelect = { selectedSkin ->
                onUpdateSkin(selectedSkin)
                showSkinPicker = false
            },
            onDismiss = { showSkinPicker = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFE0E0E0), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            ) {
                Text(
                    text = getEmojiForIcon(appliance.iconName),
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = appliance.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                    Text(
                        text = "Model: ${if (appliance.activeModel == "default") "Standar" else appliance.activeModel}",
                        fontSize = 12.sp,
                        color = Color(0xFF4F8EF7),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Info card — daily usage
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0F4FF)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoStat(label = "Penggunaan", value = if (appliance.hourUsage > 0) "${appliance.hourUsage}h" else "-")
                    VerticalDivider()
                    InfoStat(label = "Konsumsi", value = if (dailyKwh > 0) "%.2f kWh".format(dailyKwh) else "-")
                    VerticalDivider()
                    InfoStat(label = "Biaya/hari", value = if (dailyCost > 0) "Rp ${dailyCost}" else "-")
                }
            }

            // Action buttons row
            // Menggunakan FlowRow atau Grid agar 5 tombol muat dengan rapi
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(emoji = "✅", label = "Selesai", color = Color(0xFF22C55E), modifier = Modifier.weight(1f), onClick = { onOk(); onDismiss() })
                    ActionButton(emoji = "🔄", label = "Rotasi", color = Color(0xFF4F8EF7), modifier = Modifier.weight(1f), onClick = { onRotate() })
                    ActionButton(emoji = "⏱️", label = "Waktu", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f), onClick = { onEditHours(); onDismiss() })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // TOMBOL GANTI SKIN
                    ActionButton(
                        emoji = "✨",
                        label = "Ganti Skin",
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        onClick = { showSkinPicker = true }
                    )
                    ActionButton(
                        emoji = "🗑️",
                        label = "Hapus",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        onClick = { onRemove(); onDismiss() }
                    )
                }
            }
        }
    }
}

// ── Skin Selector Dialog ──────────────────────────────────────────────────────
@Composable
fun SkinSelectorDialog(
    appliance: Appliance,
    ownedSkins: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Pilih Tampilan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            val availableSkins = ownedSkins.filter {
                it == "default" || it.contains(appliance.type, ignoreCase = true)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableSkins) { skin ->
                    val isCurrent = appliance.activeModel == skin
                    Surface(
                        onClick = { onSelect(skin) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) Color(0xFFF0F4FF) else Color(0xFFF8F9FA),
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4F8EF7)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (skin == "default") "📦" else "✨", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (skin == "default") "Model Standar" else skin.replace(".glb", "").replace("_", " ").capitalize(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isCurrent) Text("Sedang digunakan", fontSize = 11.sp, color = Color(0xFF4F8EF7))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = Color.Gray) }
        }
    )
}

@Composable
private fun VerticalDivider() {
    Divider(
        modifier = Modifier.height(40.dp).width(1.dp).padding(vertical = 4.dp),
        color = Color(0xFFD0D8F0)
    )
}

@Composable
private fun InfoStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E), textAlign = TextAlign.Center)
        Text(text = label, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActionButton(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}