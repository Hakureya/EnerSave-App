package com.thunderstormhan.enersave.ui.screens.shop

import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.viewmodel.ShopItem
import com.thunderstormhan.enersave.viewmodel.ShopViewModel

@Composable
fun ShopScreen(viewModel: ShopViewModel) {
    val user by viewModel.user.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // --- HEADER GREEN AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF006B3F), Color(0xFF00A35C))),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Pic
                Surface(shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    //                     Text("👤", modifier = Modifier.wrapContentSize(), fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text("Eco Warrior", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.weight(1.0f))
                // Points Badge
                Surface(color = Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(user.points.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- CONTENT AREA ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Toko EnerSave", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Tukar poinmu dengan skin eksklusif.", fontSize = 12.sp, color = Color.Gray)

            Spacer(Modifier.height(16.dp))

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE9ECEF), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("Skin Perabot", "Donasi Poin").forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedTab = index },
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Text(title, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Items Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.shopItems) { item ->
                    val isOwned = user.ownedSkins.contains(item.modelPath)
                    ShopItemCard(item, isOwned) { viewModel.buyItem(item) }
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(item: ShopItem, isOwned: Boolean, onBuy: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.rarity, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if(item.rarity == "Rare") Color.Magenta else Color.Gray)
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                // Placeholder untuk gambar model (computer_2, dll)
                Text(if(item.modelPath.contains("computer")) "💻" else if(item.modelPath.contains("fan")) "🌀" else "❄️", fontSize = 40.sp)
            }
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(item.description, fontSize = 10.sp, color = Color.Gray, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isOwned,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isOwned) "Dimiliki" else "${item.price} Koin", fontSize = 12.sp)
            }
        }
    }
}