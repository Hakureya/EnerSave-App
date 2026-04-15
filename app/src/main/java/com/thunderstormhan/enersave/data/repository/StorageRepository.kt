package com.thunderstormhan.enersave.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    // Fungsi untuk mengunggah gambar profil dan mengembalikan URL publiknya
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<Uri> {
        return try {
            // Membuat path di Firebase Storage: profile_images/{userId}.jpg
            val storageRef = storage.reference.child("profile_images/${userId}.jpg")

            // Mengunggah file
            storageRef.putFile(imageUri).await()

            // Mengambil URL yang bisa diakses publik
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}