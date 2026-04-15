package com.thunderstormhan.enersave.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val monthlyBill: Long = 0,
    val points: Int = 0,
    val co2Saved: Double = 0.0,
    val avatarUrl: String = "",
    val ownedSkins: List<String> = listOf("default")
)