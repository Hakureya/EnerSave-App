package com.thunderstormhan.enersave.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Mendapatkan user yang sedang login saat ini
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // Fungsi Login (Email & Password)
    suspend fun login(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fungsi Register
    suspend fun register(email: String, pass: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fungsi Logout
    fun logout() {
        auth.signOut()
    }

    // Mengecek apakah user sudah terautentikasi
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // FUNGSI BARU: Memperbarui metadata profil (Nama & Foto)
    suspend fun updateProfile(displayName: String? = null, photoUri: Uri? = null): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User tidak terautentikasi"))

        return try {
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUri?.let { setPhotoUri(it) }
            }.build()

            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}