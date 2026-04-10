package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.thunderstormhan.enersave.data.model.Appliance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceActionSheet(
    appliance: Appliance,
    onOk: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onEditHours: () -> Unit,  // opens existing ApplianceControlSheet
    onDismiss: () -> Unit
) {
    val dailyKwh  = appliance.calculateDailyKwh()
    val dailyCost = appliance.calculateDailyCost()

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
                        text = "${appliance.watt}W  •  ${appliance.rotationY.toInt()}° rotasi",
                        fontSize = 12.sp,
                        color = Color.Gray
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
                    InfoStat(
                        label = "Penggunaan/hari",
                        value = if (appliance.hourUsage > 0) "${appliance.hourUsage}h" else "Belum diatur"
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .align(Alignment.CenterVertically),
                        color = Color(0xFFD0D8F0)
                    )
                    InfoStat(
                        label = "Konsumsi",
                        value = if (dailyKwh > 0) "%.2f kWh".format(dailyKwh) else "-"
                    )
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp)
                            .align(Alignment.CenterVertically),
                        color = Color(0xFFD0D8F0)
                    )
                    InfoStat(
                        label = "Biaya/hari",
                        value = if (dailyCost > 0) "Rp ${dailyCost}" else "-"
                    )
                }
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // OK — dismiss and keep
                ActionButton(
                    emoji = "✅",
                    label = "OK",
                    color = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                    onClick = { onOk(); onDismiss() }
                )

                // Rotate 45°
                ActionButton(
                    emoji = "🔄",
                    label = "Putar 45°",
                    color = Color(0xFF4F8EF7),
                    modifier = Modifier.weight(1f),
                    onClick = { onRotate() }
                )

                // Edit hours (info)
                ActionButton(
                    emoji = "⏱️",
                    label = "Jam Pakai",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onEditHours(); onDismiss() }
                )

                // Remove
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

@Composable
private fun InfoStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
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
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}