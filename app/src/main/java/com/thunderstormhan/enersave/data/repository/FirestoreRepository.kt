package com.thunderstormhan.enersave.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.thunderstormhan.enersave.data.model.User
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Fungsi untuk mengambil data profil user
    suspend fun getUserProfile(uid: String): User? {
        return try {
            db.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}