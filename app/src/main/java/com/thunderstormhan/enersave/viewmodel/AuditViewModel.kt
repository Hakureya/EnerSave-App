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
        // iconName now stores "folder/filename.glb" to match your assets structure exactly
        Appliance("id_ac",           "AC 1 PK",       800, 0f, "AC/air_conditioner",     true, 100f, 100f),
        Appliance("id_kipas",        "Kipas Angin",    45,  0f, "fan/fan",                true, 150f, 150f),
        Appliance("id_pc",           "PC Gaming",      450, 0f, "computer/computer",      true, 200f, 200f),
        Appliance("id_lampu",        "Lampu Meja",     15,  0f, "light/light_bulb",       true, 250f, 250f),
        Appliance("id_kulkas",       "Kulkas",         120, 0f, "fridge/fridge",          true, 300f, 300f),
        Appliance("id_blender",      "Blender",        300, 0f, "blender/blender",        true, 350f, 100f),
        Appliance("id_ceiling_fan",  "Kipas Plafon",   50,  0f, "ceiling_fan/ceiling_fan",true, 400f, 150f),
        Appliance("id_hair_dryer",   "Hair Dryer",     1200,0f, "hair_dryer/hair_dryer",  true, 450f, 200f),
        Appliance("id_iron",         "Setrika",        1000,0f, "iron/iron",              true, 500f, 250f),
        Appliance("id_printer",      "Printer",        50,  0f, "printer/printer",        true, 550f, 300f)
    ))
    val availableCollection = _availableCollection.asStateFlow()

    init {
        loadAppliancesFromFirestore()
    }

    fun addApplianceToCanvas(appliance: Appliance) {
        val newAppliance = appliance.copy(id = java.util.UUID.randomUUID().toString())
        _activeAppliances.value = _activeAppliances.value + newAppliance
        saveToFirestore(newAppliance)
    }

    fun removeApplianceFromCanvas(id: String) {
        _activeAppliances.value = _activeAppliances.value.filter { it.id != id }
        refreshTotalCost()
        deleteFromFirestore(id)
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

    fun refreshTotalCost() {
        val tarifPerKwh = 1500
        _totalDailyCost.value = _activeAppliances.value.sumOf { appliance ->
            ((appliance.watt * appliance.hourUsage) / 1000 * tarifPerKwh).toLong()
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
                val list = snapshot.toObjects(Appliance::class.java)
                _activeAppliances.value = list
                refreshTotalCost()
            } catch (e: Exception) {
                _activeAppliances.value = emptyList()
            }
        }
    }
}