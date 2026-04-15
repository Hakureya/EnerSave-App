package com.thunderstormhan.enersave.data.model

data class Appliance(
    val id: String = "",
    val name: String = "",
    val watt: Int = 0,
    val hourUsage: Float = 0f,
    val iconName: String = "",
    val isSwitchedOn: Boolean = true,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val modelPath: String = "",
    val rotationY: Float = 0f, // rotation in degrees, snaps to 0/45/90/135/180/225/270/315
    val activeModel: String = "default",
    val type: String = "other"
) {
    fun calculateMonthlyCost(): Long {
        return ((watt * hourUsage * 30) / 1000 * 1500).toLong()
    }

    fun calculateDailyCost(): Long {
        return ((watt * hourUsage) / 1000 * 1500).toLong()
    }

    fun calculateDailyKwh(): Float {
        return (watt * hourUsage) / 1000f
    }
}