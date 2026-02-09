package com.thunderstormhan.enersave.data.model

data class WeatherResponse(
    val main: Main,
    val name: String
)

data class Main(
    val temp: Float
)