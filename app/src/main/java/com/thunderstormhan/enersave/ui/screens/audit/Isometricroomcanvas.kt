package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.thunderstormhan.enersave.data.model.Appliance
import io.github.sceneview.Scene
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.*

// ── Isometric math ────────────────────────────────────────────────────────────
fun isoToScreen(col: Float, row: Float, tileW: Float, tileH: Float, originX: Float, originY: Float): Offset {
    return Offset(
        x = originX + (col - row) * tileW / 2f,
        y = originY + (col + row) * tileH / 2f
    )
}

fun screenToIso(screenX: Float, screenY: Float, tileW: Float, tileH: Float, originX: Float, originY: Float): Pair<Int, Int> {
    val dx  = screenX - originX
    val dy  = screenY - originY
    val col = (dx / (tileW / 2f) + dy / (tileH / 2f)) / 2f
    val row = (dy / (tileH / 2f) - dx / (tileW / 2f)) / 2f
    return Pair(col.roundToInt(), row.roundToInt())
}

// ── Colors ────────────────────────────────────────────────────────────────────
private val FloorLight  = Color(0xFFDFD8C8)
private val FloorDark   = Color(0xFFCFC8B8)
private val WallLeft    = Color(0xFFB8C4D0)
private val WallRight   = Color(0xFFD0DBE5)
private val WallTop     = Color(0xFFE8EFF5)
private val GridLine    = Color(0xFFBBB4A4)
private val InnerWall   = Color(0xFF9AAABB)
private val WallDrawing = Color(0xFF4F8EF7)

// ── Model scale config per appliance type ─────────────────────────────────────
data class ModelConfig(val scale: Float, val cameraY: Float)

fun getModelConfig(iconName: String): ModelConfig {
    return when (iconName.substringBefore("/").lowercase()) {
        "ac"              -> ModelConfig(0.6f, 0.3f)
        "fan","ceiling_fan" -> ModelConfig(0.8f, 0.5f)
        "computer"        -> ModelConfig(0.9f, 0.2f)
        "light"           -> ModelConfig(0.5f, 0.8f)
        "fridge"          -> ModelConfig(1.0f, 0.5f)
        "blender"         -> ModelConfig(0.6f, 0.3f)
        "hair_dryer"      -> ModelConfig(0.5f, 0.2f)
        "iron"            -> ModelConfig(0.5f, 0.1f)
        "printer"         -> ModelConfig(0.7f, 0.2f)
        "rice_cooker"     -> ModelConfig(0.6f, 0.3f)
        "security_camera" -> ModelConfig(0.5f, 0.4f)
        "tv"              -> ModelConfig(1.1f, 0.2f)
        "washing_machine" -> ModelConfig(1.0f, 0.4f)
        else              -> ModelConfig(0.8f, 0.3f)
    }
}

// ── Wall segment ──────────────────────────────────────────────────────────────
data class WallSegment(
    val startCol: Int, val startRow: Int,
    val endCol: Int,   val endRow: Int
)

