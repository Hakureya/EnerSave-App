package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thunderstormhan.enersave.data.model.Appliance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuditViewModel : ViewModel() {

    // Daftar alat yang ada di ruangan saat ini
    private val _activeAppliances = MutableStateFlow<List<Appliance>>(emptyList())
    val activeAppliances: StateFlow<List<Appliance>> = _activeAppliances.asStateFlow()

    // Fungsi untuk menghitung total biaya harian (Rp)
    // Rumus: (Watt * Jam / 1000) * Tarif_per_kWh
    fun calculateTotalDailyCost(): Long {
        val tarifPerKwh = 1500 // Sesuai standar tarif rata-rata
        return _activeAppliances.value.sumOf { appliance ->
            ((appliance.watt * appliance.hourUsage) / 1000 * tarifPerKwh).toLong()
        }
    }

    // Fungsi untuk menghitung total biaya bulanan (30 hari)
    fun calculateTotalMonthlyCost(): Long {
        return calculateTotalDailyCost() * 30
    }

    // Fungsi untuk memperbarui durasi pemakaian alat tertentu
    fun updateApplianceUsage(applianceId: String, newHours: Float) {
        val currentList = _activeAppliances.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == applianceId }

        if (index != -1) {
            currentList[index] = currentList[index].copy(hourUsage = newHours)
            _activeAppliances.value = currentList
            // Di sini kamu bisa menambahkan fungsi untuk update ke Firestore
        }
    }

    // Fungsi awal untuk memuat data (misal data dummy atau dari Firestore)
    fun loadInitialData() {
        _activeAppliances.value = listOf(
            Appliance("1", "AC 1 PK", 800, 8f, "wind"),
            Appliance("2", "Smart TV", 150, 4f, "tv")
        )
    }
}