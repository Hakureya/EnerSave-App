package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.model.Room
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

    // ── Rooms ─────────────────────────────────────────────────────────────────
    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    // Current room's appliances derived from rooms list
    val activeAppliances: StateFlow<List<Appliance>>
        get() = MutableStateFlow(currentRoom?.appliances ?: emptyList())

    private val _activeAppliancesFlow = MutableStateFlow<List<Appliance>>(emptyList())
    val activeAppliancesFlow: StateFlow<List<Appliance>> = _activeAppliancesFlow.asStateFlow()

    private val _totalDailyCost = MutableStateFlow(0L)
    val totalDailyCost: StateFlow<Long> = _totalDailyCost.asStateFlow()

    // ── Available appliance collection (shared across rooms) ──────────────────
    private val _availableCollection = MutableStateFlow(listOf(
        Appliance("id_ac",           "AC 1 PK",       800,  0f, "AC/air_conditioner",      true, 100f, 100f),
        Appliance("id_kipas",        "Kipas Angin",    45,   0f, "fan/fan",                 true, 150f, 150f),
        Appliance("id_pc",           "PC Gaming",      450,  0f, "computer/computer",       true, 200f, 200f),
        Appliance("id_lampu",        "Lampu Meja",     15,   0f, "light/light_bulb",        true, 250f, 250f),
        Appliance("id_kulkas",       "Kulkas",         120,  0f, "fridge/fridge",           true, 300f, 300f),
        Appliance("id_blender",      "Blender",        300,  0f, "blender/blender",         true, 350f, 100f),
        Appliance("id_ceiling_fan",  "Kipas Plafon",   50,   0f, "ceiling_fan/ceiling_fan", true, 400f, 150f),
        Appliance("id_hair_dryer",   "Hair Dryer",     1200, 0f, "hair_dryer/hair_dryer",   true, 450f, 200f),
        Appliance("id_iron",         "Setrika",        1000, 0f, "iron/iron",               true, 500f, 250f),
        Appliance("id_printer",      "Printer",        50,   0f, "printer/printer",         true, 550f, 300f)
    ))
    val availableCollection = _availableCollection.asStateFlow()

    private val currentRoom: Room?
        get() = _rooms.value.find { it.id == _currentRoomId.value }

    init {
        loadRoomsFromFirestore()
    }

    // ── Room management ───────────────────────────────────────────────────────

    fun addRoom(name: String, widthMeters: Int, heightMeters: Int) {
        val room = Room(
            id           = java.util.UUID.randomUUID().toString(),
            name         = name,
            widthMeters  = widthMeters,
            heightMeters = heightMeters,
            appliances   = emptyList()
        )
        _rooms.value = _rooms.value + room
        _currentRoomId.value = room.id
        syncCurrentRoomAppliances()
        saveRoomToFirestore(room)
    }

    fun switchRoom(roomId: String) {
        _currentRoomId.value = roomId
        syncCurrentRoomAppliances()
        refreshTotalCost()
    }

    fun deleteRoom(roomId: String) {
        _rooms.value = _rooms.value.filter { it.id != roomId }
        if (_currentRoomId.value == roomId) {
            _currentRoomId.value = _rooms.value.firstOrNull()?.id
        }
        syncCurrentRoomAppliances()
        deleteRoomFromFirestore(roomId)
    }

    // ── Appliance management (scoped to current room) ─────────────────────────

    fun addApplianceToCanvas(appliance: Appliance) {
        val roomId = _currentRoomId.value ?: return
        val new = appliance.copy(id = java.util.UUID.randomUUID().toString())
        updateRoomAppliances(roomId) { it + new }
        saveRoomToFirestore(currentRoom ?: return)
    }

    fun removeApplianceFromCanvas(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list -> list.filter { it.id != id } }
        refreshTotalCost()
        saveRoomToFirestore(currentRoom ?: return)
    }

    fun updateAppliancePosition(id: String, dragAmountX: Float, dragAmountY: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(
                positionX = it.positionX + dragAmountX,
                positionY = it.positionY + dragAmountY
            ) else it }
        }
        saveRoomToFirestore(currentRoom ?: return)
    }

    fun updateApplianceUsage(id: String, newHours: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(hourUsage = newHours) else it }
        }
        refreshTotalCost()
        saveRoomToFirestore(currentRoom ?: return)
    }

    fun rotateAppliance(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(rotationY = (it.rotationY + 45f) % 360f) else it }
        }
        saveRoomToFirestore(currentRoom ?: return)
    }

    fun refreshTotalCost() {
        val tarifPerKwh = 1500
        _totalDailyCost.value = _activeAppliancesFlow.value.sumOf {
            ((it.watt * it.hourUsage) / 1000 * tarifPerKwh).toLong()
        }
    }

    fun calculateTotalDailyCost(): Long = _totalDailyCost.value

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun updateRoomAppliances(roomId: String, transform: (List<Appliance>) -> List<Appliance>) {
        _rooms.value = _rooms.value.map { room ->
            if (room.id == roomId) room.copy(appliances = transform(room.appliances))
            else room
        }
        syncCurrentRoomAppliances()
    }

    private fun syncCurrentRoomAppliances() {
        _activeAppliancesFlow.value = currentRoom?.appliances ?: emptyList()
    }

    private fun saveRoomToFirestore(room: Room) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("rooms").document(room.id)
                    .set(room).await()
            } catch (e: Exception) { }
        }
    }

    private fun deleteRoomFromFirestore(roomId: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("rooms").document(roomId)
                    .delete().await()
            } catch (e: Exception) { }
        }
    }

    private fun loadRoomsFromFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("rooms").get().await()
                val loaded = snapshot.toObjects(Room::class.java)
                _rooms.value = loaded
                _currentRoomId.value = loaded.firstOrNull()?.id
                syncCurrentRoomAppliances()
                refreshTotalCost()
            } catch (e: Exception) {
                _rooms.value = emptyList()
            }
        }
    }
}