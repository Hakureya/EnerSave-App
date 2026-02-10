package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import kotlin.math.roundToInt

@Composable
fun RoomCanvas(
    activeAppliances: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit,
    onApplianceDrag: (String, Float, Float) -> Unit // Callback baru untuk drag
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // gray-50
    ) {
        activeAppliances.forEach { appliance ->
            // Wrapper untuk setiap item agar bisa di-drag
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            appliance.positionX.roundToInt(),
                            appliance.positionY.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Kirim perubahan posisi ke ViewModel
                            onApplianceDrag(appliance.id, dragAmount.x, dragAmount.y)
                        }
                    }
            ) {
                IsoItem(
                    appliance = appliance,
                    modifier = Modifier.padding(8.dp),
                    onClick = { onApplianceClick(appliance) }
                )
            }
        }
    }
}

@Composable
fun IsoItem(
    appliance: Appliance,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.size(80.dp).padding(4.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().clickable { onClick() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(getEmojiForIcon(appliance.iconName), fontSize = 32.sp)
                }
            }

            // Indikator Status
            val isActive = appliance.hourUsage > 0
            Surface(
                modifier = Modifier.size(14.dp).offset(x = 2.dp, y = (-2).dp),
                shape = RoundedCornerShape(50),
                color = if (isActive) Color(0xFF22C55E) else Color(0xFFEF4444),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {}
        }
        Text(appliance.name, fontSize = 10.sp, color = Color.Gray)
    }
}

// Fungsi pembantu untuk menentukan icon visual (Sesuai dengan pembahasan sebelumnya)
fun getEmojiForIcon(iconName: String): String {
    return when (iconName.lowercase()) {
        "wind", "ac" -> "❄️"
        "tv", "television" -> "📺"
        "fan" -> "🌀"
        "computer", "pc" -> "💻"
        "lightbulb", "lamp" -> "💡"
        "snowflake", "fridge" -> "🧊"
        "bolt" -> "⚡"
        else -> "🔌"
    }
}