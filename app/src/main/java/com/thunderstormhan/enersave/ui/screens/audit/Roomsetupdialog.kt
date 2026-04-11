package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// RoomConfig is now just a local UI helper — actual data stored in Room model
data class RoomConfig(
    val name: String,
    val widthMeters: Int,
    val heightMeters: Int
)

@Composable
fun RoomSetupDialog(
    onConfirm: (RoomConfig) -> Unit
) {
    var nameInput   by remember { mutableStateOf("Ruang Tamu") }
    var widthInput  by remember { mutableStateOf("4") }
    var heightInput by remember { mutableStateOf("3") }

    val width   = widthInput.toIntOrNull()?.coerceIn(2, 10) ?: 0
    val height  = heightInput.toIntOrNull()?.coerceIn(2, 10) ?: 0
    val isValid = width > 0 && height > 0 && nameInput.isNotBlank()

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏠", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tambah Ruangan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    "Beri nama dan ukuran ruangan kamu.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                // Room name input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Ruangan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F8EF7),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Size inputs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MeterInput("Lebar (m)",  widthInput)  { widthInput = it }
                    Text("×", fontSize = 24.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    MeterInput("Panjang (m)", heightInput) { heightInput = it }
                }

                // Room preview
                if (isValid) {
                    Spacer(Modifier.height(16.dp))
                    RoomPreviewMini(widthM = width, heightM = height)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${width}m × ${height}m = ${width * height} m²",
                        fontSize = 11.sp,
                        color = Color(0xFF4F8EF7),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isValid) onConfirm(RoomConfig(nameInput.trim(), width, height))
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F8EF7))
                ) {
                    Text("Buat Ruangan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun MeterInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 2) onValueChange(it) },
            modifier = Modifier.width(90.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4F8EF7),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun RoomPreviewMini(widthM: Int, heightM: Int) {
    val maxDim   = maxOf(widthM, heightM)
    val cellSize = (140f / maxDim).dp
    val roomW    = cellSize * widthM
    val roomH    = cellSize * heightM
    Box(
        modifier = Modifier
            .size(width = roomW + 4.dp, height = roomH + 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE8F0FE))
            .border(2.dp, Color(0xFF4F8EF7), RoundedCornerShape(4.dp))
    ) {
        for (col in 1 until widthM) {
            Box(modifier = Modifier
                .offset(x = cellSize * col + 2.dp, y = 2.dp)
                .width(1.dp).height(roomH)
                .background(Color(0xFF4F8EF7).copy(alpha = 0.25f)))
        }
        for (row in 1 until heightM) {
            Box(modifier = Modifier
                .offset(x = 2.dp, y = cellSize * row + 2.dp)
                .height(1.dp).width(roomW)
                .background(Color(0xFF4F8EF7).copy(alpha = 0.25f)))
        }
    }
}