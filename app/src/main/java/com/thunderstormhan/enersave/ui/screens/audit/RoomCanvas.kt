package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thunderstormhan.enersave.data.model.Appliance
import io.github.sceneview.Scene
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.roundToInt

// Room grid configuration
private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 3

// Colors for the room
private val FloorTileColor       = Color(0xFFE8DCC8) // warm beige floor
private val FloorTileAltColor    = Color(0xFFDDD0B5) // slightly darker alternate tile
private val WallColor            = Color(0xFFC8B99A) // warm wall tone
private val WallTopColor         = Color(0xFFB5A688) // darker top of wall (depth)
private val GridLineColor        = Color(0xFFBBA98A) // subtle grid lines
private val TileHighlightColor   = Color(0xFFFFD580).copy(alpha = 0.35f) // highlight on hover
private val TilePlatformColor    = Color(0xFFD4C4A0) // platform under placed model
private val SelectedBorderColor  = Color(0xFF4F8EF7)

@Composable
fun RoomCanvas(
    activeAppliances: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit,
    onApplianceDrag: (String, Float, Float) -> Unit,
    onDelete: (String) -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var trashBounds by remember { mutableStateOf(Rect.Zero) }

    // Track which appliance is being dragged (not yet snapped)
    var draggingId by remember { mutableStateOf<String?>(null) }

    // Canvas size measured at runtime
    var canvasWidthPx by remember { mutableStateOf(0f) }
    var canvasHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                canvasWidthPx = coords.size.width.toFloat()
                canvasHeightPx = coords.size.height.toFloat()
            }
    ) {
        // ── LAYER 1: ROOM BACKGROUND (walls + floor grid) ──────────────────
        RoomBackground(
            columns = GRID_COLUMNS,
            rows = GRID_ROWS,
            canvasWidth = canvasWidthPx,
            canvasHeight = canvasHeightPx
        )

        // ── LAYER 2: PLACED APPLIANCES ──────────────────────────────────────
        if (canvasWidthPx > 0f && canvasHeightPx > 0f) {
            val tileW = canvasWidthPx / GRID_COLUMNS
            val tileH = canvasHeightPx / GRID_ROWS

            activeAppliances.forEach { appliance ->
                key(appliance.id) {
                    // Snap position: center of whichever tile the appliance is on
                    val snappedX = (appliance.positionX / tileW).toInt()
                        .coerceIn(0, GRID_COLUMNS - 1) * tileW + tileW / 2f - 50.dp.value
                    val snappedY = (appliance.positionY / tileH).toInt()
                        .coerceIn(0, GRID_ROWS - 1) * tileH + tileH / 2f - 60.dp.value

                    val isDragging = draggingId == appliance.id

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (if (isDragging) appliance.positionX else snappedX).roundToInt(),
                                    (if (isDragging) appliance.positionY else snappedY).roundToInt()
                                )
                            }
                            .pointerInput(appliance.id) {
                                detectDragGestures(
                                    onDragStart = { draggingId = appliance.id },
                                    onDragEnd = {
                                        draggingId = null
                                        // Check if dropped on trash
                                        val center = Offset(
                                            appliance.positionX,
                                            appliance.positionY
                                        )
                                        if (trashBounds.contains(center)) {
                                            onDelete(appliance.id)
                                        }
                                        // Snap: update position to nearest tile center
                                        val col = (appliance.positionX / tileW)
                                            .toInt().coerceIn(0, GRID_COLUMNS - 1)
                                        val row = (appliance.positionY / tileH)
                                            .toInt().coerceIn(0, GRID_ROWS - 1)
                                        val snappedCenterX = col * tileW + tileW / 2f
                                        val snappedCenterY = row * tileH + tileH / 2f
                                        onApplianceDrag(
                                            appliance.id,
                                            snappedCenterX - appliance.positionX,
                                            snappedCenterY - appliance.positionY
                                        )
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    onApplianceDrag(appliance.id, dragAmount.x, dragAmount.y)
                                }
                            }
                    ) {
                        ThreeDItem(
                            appliance = appliance,
                            engine = engine,
                            modelLoader = modelLoader,
                            tileWidth = tileW,
                            tileHeight = tileH,
                            isDragging = isDragging,
                            onClick = { onApplianceClick(appliance) }
                        )
                    }
                }
            }
        }

        // ── LAYER 3: TRASH BUTTON ───────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 200.dp, end = 24.dp)
                .size(56.dp)
                .onGloballyPositioned { coords ->
                    trashBounds = coords.boundsInWindow()
                },
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗑️", fontSize = 22.sp)
            }
        }
    }
}

