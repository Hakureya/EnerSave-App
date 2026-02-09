package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditTopBar(viewModel: AuditViewModel = viewModel()) {
    // Mengamati perubahan daftar alat secara reaktif
    val appliances by viewModel.activeAppliances.collectAsState()

    // Total biaya akan otomatis terhitung setiap kali 'appliances' berubah
    val totalDaily = viewModel.calculateTotalDailyCost()

    Surface(shadowElevation = 4.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Simulasi Ruangan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("LIVE AUDIT MODE", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Menampilkan Total Biaya
            Surface(
                color = Color(0xFF2563EB), // blue-600
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Rp $totalDaily/hari",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}