// ── Main composable ───────────────────────────────────────────────────────────
@Composable
fun IsometricRoomCanvas(
    roomConfig: RoomConfig,
    activeAppliances: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit,  // opens ApplianceActionSheet in AuditScreen
    onApplianceDrag: (String, Float, Float) -> Unit,
    onDelete: (String) -> Unit
    // NOTE: no onRotate here — rotation is handled via ApplianceActionSheet
) {
    val engine      = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val density     = LocalDensity.current

    var canvasW      by remember { mutableStateOf(0f) }
    var canvasH      by remember { mutableStateOf(0f) }
    var trashBounds  by remember { mutableStateOf(Rect.Zero) }
    var walls        by remember { mutableStateOf(listOf<WallSegment>()) }
    var isDrawingWall by remember { mutableStateOf(false) }
    var wallStart    by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var wallEnd      by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var wallModeOn   by remember { mutableStateOf(false) }
    var draggingId   by remember { mutableStateOf<String?>(null) }

    val tileWDp = 80.dp
    val tileHDp = 40.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .onGloballyPositioned { coords ->
                canvasW = coords.size.width.toFloat()
                canvasH = coords.size.height.toFloat()
            }
    ) {
        val tileWPx = with(density) { tileWDp.toPx() }
        val tileHPx = with(density) { tileHDp.toPx() }
        val wallH   = tileHPx * 1.8f
        val roomScreenH = (roomConfig.widthMeters + roomConfig.heightMeters) * tileHPx / 2f + wallH
        val originX = canvasW / 2f
        val originY = (canvasH - roomScreenH) / 2f + wallH

        // ── CANVAS: floor + walls ─────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (wallModeOn) Modifier.pointerInput(roomConfig, tileWPx, tileHPx, originX, originY) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val (c, r) = screenToIso(offset.x, offset.y, tileWPx, tileHPx, originX, originY)
                                wallStart = Pair(c.coerceIn(0, roomConfig.widthMeters), r.coerceIn(0, roomConfig.heightMeters))
                                wallEnd   = wallStart
                                isDrawingWall = true
                            },
                            onDragEnd = {
                                val s = wallStart; val e = wallEnd
                                if (s != null && e != null && s != e)
                                    walls = walls + WallSegment(s.first, s.second, e.first, e.second)
                                isDrawingWall = false; wallStart = null; wallEnd = null
                            }
                        ) { change, _ ->
                            change.consume()
                            val (c, r) = screenToIso(change.position.x, change.position.y, tileWPx, tileHPx, originX, originY)
                            wallEnd = Pair(c.coerceIn(0, roomConfig.widthMeters), r.coerceIn(0, roomConfig.heightMeters))
                        }
                    } else Modifier
                )
        ) {
            if (canvasW == 0f) return@Canvas
            drawIsometricRoom(roomConfig.widthMeters, roomConfig.heightMeters, tileWPx, tileHPx, originX, originY)
            walls.forEach { drawIsoWall(it, tileWPx, tileHPx, originX, originY) }
            if (isDrawingWall && wallStart != null && wallEnd != null) {
                val s  = wallStart!!; val e = wallEnd!!
                val sp = isoToScreen(s.first.toFloat(), s.second.toFloat(), tileWPx, tileHPx, originX, originY)
                val ep = isoToScreen(e.first.toFloat(), e.second.toFloat(), tileWPx, tileHPx, originX, originY)
                drawLine(WallDrawing, sp, ep, strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                drawCircle(WallDrawing, 6f, sp)
                drawCircle(WallDrawing, 6f, ep)
            }
        }

        // ── APPLIANCES ────────────────────────────────────────────────────
        if (canvasW > 0f) {
            activeAppliances.forEach { appliance ->
                key(appliance.id) {
                    val snappedCol = (appliance.positionX / tileWPx)
                        .roundToInt().coerceIn(0, roomConfig.widthMeters - 1)
                    val snappedRow = (appliance.positionY / tileHPx)
                        .roundToInt().coerceIn(0, roomConfig.heightMeters - 1)

                    val isoPos  = isoToScreen(
                        snappedCol + 0.5f, snappedRow + 0.5f,
                        tileWPx, tileHPx, originX, originY
                    )
                    val halfPx = with(density) { 44.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (isoPos.x - halfPx).roundToInt(),
                                    (isoPos.y - halfPx).roundToInt()
                                )
                            }
                            .size(88.dp)
                            .pointerInput(appliance.id, wallModeOn) {
                                if (!wallModeOn) {
                                    // Single pointerInput handles both tap and drag
                                    // to avoid gesture conflicts
                                    // awaitEachGesture restarts for every new touch
                                    awaitEachGesture {
                                        var isDragging = false
                                        var dragOffset = Offset.Zero
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        draggingId = appliance.id

                                        do {
                                            val event  = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (change.positionChanged()) {
                                                val delta = change.positionChange()
                                                dragOffset += delta
                                                if (dragOffset.getDistance() > 8f) {
                                                    isDragging = true
                                                    onApplianceDrag(appliance.id, delta.x, delta.y)
                                                    change.consume()
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })

                                        if (!isDragging) {
                                            onApplianceClick(appliance)
                                        } else {
                                            val center = Offset(isoPos.x, isoPos.y)
                                            if (trashBounds.contains(center)) onDelete(appliance.id)
                                        }
                                        draggingId = null
                                    }
                                }
                            }
                    ) {
                        ApplianceModelView(
                            appliance   = appliance,
                            engine      = engine,
                            modelLoader = modelLoader
                        )
                    }
                }
            }
        }

        // ── WALL MODE CHIPS ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp, start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallChip(
                label = if (wallModeOn) "✏️ Mode Dinding" else "➕ Dinding",
                color = if (wallModeOn) Color(0xFF4F8EF7) else Color(0xFF6B7280)
            ) { wallModeOn = !wallModeOn }
            if (walls.isNotEmpty()) {
                SmallChip("Hapus Dinding", Color(0xFFEF4444)) { walls = walls.dropLast(1) }
            }
        }

        // ── TRASH ─────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(52.dp)
                .onGloballyPositioned { coords -> trashBounds = coords.boundsInWindow() },
            shape  = CircleShape,
            color  = Color.Red.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) { Text("🗑️", fontSize = 20.sp) }
        }
    }
}

