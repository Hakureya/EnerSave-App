package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import com.thunderstormhan.enersave.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    // State yang akan dipantau oleh Jetpack Compose
    private val _userData = MutableStateFlow(User())
    val userData: StateFlow<User> = _userData

    fun loadUser() {
        // Logika ambil data dari repository di sini
    }
}