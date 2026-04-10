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

    private val _activeAppliances = MutableStateFlow<List<Appliance>>(emptyList())
    val activeAppliances: StateFlow<List<Appliance>> = _activeAppliances.asStateFlow()

    private val _totalDailyCost = MutableStateFlow(0L)
    val totalDailyCost: StateFlow<Long> = _totalDailyCost.asStateFlow()

    private val _availableCollection = MutableStateFlow(listOf(
        Appliance("id_ac",            "AC 1 PK",        800,  0f, "AC/air_conditioner",      true, 100f, 100f),
        Appliance("id_kipas",         "Kipas Angin",     45,  0f, "fan/fan",                 true, 150f, 150f),
        Appliance("id_pc",            "PC Gaming",       450, 0f, "computer/computer",       true, 200f, 200f),
        Appliance("id_lampu",         "Lampu Meja",      15,  0f, "light/light_bulb",        true, 250f, 250f),
        Appliance("id_kulkas",        "Kulkas",          120, 0f, "fridge/fridge",           true, 300f, 300f),
        Appliance("id_blender",       "Blender",         300, 0f, "blender/blender",         true, 350f, 100f),
        Appliance("id_ceiling_fan",   "Kipas Plafon",    50,  0f, "ceiling_fan/ceiling_fan", true, 400f, 150f),
        Appliance("id_hair_dryer",    "Hair Dryer",      1200,0f, "hair_dryer/hair_dryer",   true, 450f, 200f),
        Appliance("id_iron",          "Setrika",         1000,0f, "iron/iron",               true, 500f, 250f),
        Appliance("id_printer",       "Printer",         50,  0f, "printer/printer",         true, 550f, 300f)
    ))
    val availableCollection = _availableCollection.asStateFlow()

    init { loadAppliancesFromFirestore() }

    fun addApplianceToCanvas(appliance: Appliance) {
        val new = appliance.copy(id = java.util.UUID.randomUUID().toString())
        _activeAppliances.value = _activeAppliances.value + new
        saveToFirestore(new)
    }

    fun removeApplianceFromCanvas(id: String) {
        _activeAppliances.value = _activeAppliances.value.filter { it.id != id }
        refreshTotalCost()
        deleteFromFirestore(id)
    }

    fun updateAppliancePosition(id: String, dragAmountX: Float, dragAmountY: Float) {
        _activeAppliances.value = _activeAppliances.value.map {
            if (it.id == id) it.copy(
                positionX = it.positionX + dragAmountX,
                positionY = it.positionY + dragAmountY
            ) else it
        }.also { list ->
            list.find { it.id == id }?.let { saveToFirestore(it) }
        }
    }

    fun updateApplianceUsage(id: String, newHours: Float) {
        _activeAppliances.value = _activeAppliances.value.map {
            if (it.id == id) it.copy(hourUsage = newHours) else it
        }
        refreshTotalCost()
        _activeAppliances.value.find { it.id == id }?.let { saveToFirestore(it) }
    }

    // Rotate by 45° each call, wraps at 360°
    fun rotateAppliance(id: String) {
        _activeAppliances.value = _activeAppliances.value.map {
            if (it.id == id) it.copy(rotationY = (it.rotationY + 45f) % 360f) else it
        }
        _activeAppliances.value.find { it.id == id }?.let { saveToFirestore(it) }
    }

    fun calculateTotalDailyCost(): Long {
        val tarifPerKwh = 1500
        return _activeAppliances.value.sumOf {
            ((it.watt * it.hourUsage) / 1000 * tarifPerKwh).toLong()
        }
    }

    fun refreshTotalCost() {
        val tarifPerKwh = 1500
        _totalDailyCost.value = _activeAppliances.value.sumOf {
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
            } catch (e: Exception) { }
        }
    }

    private fun deleteFromFirestore(id: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("user_appliances").document(id)
                    .delete().await()
            } catch (e: Exception) { }
        }
    }

    private fun loadAppliancesFromFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("user_appliances").get().await()
                _activeAppliances.value = snapshot.toObjects(Appliance::class.java)
                refreshTotalCost()
            } catch (e: Exception) {
                _activeAppliances.value = emptyList()
            }
        }
    }
}