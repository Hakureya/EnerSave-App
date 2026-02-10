package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.thunderstormhan.enersave.viewmodel.AuditViewModel

@Composable
fun AuditScreen(viewModel: AuditViewModel) {
    var selectedAppliance by remember { mutableStateOf<Appliance?>(null) }
    val activeList by viewModel.activeAppliances.collectAsState()
    val collection by viewModel.availableCollection.collectAsState()

    Scaffold(
        topBar = {
            AuditTopBar(viewModel = viewModel)
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Text("KOLEKSI ALAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(collection) { item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.addApplianceToCanvas(item) }
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF3F4F6)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(getEmojiForIcon(item.iconName), fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.name, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            RoomCanvas(
                activeAppliances = activeList,
                onApplianceClick = { selectedAppliance = it },
                onApplianceDrag = { id, x, y -> viewModel.updateAppliancePosition(id, x, y) }
            )

            if (activeList.isEmpty()) {
                Text(
                    text = "Ketuk alat di bawah untuk\nmemulai simulasi audit",
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            selectedAppliance?.let { appliance ->
                ApplianceControlSheet(
                    appliance = appliance,
                    onSave = { newHours ->
                        viewModel.updateApplianceUsage(appliance.id, newHours)
                        selectedAppliance = null
                    },
                    onDismiss = { selectedAppliance = null }
                )
            }
        }
    }
}