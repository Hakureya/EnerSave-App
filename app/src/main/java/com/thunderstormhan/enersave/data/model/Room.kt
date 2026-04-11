package com.thunderstormhan.enersave.data.model

data class Room(
    val id: String = "",
    val name: String = "",
    val widthMeters: Int = 3,
    val heightMeters: Int = 3,
    val appliances: List<Appliance> = emptyList()
)