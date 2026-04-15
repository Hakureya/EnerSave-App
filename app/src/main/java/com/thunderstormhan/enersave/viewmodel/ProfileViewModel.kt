package com.thunderstormhan.enersave.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thunderstormhan.enersave.data.repository.AuthRepository
import com.thunderstormhan.enersave.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val storageRepository: StorageRepository = StorageRepository() // Repositori baru
) : ViewModel() {

    private val _userName = MutableStateFlow(authRepository.currentUser?.displayName ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<Uri?>(authRepository.currentUser?.photoUrl)
    val profilePhotoUri: StateFlow<Uri?> = _profilePhotoUri.asStateFlow()

    fun updateProfilePhoto(uri: Uri) {
        val user = authRepository.currentUser ?: return

        // 1. Update UI secara instan agar pengguna merasa aplikasi responsif
        _profilePhotoUri.value = uri

        viewModelScope.launch {
            // 2. Unggah gambar ke Firebase Storage
            val uploadResult = storageRepository.uploadProfileImage(user.uid, uri)

            uploadResult.onSuccess { downloadUrl ->
                // 3. Jika berhasil, simpan URL publik tersebut ke profil Firebase Auth
                authRepository.updateProfile(photoUri = downloadUrl)
            }.onFailure { exception ->
                // Jika gagal, kembalikan UI ke foto sebelumnya (atau berikan notifikasi error)
                _profilePhotoUri.value = user.photoUrl
                exception.printStackTrace()
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}