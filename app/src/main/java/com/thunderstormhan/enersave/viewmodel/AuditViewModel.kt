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
    // null currentRoomId = main room view
    private val _isMainRoom = MutableStateFlow(false)
    val isMainRoom: StateFlow<Boolean> = _isMainRoom.asStateFlow()

    // Which rooms are placed on the main canvas
    private val _placedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val placedRoomIds: StateFlow<Set<String>> = _placedRoomIds.asStateFlow()

    // Positions of rooms on the main canvas (roomId -> x,y in px)
    private val _mainRoomPositions = MutableStateFlow<Map<String, Pair<Float,Float>>>(emptyMap())
    val mainRoomPositions: StateFlow<Map<String, Pair<Float,Float>>> = _mainRoomPositions.asStateFlow()

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

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // True while initial Firestore load is in progress
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
        // saved on explicit Save
    }

    fun switchToMainRoom() {
        _isMainRoom.value = true
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
        // saved on explicit Save
    }

    fun updateMainRoomPosition(roomId: String, x: Float, y: Float) {
        _mainRoomPositions.value = _mainRoomPositions.value + (roomId to Pair(x, y))
        // saved on explicit Save
    }

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

    // ── Appliance management (scoped to current room) ─────────────────────────

    fun addApplianceToCanvas(appliance: Appliance) {
        val roomId = _currentRoomId.value ?: return
        val new = appliance.copy(id = java.util.UUID.randomUUID().toString())
        updateRoomAppliances(roomId) { it + new }
        // saved on explicit Save
    }

    fun removeApplianceFromCanvas(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list -> list.filter { it.id != id } }
        refreshTotalCost()
        // positions saved on explicit Save button tap
    }

    fun updateAppliancePosition(id: String, dragAmountX: Float, dragAmountY: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(
                positionX = it.positionX + dragAmountX,
                positionY = it.positionY + dragAmountY
            ) else it }
        }
        // saved on explicit Save
    }

    fun updateApplianceUsage(id: String, newHours: Float) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(hourUsage = newHours) else it }
        }
        refreshTotalCost()
        // saved on explicit Save
    }

    fun rotateAppliance(id: String) {
        val roomId = _currentRoomId.value ?: return
        updateRoomAppliances(roomId) { list ->
            list.map { if (it.id == id) it.copy(rotationY = (it.rotationY + 45f) % 360f) else it }
        }
        // saved on explicit Save
    }

    // Save everything at once when user taps Save
    fun saveAll() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            _saveSuccess.value = false
            try {
                // Save all rooms
                _rooms.value.forEach { room ->
                    val data = mapOf(
                        "id"           to room.id,
                        "name"         to room.name,
                        "widthMeters"  to room.widthMeters,
                        "heightMeters" to room.heightMeters,
                        "appliances"   to room.appliances.map { a ->
                            mapOf(
                                "id"           to a.id,
                                "name"         to a.name,
                                "watt"         to a.watt,
                                "hourUsage"    to a.hourUsage,
                                "iconName"     to a.iconName,
                                "isSwitchedOn" to a.isSwitchedOn,
                                "positionX"    to a.positionX,
                                "positionY"    to a.positionY,
                                "modelPath"    to a.modelPath,
                                "rotationY"    to a.rotationY
                            )
                        }
                    )
                    db.collection("users").document(userId)
                        .collection("rooms").document(room.id)
                        .set(data).await()
                }
                // Save main canvas state
                val canvasData = mapOf(
                    "placedRoomIds"     to _placedRoomIds.value.toList(),
                    "mainRoomPositions" to _mainRoomPositions.value.map { (k, v) ->
                        mapOf("id" to k, "x" to v.first, "y" to v.second)
                    }
                )
                db.collection("users").document(userId)
                    .collection("canvas").document("main")
                    .set(canvasData).await()

                _saveSuccess.value = true
                // Reset success indicator after 2 seconds
                kotlinx.coroutines.delay(2000)
                _saveSuccess.value = false
            } catch (e: Exception) {
                // Save failed — button resets to normal
            } finally {
                _isSaving.value = false
            }
        }
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
                // Explicitly convert to map so Firestore stores appliances correctly
                val data = mapOf(
                    "id"           to room.id,
                    "name"         to room.name,
                    "widthMeters"  to room.widthMeters,
                    "heightMeters" to room.heightMeters,
                    "appliances"   to room.appliances.map { a ->
                        mapOf(
                            "id"           to a.id,
                            "name"         to a.name,
                            "watt"         to a.watt,
                            "hourUsage"    to a.hourUsage,
                            "iconName"     to a.iconName,
                            "isSwitchedOn" to a.isSwitchedOn,
                            "positionX"    to a.positionX,
                            "positionY"    to a.positionY,
                            "modelPath"    to a.modelPath,
                            "rotationY"    to a.rotationY
                        )
                    }
                )
                db.collection("users").document(userId)
                    .collection("rooms").document(room.id)
                    .set(data).await()
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

    // Save main canvas state (placed rooms + positions) as a single document
    private fun saveMainCanvasToFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "placedRoomIds"    to _placedRoomIds.value.toList(),
                    "mainRoomPositions" to _mainRoomPositions.value.map { (k, v) ->
                        mapOf("id" to k, "x" to v.first, "y" to v.second)
                    }
                )
                db.collection("users").document(userId)
                    .collection("canvas").document("main")
                    .set(data).await()
            } catch (e: Exception) { }
        }
    }

    private fun loadRoomsFromFirestore() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Load rooms — fetch raw maps to avoid Firestore deserialization issues
                // with nested Appliance list
                val roomSnapshot = db.collection("users").document(userId)
                    .collection("rooms").get().await()

                val loaded = roomSnapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Room(
                            id           = doc.id,
                            name         = data["name"] as? String ?: "",
                            widthMeters  = (data["widthMeters"] as? Number)?.toInt() ?: 3,
                            heightMeters = (data["heightMeters"] as? Number)?.toInt() ?: 3,
                            appliances   = parseAppliances(data["appliances"])
                        )
                    } catch (e: Exception) { null }
                }
                _rooms.value = loaded
                _currentRoomId.value = loaded.firstOrNull()?.id
                syncCurrentRoomAppliances()
                refreshTotalCost()

                // Load main canvas state
                val canvasSnapshot = db.collection("users").document(userId)
                    .collection("canvas").document("main").get().await()
                if (canvasSnapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val placedList = canvasSnapshot.get("placedRoomIds") as? List<String> ?: emptyList()
                    _placedRoomIds.value = placedList.toSet()

                    @Suppress("UNCHECKED_CAST")
                    val posList = canvasSnapshot.get("mainRoomPositions") as? List<Map<String, Any>> ?: emptyList()
                    _mainRoomPositions.value = posList.associate { entry ->
                        val id = entry["id"] as? String ?: ""
                        val x  = (entry["x"] as? Number)?.toFloat() ?: 0f
                        val y  = (entry["y"] as? Number)?.toFloat() ?: 0f
                        id to Pair(x, y)
                    }.filterKeys { it.isNotEmpty() }
                }
            } catch (e: Exception) {
                _rooms.value = emptyList()
            } finally {
                // Always mark loading done, even if Firestore failed
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
                    id          = map["id"] as? String ?: "",
                    name        = map["name"] as? String ?: "",
                    watt        = (map["watt"] as? Number)?.toInt() ?: 0,
                    hourUsage   = (map["hourUsage"] as? Number)?.toFloat() ?: 0f,
                    iconName    = map["iconName"] as? String ?: "",
                    isSwitchedOn = map["isSwitchedOn"] as? Boolean ?: true,
                    positionX   = (map["positionX"] as? Number)?.toFloat() ?: 0f,
                    positionY   = (map["positionY"] as? Number)?.toFloat() ?: 0f,
                    modelPath   = map["modelPath"] as? String ?: "",
                    rotationY   = (map["rotationY"] as? Number)?.toFloat() ?: 0f
                )
            } catch (e: Exception) { null }
        }
    }
}