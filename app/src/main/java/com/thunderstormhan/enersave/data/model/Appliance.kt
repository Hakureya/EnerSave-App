package com.thunderstormhan.enersave.data.model

data class UserAppliance(
    val id: String = "",          // ID unik dokumen
    val name: String = "",        // Contoh: "Lampu Meja"
    val watts: Int = 0,           // Konsumsi daya
    val hoursPerDay: Float = 0f,  // Durasi penggunaan
    val posX: Float = 0f,         // Posisi X di lantai (0.0 - 100.0)
    val posY: Float = 0f,         // Posisi Y di lantai (0.0 - 100.0)
    val iconTag: String = "",     // String penanda ikon (misal: "bulb", "ac", "tv")
    val isOn: Boolean = false     // Status saklar
)

