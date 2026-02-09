package com.thunderstormhan.enersave.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
}