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
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditTopBar(viewModel: AuditViewModel) {
    val totalCost  by viewModel.totalDailyCost.collectAsState()
    val isSaving   by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    Surface(shadowElevation = 4.dp, color = Color.White) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Simulasi Ruangan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("LIVE AUDIT MODE", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Cost badge
                Surface(
                    color = Color(0xFF2563EB),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text     = "Rp $totalCost/hari",
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save button
                Surface(
                    onClick = { viewModel.saveAll() },
                    color   = when {
                        isSaving    -> Color(0xFFE5E7EB)
                        saveSuccess -> Color(0xFF22C55E)
                        else        -> Color(0xFF4F8EF7)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(10.dp),
                                color     = Color.Gray,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Text(
                                text  = if (saveSuccess) "✓" else "💾",
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text  = when {
                                isSaving    -> "Menyimpan..."
                                saveSuccess -> "Tersimpan"
                                else        -> "Simpan"
                            },
                            color      = if (isSaving) Color.Gray else Color.White,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}