// ── Room background: walls on top + left + right, floor grid ──────────────────
@Composable
fun RoomBackground(
    columns: Int,
    rows: Int,
    canvasWidth: Float,
    canvasHeight: Float
) {
    if (canvasWidth == 0f || canvasHeight == 0f) return

    val wallThickness = canvasHeight * 0.12f // top wall height
    val sideWallWidth = canvasWidth * 0.04f  // left/right wall strip

    Canvas(modifier = Modifier.fillMaxSize()) {
        val tileW = canvasWidth / columns
        val tileH = (canvasHeight - wallThickness) / rows

        // --- Floor tiles (checkerboard pattern) ---
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val tileColor = if ((row + col) % 2 == 0) FloorTileColor else FloorTileAltColor
                drawRect(
                    color = tileColor,
                    topLeft = Offset(
                        x = sideWallWidth + col * tileW,
                        y = wallThickness + row * tileH
                    ),
                    size = Size(tileW, tileH)
                )
            }
        }

        // --- Grid lines on floor ---
        // Vertical lines
        for (col in 0..columns) {
            drawLine(
                color = GridLineColor,
                start = Offset(sideWallWidth + col * tileW, wallThickness),
                end = Offset(sideWallWidth + col * tileW, canvasHeight),
                strokeWidth = 1.5f
            )
        }
        // Horizontal lines
        for (row in 0..rows) {
            drawLine(
                color = GridLineColor,
                start = Offset(sideWallWidth, wallThickness + row * tileH),
                end = Offset(canvasWidth - sideWallWidth, wallThickness + row * tileH),
                strokeWidth = 1.5f
            )
        }

        // --- Back wall (top) ---
        drawRect(
            color = WallColor,
            topLeft = Offset(sideWallWidth, 0f),
            size = Size(canvasWidth - sideWallWidth * 2, wallThickness)
        )
        // Wall top edge (darker strip for depth)
        drawRect(
            color = WallTopColor,
            topLeft = Offset(sideWallWidth, 0f),
            size = Size(canvasWidth - sideWallWidth * 2, wallThickness * 0.08f)
        )
        // Wall bottom edge (shadow line)
        drawLine(
            color = GridLineColor,
            start = Offset(sideWallWidth, wallThickness),
            end = Offset(canvasWidth - sideWallWidth, wallThickness),
            strokeWidth = 3f
        )

        // --- Left wall strip ---
        drawRect(
            color = WallColor.copy(alpha = 0.7f),
            topLeft = Offset(0f, 0f),
            size = Size(sideWallWidth, canvasHeight)
        )

        // --- Right wall strip ---
        drawRect(
            color = WallColor.copy(alpha = 0.5f),
            topLeft = Offset(canvasWidth - sideWallWidth, 0f),
            size = Size(sideWallWidth, canvasHeight)
        )

        // --- Wall corners (darker triangles for 3D feel) ---
        drawLine(
            color = WallTopColor,
            start = Offset(sideWallWidth, 0f),
            end = Offset(sideWallWidth, wallThickness),
            strokeWidth = 2f
        )
        drawLine(
            color = WallTopColor,
            start = Offset(canvasWidth - sideWallWidth, 0f),
            end = Offset(canvasWidth - sideWallWidth, wallThickness),
            strokeWidth = 2f
        )
    }
}

// ── Individual 3D item on a tile platform ─────────────────────────────────────
@Composable
fun ThreeDItem(
    appliance: Appliance,
    engine: com.google.android.filament.Engine,
    modelLoader: ModelLoader,
    tileWidth: Float,
    tileHeight: Float,
    isDragging: Boolean,
    onClick: () -> Unit
) {
    val glbPath = "models/${appliance.iconName}.glb"
    val modelInstance = remember(glbPath) {
        modelLoader.createModelInstance(assetFileLocation = glbPath)
    }

    val isActive = appliance.hourUsage > 0

    // Platform size matches one grid tile
    val platformWidthDp  = (tileWidth  / 3f).dp  // roughly 1/3 of tile
    val platformHeightDp = (tileHeight / 5f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(if (isDragging) 12.dp else 4.dp, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent)
                .then(
                    if (isDragging)
                        Modifier.border(2.dp, SelectedBorderColor, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .clickable { onClick() }
        ) {
            if (modelInstance == null) {
                // Fallback emoji
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TilePlatformColor.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getEmojiForIcon(appliance.iconName),
                        fontSize = 36.sp
                    )
                }
            } else {
                // Transparent SceneView — isOpaque = false removes the black background
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
                    isOpaque = false, // ← KEY: removes black background
                    childNodes = rememberNodes {
                        add(
                            ModelNode(
                                modelInstance = modelInstance,
                                autoAnimate = false,
                                scaleToUnits = 1.0f
                            )
                        )
                    }
                )
            }

            // Power dot
            Surface(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp),
                shape = CircleShape,
                color = if (isActive) Color(0xFF22C55E) else Color(0xFFEF4444),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {}
        }

        // Tile platform under the model
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(TilePlatformColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appliance.name,
                fontSize = 7.sp,
                color = Color(0xFF7A6A50),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

fun getEmojiForIcon(iconName: String): String {
    val folder = iconName.substringBefore("/").lowercase()
    return when (folder) {
        "ac"              -> "❄️"
        "fan",
        "ceiling_fan"     -> "🌀"
        "computer"        -> "💻"
        "light"           -> "💡"
        "fridge"          -> "🧊"
        "blender"         -> "🫙"
        "hair_dryer"      -> "💨"
        "iron"            -> "👕"
        "printer"         -> "🖨️"
        "rice_cooker"     -> "🍚"
        "security_camera" -> "📷"
        "tv"              -> "📺"
        "washing_machine" -> "🫧"
        else              -> "🔌"
    }
}