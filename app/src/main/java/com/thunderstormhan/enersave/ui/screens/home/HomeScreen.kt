package com.thunderstormhan.enersave.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.EnergySavingsLeaf

import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Weather Card (Blue Gradient)
            item {
                WeatherCard()
            }

            // 2. Daily Streak Section
            item {
                StreakCard()
            }

            // 3. Monthly Challenge Card (Orange)
            item {
                ChallengeCard()
            }

            // 4. Did You Know Section (Green Border)
            item {
                FactCard()
            }

            // Spacer bawah untuk navigasi bar nanti
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun WeatherCard() {
    // State untuk data cuaca
    var temperature by remember { mutableStateOf("...") }
    var cityName by remember { mutableStateOf("Memuat...") }

    // Ambil data saat layar dibuka
    LaunchedEffect(Unit) {
        try {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()

            val service = retrofit.create(com.thunderstormhan.enersave.data.repository.WeatherService::class.java)

            // Ganti "YOUR_API_KEY" dengan API Key aslimu
            val response = service.getWeather("Jakarta", "YOUR_API_KEY")

            temperature = "${response.main.temp.toInt()}°C"
            cityName = response.name
        } catch (e: Exception) {
            cityName = "Gagal memuat"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.Yellow)
                Spacer(modifier = Modifier.width(8.dp))
                // Menampilkan data realtime
                Text("$temperature • $cityName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            val weatherTip = if (temperature.replace("°C", "").toIntOrNull() ?: 0 > 30)
                "Cuaca panas terik!" else "Cuaca sedang sejuk."

            Text(weatherTip, color = Color(0xFFDBEAFE), fontSize = 12.sp)

            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                val tip = if (temperature.contains("3")) "💡 Tip: Tutup gorden, AC set 24°C saja."
                else "💡 Tip: Matikan AC, buka jendela."
                Text(
                    tip,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StreakCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Streak Harian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("5 Hari!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Representasi hari 1-4 (Selesai)
                repeat(4) { StreakIcon(status = "done") }
                // Hari ke-5 (Aktif)
                StreakIcon(status = "active")
                // Hari ke-6 & 7 (Terkunci)
                repeat(2) { StreakIcon(status = "locked") }
            }
        }
    }
}

@Composable
fun StreakIcon(status: String) {
    val bgColor = when(status) {
        "done" -> Color(0xFFFFEDD5)
        "active" -> Color(0xFFF97316)
        else -> Color(0xFFF3F4F6)
    }
    val contentColor = when(status) {
        "done" -> Color(0xFFF97316)
        "active" -> Color.White
        else -> Color(0xFFD1D5DB)
    }

    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = bgColor,
        border = if(status == "done") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            when(status) {
                "done" -> Text("🔥", fontSize = 14.sp)
                "active" -> Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = contentColor)
                "locked" -> Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = contentColor)
            }
        }
    }
}

@Composable
fun ChallengeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEDD5), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text("TANTANGAN BULAN INI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
            Text("Puasa Listrik 1 Jam", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text("Matikan semua alat pukul 17:00 - 18:00.", fontSize = 12.sp, color = Color(0xFF4B5563))
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                shape = RoundedCornerShape(50)
            ) {
                Text("Ikuti Tantangan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FactCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = Color(0xFFDCFCE7), shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.EnergySavingsLeaf, null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Tahukah Kamu?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Mode 'Sleep' laptop tetap memakan daya 15 Watt.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}