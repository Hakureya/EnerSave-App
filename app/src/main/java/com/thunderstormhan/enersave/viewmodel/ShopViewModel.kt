package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import com.thunderstormhan.enersave.data.model.User
import com.thunderstormhan.enersave.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShopViewModel(private val repository: FirestoreRepository = FirestoreRepository()) : ViewModel() {
    // Simulasi data user
    private val _user = MutableStateFlow(User(points = 1000))
    val user: StateFlow<User> = _user.asStateFlow()

    // Daftar item di toko
    val shopItems = listOf(
        ShopItem("1", "Super Computer V2", "computer_2", 500, "Rare", "Desain futuristik dengan performa tinggi."),
        ShopItem("2", "Kipas Angin Turbo", "fan_2", 350, "Common", "Hembusan angin lebih kuat dan hemat energi."),
        ShopItem("3", "Kulkas Dapur Modern", "kitchen_fridge", 400, "Common", "Kapasitas besar dengan pendinginan optimal.")
    )

    fun buyItem(item: ShopItem) {
        val currentUser = _user.value
        if (currentUser.points >= item.price && !currentUser.ownedSkins.contains(item.modelPath)) {
            val updatedUser = currentUser.copy(
                points = currentUser.points - item.price,
                ownedSkins = currentUser.ownedSkins + item.modelPath
            )
            _user.value = updatedUser
            // Update ke repository/Firestore di sini
        }
    }
}

data class ShopItem(
    val id: String,
    val name: String,
    val modelPath: String,
    val price: Int,
    val rarity: String,
    val description: String
)