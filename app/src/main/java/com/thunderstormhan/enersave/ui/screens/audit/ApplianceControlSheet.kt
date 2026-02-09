package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceControlSheet(
    appliance: Appliance,
    onSave: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(appliance.hourUsage) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text(appliance.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${appliance.watt} Watt", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Durasi Pemakaian", fontSize = 14.sp)
                Text("${sliderValue.toInt()} Jam/Hari", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 0f..24f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF16A34A), activeTrackColor = Color(0xFF16A34A))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEFF6FF), // blue-50
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Biaya / bulan", fontSize = 10.sp, color = Color.Blue)
                        // Hitung biaya dinamis berdasarkan slider
                        val tempAppliance = appliance.copy(hourUsage = sliderValue)
                        Text("Rp ${tempAppliance.calculateMonthlyCost()}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(onClick = { onSave(sliderValue) }) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}