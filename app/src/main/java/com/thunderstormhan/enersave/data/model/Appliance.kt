package com.thunderstormhan.enersave.data.model

data class Appliance(
    val id: String = "",
    val name: String = "",
    val watt: Int = 0,
    val hourUsage: Float = 0f,
    val iconName: String = "",
    val isSwitchedOn: Boolean = true,
    // Tambahkan posisi awal (dalam DP atau Float)
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val modelPath: String = ""
) {
    // Menghitung estimasi biaya bulanan (Rp 1.500 per kWh)
    fun calculateMonthlyCost(): Long {
        return ((watt * hourUsage * 30) / 1000 * 1500).toLong()
    }
}