// ── 3D model view ─────────────────────────────────────────────────────────────
@Composable
fun ApplianceModelView(
    appliance: Appliance,
    engine: com.google.android.filament.Engine,
    modelLoader: ModelLoader
) {
    val glbPath = "models/${appliance.iconName}.glb"
    val config  = getModelConfig(appliance.iconName)
    val modelInstance = remember(glbPath) {
        modelLoader.createModelInstance(assetFileLocation = glbPath)
    }
    val isActive = appliance.hourUsage > 0

    Box(modifier = Modifier.fillMaxSize()) {
        if (modelInstance == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(getEmojiForIcon(appliance.iconName), fontSize = 32.sp)
            }
        } else {
            // key(rotationY) forces node recreation when rotation changes
            // because rememberNodes is memoized and won't update otherwise
            key(appliance.rotationY) {
                Scene(
                    modifier    = Modifier.fillMaxSize(),
                    engine      = engine,
                    modelLoader = modelLoader,
                    isOpaque    = false,
                    childNodes  = rememberNodes {
                        add(ModelNode(
                            modelInstance = modelInstance,
                            autoAnimate   = false,
                            scaleToUnits  = config.scale
                        ).apply {
                            position = Position(x = 0f, y = -config.cameraY, z = 0f)
                            rotation = Rotation(x = 0f, y = appliance.rotationY, z = 0f)
                        })
                    }
                )
            }
        }

        // Power dot
        Surface(
            modifier = Modifier.size(9.dp).align(Alignment.TopEnd),
            shape    = CircleShape,
            color    = if (isActive) Color(0xFF22C55E) else Color(0xFFEF4444),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
        ) {}

        // Name label
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(appliance.name, fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Small chip button ─────────────────────────────────────────────────────────
@Composable
private fun SmallChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(20.dp),
        color   = color.copy(alpha = 0.12f),
        border  = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = color,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ── Draw isometric room ───────────────────────────────────────────────────────
private fun DrawScope.drawIsometricRoom(
    widthM: Int, heightM: Int,
    tileW: Float, tileH: Float,
    originX: Float, originY: Float
) {
    for (row in 0 until heightM) {
        for (col in 0 until widthM) {
            val tl = isoToScreen(col.toFloat(), row.toFloat(),  tileW, tileH, originX, originY)
            val tr = isoToScreen(col + 1f,      row.toFloat(),  tileW, tileH, originX, originY)
            val br = isoToScreen(col + 1f,      row + 1f,       tileW, tileH, originX, originY)
            val bl = isoToScreen(col.toFloat(), row + 1f,       tileW, tileH, originX, originY)
            val path = Path().apply {
                moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
                lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
            }
            drawPath(path, if ((col + row) % 2 == 0) FloorLight else FloorDark)
            drawPath(path, GridLine, style = Stroke(1f))
        }
    }
    val wallHeight = tileH * 1.8f
    for (col in 0 until widthM) {
        val tl = isoToScreen(col.toFloat(), 0f, tileW, tileH, originX, originY)
        val tr = isoToScreen(col + 1f,      0f, tileW, tileH, originX, originY)
        val face = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tl.x, tl.y - wallHeight)
            lineTo(tr.x, tr.y - wallHeight); lineTo(tr.x, tr.y); close()
        }
        drawPath(face, WallLeft)
        drawPath(face, GridLine, style = Stroke(0.8f))
        val top = Path().apply {
            moveTo(tl.x, tl.y - wallHeight - tileH * 0.3f)
            lineTo(tr.x, tr.y - wallHeight - tileH * 0.3f)
            lineTo(tr.x, tr.y - wallHeight)
            lineTo(tl.x, tl.y - wallHeight); close()
        }
        drawPath(top, WallTop)
    }
    for (row in 0 until heightM) {
        val tl = isoToScreen(0f, row.toFloat(), tileW, tileH, originX, originY)
        val bl = isoToScreen(0f, row + 1f,      tileW, tileH, originX, originY)
        val face = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tl.x, tl.y - wallHeight)
            lineTo(bl.x, bl.y - wallHeight); lineTo(bl.x, bl.y); close()
        }
        drawPath(face, WallRight)
        drawPath(face, GridLine, style = Stroke(0.8f))
    }
    val tl = isoToScreen(0f,             0f,              tileW, tileH, originX, originY)
    val tr = isoToScreen(widthM.toFloat(), 0f,             tileW, tileH, originX, originY)
    val br = isoToScreen(widthM.toFloat(), heightM.toFloat(), tileW, tileH, originX, originY)
    val bl = isoToScreen(0f,             heightM.toFloat(), tileW, tileH, originX, originY)
    drawPath(Path().apply {
        moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
        lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
    }, Color(0xFF8A9BAD), style = Stroke(2f))
}

// ── Draw interior wall ────────────────────────────────────────────────────────
private fun DrawScope.drawIsoWall(
    wall: WallSegment, tileW: Float, tileH: Float, originX: Float, originY: Float
) {
    val start = isoToScreen(wall.startCol.toFloat(), wall.startRow.toFloat(), tileW, tileH, originX, originY)
    val end   = isoToScreen(wall.endCol.toFloat(),   wall.endRow.toFloat(),   tileW, tileH, originX, originY)
    val wallH = tileH * 1.8f
    val face  = Path().apply {
        moveTo(start.x, start.y); lineTo(start.x, start.y - wallH)
        lineTo(end.x,   end.y   - wallH); lineTo(end.x, end.y); close()
    }
    drawPath(face, InnerWall)
    drawPath(face, Color(0xFF7A8A99), style = Stroke(1.5f))
    val cap = Path().apply {
        moveTo(start.x, start.y - wallH)
        lineTo(start.x + tileW * 0.1f, start.y - wallH - tileH * 0.2f)
        lineTo(end.x   + tileW * 0.1f, end.y   - wallH - tileH * 0.2f)
        lineTo(end.x,   end.y   - wallH); close()
    }
    drawPath(cap, WallTop)
    drawPath(cap, Color(0xFF7A8A99), style = Stroke(1f))
}