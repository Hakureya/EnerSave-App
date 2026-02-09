package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thunderstormhan.enersave.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    // State untuk memantau status loading (saat tombol ditekan)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // State untuk memantau pesan error (jika login gagal)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // State untuk status login (jika berhasil, navigasi ke Home)
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    // Fungsi Login yang dipanggil dari LoginScreen
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.login(email, pass)

            result.onSuccess {
                _loginSuccess.value = true
            }.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Terjadi kesalahan saat masuk"
            }

            _isLoading.value = false
        }
    }

    // Fungsi Register yang dipanggil dari RegisterScreen
    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.register(email, pass)

            result.onSuccess {
                _loginSuccess.value = true
            }.onFailure { exception ->
                _errorMessage.value = exception.localizedMessage ?: "Pendaftaran gagal"
            }

            _isLoading.value = false
        }
    }

    fun resetState() {
        _errorMessage.value = null
        _loginSuccess.value = false
    }
}