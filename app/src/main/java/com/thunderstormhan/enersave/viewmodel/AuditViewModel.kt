package com.thunderstormhan.enersave.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.thunderstormhan.enersave.data.model.Appliance
import com.thunderstormhan.enersave.data.model.Room
import com.thunderstormhan.enersave.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuditViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // ── State Flows ──────────────────────────────────────────────────────────
    private val _isMainRoom = MutableStateFlow(false)
    val isMainRoom: StateFlow<Boolean> = _isMainRoom.asStateFlow()

    private val _placedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val placedRoomIds: StateFlow<Set<String>> = _placedRoomIds.asStateFlow()

    private val _mainRoomPositions = MutableStateFlow<Map<String, Pair<Float, Float>>>(emptyMap())
    val mainRoomPositions: StateFlow<Map<String, Pair<Float, Float>>> = _mainRoomPositions.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _activeAppliancesFlow = MutableStateFlow<List<Appliance>>(emptyList())
    val activeAppliancesFlow: StateFlow<List<Appliance>> = _activeAppliancesFlow.asStateFlow()

    private val _totalDailyCost = MutableStateFlow(0L)
    val totalDailyCost: StateFlow<Long> = _totalDailyCost.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Master Collection ─────────────────────────────────────────────────────
    // Di AuditViewModel.kt (Baris 55-65)
    // Di AuditViewModel.kt
    private val _availableCollection = MutableStateFlow(listOf(
        Appliance("id_ac", "AC 1 PK", 800, 0f, "ac/air_conditioner", true, 100f, 100f, "", 0f, "default", type = "ac"),
        Appliance("id_kipas", "Kipas Angin", 45, 0f, "fan/fan", true, 150f, 150f, "", 0f, "default", type = "fan"),
        Appliance("id_pc", "PC Gaming", 450, 0f, "computer/computer", true, 200f, 200f, "", 0f, "default", type = "computer"),
        Appliance("id_lampu", "Lampu Meja", 15, 0f, "light/light_bulb", true, 250f, 250f, "", 0f, "default", type = "light"),
        Appliance("id_kulkas", "Kulkas", 120, 0f, "fridge/fridge", true, 300f, 300f, "", 0f, "default", type = "fridge"),
        Appliance("id_blender", "Blender", 300, 0f, "blender/blender", true, 350f, 100f, "", 0f, "default", type = "blender"),
        Appliance("id_ceiling_fan", "Kipas Plafon", 50, 0f, "ceiling_fan/ceiling_fan", true, 400f, 150f, "", 0f, "default", type = "ceiling_fan"),
        Appliance("id_hair_dryer", "Hair Dryer", 1200, 0f, "hair_dryer/hair_dryer", true, 450f, 200f, "", 0f, "default", type = "hair_dryer"),
        Appliance("id_iron", "Setrika", 1000, 0f, "iron/iron", true, 500f, 250f, "", 0f, "default", type = "iron"),
        Appliance("id_printer", "Printer", 50, 0f, "printer/printer", true, 550f, 300f, "", 0f, "default", type = "printer")
    ))
    val availableCollection = _availableCollection.asStateFlow()

    private val currentRoom: Room?
        get() = _rooms.value.find { it.id == _currentRoomId.value }

    init {
        loadRoomsFromFirestore()
    }

    // ── Room Logic ────────────────────────────────────────────────────────────

    fun addRoom(name: String, widthMeters: Int, heightMeters: Int) {
        val room = Room(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            widthMeters = widthMeters,
            heightMeters = heightMeters,
            appliances = emptyList()
        )
        _rooms.value = _rooms.value + room
        _currentRoomId.value = room.id
        syncCurrentRoomAppliances()
    }

    fun switchToMainRoom() { _isMainRoom.value = true }

    fun switchRoom(roomId: String) {
        _isMainRoom.value = false
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

    fun toggleRoomOnMainCanvas(roomId: String) {
        val placed = _placedRoomIds.value.toMutableSet()
        if (placed.contains(roomId)) {
            placed.remove(roomId)
            _mainRoomPositions.value = _mainRoomPositions.value - roomId
        } else {
            placed.add(roomId)
        }
        _placedRoomIds.value = placed
    }

    fun updateMainRoomPosition(roomId: String, x: Float, y: Float) {
        _mainRoomPositions.value = _mainRoomPositions.value + (roomId to Pair(x, y))
    }

    // ── Appliance Logic ───────────────────────────────────────────────────────

    fun addApplianceToCanvas(appliance: Appliance) {
        val roomId = _currentRoomId.value ?: return
        val new = appliance.copy(id = java.util.UUID.randomUUID().toString())
        updateRoomAppliances(roomId) { it + new }
    }

    fun removeApplianceFromCanvas(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list -> list.filter { it.id != id } }
        refreshTotalCost()
    }

    fun updateAppliancePosition(id: String, dragAmountX: Float, dragAmountY: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(
                positionX = it.positionX + dragAmountX,
                positionY = it.positionY + dragAmountY
            ) else it }
        }
    }

    fun updateApplianceUsage(id: String, newHours: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(hourUsage = newHours) else it }
        }
        refreshTotalCost()
    }

    fun rotateAppliance(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(rotationY = (it.rotationY + 45f) % 360f) else it }
        }
    }

    fun updateApplianceSkin(applianceId: String, newModel: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == applianceId) it.copy(activeModel = newModel) else it }
        }
    }

    // ── Persistence Logic ─────────────────────────────────────────────────────

    fun saveAll() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Simpan Semua Ruangan
                _rooms.value.forEach { room ->
                    val data = mapOf(
                        "id" to room.id,
                        "name" to room.name,
                        "widthMeters" to room.widthMeters,
                        "heightMeters" to room.heightMeters,
                        "appliances" to room.appliances.map { a ->
                            mapOf(
                                "id" to a.id,
                                "name" to a.name,
                                "watt" to a.watt,
                                "hourUsage" to a.hourUsage,
                                "iconName" to a.iconName,
                                "isSwitchedOn" to a.isSwitchedOn,
                                "positionX" to a.positionX,
                                "positionY" to a.positionY,
                                "rotationY" to a.rotationY,
                                "activeModel" to a.activeModel // PENTING: Menyimpan skin
                            )
                        }
                    )
                    db.collection("users").document(userId)
                        .collection("rooms").document(room.id)
                        .set(data).await()
                }

                // Simpan State Canvas Utama
                val canvasData = mapOf(
                    "placedRoomIds" to _placedRoomIds.value.toList(),
                    "mainRoomPositions" to _mainRoomPositions.value.map { (k, v) ->
                        mapOf("id" to k, "x" to v.first, "y" to v.second)
                    }
                )
                db.collection("users").document(userId)
                    .collection("canvas").document("main")
                    .set(canvasData).await()

                _saveSuccess.value = true
                delay(2000)
                _saveSuccess.value = false
            } catch (e: Exception) {
                // Log error here
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun loadRoomsFromFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val roomSnapshot = db.collection("users").document(userId)
                    .collection("rooms").get().await()

                val loaded = roomSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Room(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        widthMeters = (data["widthMeters"] as? Number)?.toInt() ?: 3,
                        heightMeters = (data["heightMeters"] as? Number)?.toInt() ?: 3,
                        appliances = parseAppliances(data["appliances"])
                    )
                }
                _rooms.value = loaded
                _currentRoomId.value = loaded.firstOrNull()?.id
                syncCurrentRoomAppliances()
                refreshTotalCost()

                // Load canvas
                val canvasSnapshot = db.collection("users").document(userId)
                    .collection("canvas").document("main").get().await()
                if (canvasSnapshot.exists()) {
                    val placedList = canvasSnapshot.get("placedRoomIds") as? List<String> ?: emptyList()
                    _placedRoomIds.value = placedList.toSet()

                    val posList = canvasSnapshot.get("mainRoomPositions") as? List<Map<String, Any>> ?: emptyList()
                    _mainRoomPositions.value = posList.associate { entry ->
                        val id = entry["id"] as? String ?: ""
                        val x = (entry["x"] as? Number)?.toFloat() ?: 0f
                        val y = (entry["y"] as? Number)?.toFloat() ?: 0f
                        id to Pair(x, y)
                    }.filterKeys { it.isNotEmpty() }
                }
            } catch (e: Exception) {
                _rooms.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAppliances(raw: Any?): List<Appliance> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { map ->
            try {
                Appliance(
                    id = map["id"] as? String ?: "",
                    name = map["name"] as? String ?: "",
                    watt = (map["watt"] as? Number)?.toInt() ?: 0,
                    hourUsage = (map["hourUsage"] as? Number)?.toFloat() ?: 0f,
                    iconName = map["iconName"] as? String ?: "",
                    isSwitchedOn = map["isSwitchedOn"] as? Boolean ?: true,
                    positionX = (map["positionX"] as? Number)?.toFloat() ?: 0f,
                    positionY = (map["positionY"] as? Number)?.toFloat() ?: 0f,
                    rotationY = (map["rotationY"] as? Number)?.toFloat() ?: 0f,
                    activeModel = map["activeModel"] as? String ?: "default", // PENTING: Membaca skin
                    type         = map["type"] as? String ?: "other"
                )
            } catch (e: Exception) { null }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    fun refreshTotalCost() {
        val tarifPerKwh = 1500
        _totalDailyCost.value = _activeAppliancesFlow.value.sumOf {
            ((it.watt * it.hourUsage) / 1000 * tarifPerKwh).toLong()
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
}