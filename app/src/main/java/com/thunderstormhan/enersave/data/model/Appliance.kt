package com.thunderstormhan.enersave.data.model

data class Appliance(
    val id: String = "",
    val name: String = "",
    val watt: Int = 0,
    val hourUsage: Float = 0f,
    val iconName: String = "", // misal: "fan", "bolt"
    val colorHex: String = "#3B82F6",
    val isSwitchedOn: Boolean = true
) {
    // Menghitung estimasi biaya bulanan (Rp 1.500 per kWh)
    fun calculateMonthlyCost(): Long {
        return ((watt * hourUsage * 30) / 1000 * 1500).toLong()
    }
}