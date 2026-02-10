package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuditViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Daftar alat aktif di canvas
    private val _activeAppliances = MutableStateFlow<List<Appliance>>(emptyList())
    val activeAppliances: StateFlow<List<Appliance>> = _activeAppliances.asStateFlow()

    private val _totalDailyCost = MutableStateFlow(0L)
    val totalDailyCost: StateFlow<Long> = _totalDailyCost.asStateFlow()
    // Koleksi alat yang bisa dipilih
    private val _availableCollection = MutableStateFlow(listOf(
        Appliance("id_ac", "AC 1 PK", 800, 0f, "wind", true, 100f, 100f),
        Appliance("id_kipas", "Kipas Angin", 45, 0f, "fan", true, 150f, 150f),
        Appliance("id_pc", "PC Gaming", 450, 0f, "computer", true, 200f, 200f),
        Appliance("id_lampu", "Lampu Meja", 15, 0f, "lightbulb", true, 250f, 250f),
        Appliance("id_kulkas", "Kulkas", 120, 0f, "snowflake", true, 300f, 300f)
    ))
    val availableCollection = _availableCollection.asStateFlow()

    init {
        loadAppliancesFromFirestore()
    }

    fun addApplianceToCanvas(appliance: Appliance) {
        val newAppliance = appliance.copy(id = java.util.UUID.randomUUID().toString())
        val currentList = _activeAppliances.value.toMutableList()
        currentList.add(newAppliance)
        _activeAppliances.value = currentList
        saveToFirestore(newAppliance)
    }

    fun updateAppliancePosition(id: String, dragAmountX: Float, dragAmountY: Float) {
        val currentList = _activeAppliances.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = currentList[index].copy(
                positionX = currentList[index].positionX + dragAmountX,
                positionY = currentList[index].positionY + dragAmountY
            )
            currentList[index] = updated
            _activeAppliances.value = currentList
            saveToFirestore(updated)
        }
    }

    fun updateApplianceUsage(id: String, newHours: Float) {
        val currentList = _activeAppliances.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = currentList[index].copy(hourUsage = newHours)
            currentList[index] = updated
            _activeAppliances.value = currentList

            // PENTING: Hitung ulang total biaya setelah data berubah
            refreshTotalCost()
            saveToFirestore(updated)
        }
    }

    fun calculateTotalDailyCost(): Long {
        val tarifPerKwh = 1500
        return _activeAppliances.value.sumOf {
            ((it.watt * it.hourUsage) / 1000 * tarifPerKwh).toLong()
        }
    }

    private fun saveToFirestore(appliance: Appliance) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("user_appliances").document(appliance.id)
                    .set(appliance).await()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    // Di dalam AuditViewModel.kt
    private fun loadAppliancesFromFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("user_appliances").get().await()
                val list = snapshot.toObjects(Appliance::class.java)
                _activeAppliances.value = list

                // PENTING: Hitung ulang biaya setelah data dari Firebase masuk
                refreshTotalCost()
            } catch (e: Exception) {
                _activeAppliances.value = emptyList()
            }
        }
    }

    // Buat fungsi untuk memperbarui total biaya
    fun refreshTotalCost() {
        val tarifPerKwh = 1500
        _totalDailyCost.value = _activeAppliances.value.sumOf { appliance ->
            // Rumus: (Watt * Jam / 1000) * Tarif
            ((appliance.watt * appliance.hourUsage) / 1000 * tarifPerKwh).toLong()
        }
    }

    // Update fungsi updateApplianceUsage agar memicu perhitungan ulang

}