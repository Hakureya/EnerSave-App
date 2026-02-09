package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance

@Composable
fun RoomCanvas(
    activeAppliances: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // gray-50
    ) {
        // Simulasi posisi alat (Contoh posisi statis untuk simulasi)
        activeAppliances.forEachIndexed { index, appliance ->
            val modifier = when(index) {
                0 -> Modifier.align(Alignment.TopCenter).offset(y = 50.dp) // Posisi AC
                1 -> Modifier.align(Alignment.Center).offset(x = (-60).dp) // Posisi TV
                else -> Modifier.align(Alignment.BottomCenter).offset(y = (-100).dp)
            }

            IsoItem(
                appliance = appliance,
                modifier = modifier.clickable { onApplianceClick(appliance) }
            )
        }
    }
}

@Composable
fun IsoItem(appliance: Appliance, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⚡", fontSize = 24.sp) // Ganti dengan Icon nyata sesuai iconName
                }
            }
            // Status Dot (On/Off)
            Surface(
                modifier = Modifier.size(12.dp).offset(x = 4.dp, y = (-4).dp),
                shape = RoundedCornerShape(50),
                color = if (appliance.isSwitchedOn) Color.Green else Color.Red,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {}
        }
        Text(
            text = appliance.name,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp),
            color = Color.DarkGray
        )
    }
}