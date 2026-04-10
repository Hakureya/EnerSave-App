package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.*

// ── Isometric math helpers ────────────────────────────────────────────────────
// Converts grid (col, row) in meters to isometric screen (x, y)
// Origin is top-center of the canvas
fun isoToScreen(col: Float, row: Float, tileW: Float, tileH: Float, originX: Float, originY: Float): Offset {
    return Offset(
        x = originX + (col - row) * tileW / 2f,
        y = originY + (col + row) * tileH / 2f
    )
}

// Converts screen tap position back to grid (col, row)
fun screenToIso(screenX: Float, screenY: Float, tileW: Float, tileH: Float, originX: Float, originY: Float): Pair<Int, Int> {
    val dx = screenX - originX
    val dy = screenY - originY
    val col = (dx / (tileW / 2f) + dy / (tileH / 2f)) / 2f
    val row = (dy / (tileH / 2f) - dx / (tileW / 2f)) / 2f
    return Pair(col.roundToInt(), row.roundToInt())
}

// Colors
private val FloorLight   = Color(0xFFDFD8C8)
private val FloorDark    = Color(0xFFCFC8B8)
private val WallLeft     = Color(0xFFB8C4D0)
private val WallRight    = Color(0xFFD0DBE5)
private val WallTop      = Color(0xFFE8EFF5)
private val GridLine     = Color(0xFFBBB4A4)
private val InnerWall    = Color(0xFF9AAABB)
private val WallDrawing  = Color(0xFF4F8EF7)

data class WallSegment(
    val startCol: Int, val startRow: Int,
    val endCol: Int,   val endRow: Int
)

@Composable
fun IsometricRoomCanvas(
    roomConfig: RoomConfig,
    activeAppliances: List<Appliance>,
    onApplianceClick: (Appliance) -> Unit,
    onApplianceDrag: (String, Float, Float) -> Unit,
    onDelete: (String) -> Unit
) {
    val engine      = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // Tile size: 1 meter = 100dp in world, but we scale to fit the canvas
    // tileW = isometric tile width (diamond width), tileH = half height
    val tileWDp = 80.dp
    val tileHDp = 40.dp

    var trashBounds by remember { mutableStateOf(Rect.Zero) }

    // Wall drawing state
    var walls         by remember { mutableStateOf(listOf<WallSegment>()) }
    var isDrawingWall by remember { mutableStateOf(false) }
    var wallStart     by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var wallEnd       by remember { mutableStateOf<Pair<Int,Int>?>(null) }
    var wallModeOn    by remember { mutableStateOf(false) }

    // Dragging
    var draggingId by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .onGloballyPositioned { coords ->
                canvasW = coords.size.width.toFloat()
                canvasH = coords.size.height.toFloat()
            }
    ) {
        val tileWPx   = with(density) { tileWDp.toPx() }
        val tileHPx   = with(density) { tileHDp.toPx() }

        // Origin = top-center, shifted down so the room fits
        val originX = canvasW / 2f
        val originY = with(density) { 60.dp.toPx() }

        // ── ISOMETRIC ROOM FLOOR + WALLS ──────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (wallModeOn) Modifier.pointerInput(roomConfig, tileWPx, tileHPx, originX, originY) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val (c, r) = screenToIso(offset.x, offset.y, tileWPx, tileHPx, originX, originY)
                                val cc = c.coerceIn(0, roomConfig.widthMeters)
                                val rc = r.coerceIn(0, roomConfig.heightMeters)
                                wallStart = Pair(cc, rc)
                                wallEnd   = Pair(cc, rc)
                                isDrawingWall = true
                            },
                            onDragEnd = {
                                val s = wallStart; val e = wallEnd
                                if (s != null && e != null && s != e) {
                                    walls = walls + WallSegment(s.first, s.second, e.first, e.second)
                                }
                                isDrawingWall = false
                                wallStart = null; wallEnd = null
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

            drawIsometricRoom(
                widthM  = roomConfig.widthMeters,
                heightM = roomConfig.heightMeters,
                tileW   = tileWPx,
                tileH   = tileHPx,
                originX = originX,
                originY = originY
            )

            // Draw saved walls
            walls.forEach { wall ->
                drawIsoWall(wall, tileWPx, tileHPx, originX, originY)
            }

            // Draw wall in progress
            if (isDrawingWall && wallStart != null && wallEnd != null) {
                val s = wallStart!!; val e = wallEnd!!
                val startPt = isoToScreen(s.first.toFloat(), s.second.toFloat(), tileWPx, tileHPx, originX, originY)
                val endPt   = isoToScreen(e.first.toFloat(), e.second.toFloat(), tileWPx, tileHPx, originX, originY)
                drawLine(WallDrawing, startPt, endPt, strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                drawCircle(WallDrawing, 6f, startPt)
                drawCircle(WallDrawing, 6f, endPt)
            }

            // Dimension labels
            drawDimensionLabel(
                text    = "${roomConfig.widthMeters} m",
                from    = isoToScreen(0f, roomConfig.heightMeters.toFloat(), tileWPx, tileHPx, originX, originY),
                to      = isoToScreen(roomConfig.widthMeters.toFloat(), roomConfig.heightMeters.toFloat(), tileWPx, tileHPx, originX, originY)
            )
            drawDimensionLabel(
                text    = "${roomConfig.heightMeters} m",
                from    = isoToScreen(roomConfig.widthMeters.toFloat(), 0f, tileWPx, tileHPx, originX, originY),
                to      = isoToScreen(roomConfig.widthMeters.toFloat(), roomConfig.heightMeters.toFloat(), tileWPx, tileHPx, originX, originY)
            )
        }

        // ── APPLIANCES (placed as Compose overlays at iso positions) ───────
        if (canvasW > 0f) {
            activeAppliances.forEach { appliance ->
                key(appliance.id) {
                    // snap to nearest grid cell
                    val snappedCol = (appliance.positionX / tileWPx)
                        .roundToInt().coerceIn(0, roomConfig.widthMeters - 1)
                    val snappedRow = (appliance.positionY / tileHPx)
                        .roundToInt().coerceIn(0, roomConfig.heightMeters - 1)

                    val isoPos = isoToScreen(
                        snappedCol.toFloat() + 0.5f,
                        snappedRow.toFloat() + 0.5f,
                        tileWPx, tileHPx, originX, originY
                    )

                    val isDragging = draggingId == appliance.id

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (isoPos.x - with(density) { 44.dp.toPx() }).roundToInt(),
                                    (isoPos.y - with(density) { 44.dp.toPx() }).roundToInt()
                                )
                            }
                            .size(88.dp)
                            .pointerInput(appliance.id) {
                                detectDragGestures(
                                    onDragStart = { draggingId = appliance.id },
                                    onDragEnd = {
                                        draggingId = null
                                        val center = Offset(isoPos.x, isoPos.y)
                                        if (trashBounds.contains(center)) {
                                            onDelete(appliance.id)
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    onApplianceDrag(appliance.id, dragAmount.x, dragAmount.y)
                                }
                            }
                            .pointerInput(appliance.id) {
                                detectTapGestures { onApplianceClick(appliance) }
                            }
                    ) {
                        IsometricApplianceItem(
                            appliance   = appliance,
                            engine      = engine,
                            modelLoader = modelLoader,
                            isDragging  = isDragging
                        )
                    }
                }
            }
        }

        // ── WALL MODE TOGGLE ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (walls.isNotEmpty()) {
                SmallChip(label = "Hapus Dinding", color = Color(0xFFEF4444)) {
                    walls = walls.dropLast(1)
                }
            }
            SmallChip(
                label = if (wallModeOn) "✏️ Mode Dinding" else "➕ Tambah Dinding",
                color = if (wallModeOn) Color(0xFF4F8EF7) else Color(0xFF6B7280)
            ) {
                wallModeOn = !wallModeOn
            }
        }

        // ── TRASH BUTTON ───────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .size(52.dp)
                .onGloballyPositioned { coords ->
                    trashBounds = coords.boundsInWindow()
                },
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗑️", fontSize = 20.sp)
            }
        }
    }
}

// ── Draw the isometric floor grid + walls ────────────────────────────────────
private fun DrawScope.drawIsometricRoom(
    widthM: Int, heightM: Int,
    tileW: Float, tileH: Float,
    originX: Float, originY: Float
) {
    // Draw floor tiles
    for (row in 0 until heightM) {
        for (col in 0 until widthM) {
            val tl = isoToScreen(col.toFloat(),       row.toFloat(),       tileW, tileH, originX, originY)
            val tr = isoToScreen(col.toFloat() + 1f,  row.toFloat(),       tileW, tileH, originX, originY)
            val br = isoToScreen(col.toFloat() + 1f,  row.toFloat() + 1f,  tileW, tileH, originX, originY)
            val bl = isoToScreen(col.toFloat(),        row.toFloat() + 1f,  tileW, tileH, originX, originY)

            val path = Path().apply {
                moveTo(tl.x, tl.y)
                lineTo(tr.x, tr.y)
                lineTo(br.x, br.y)
                lineTo(bl.x, bl.y)
                close()
            }

            val tileColor = if ((col + row) % 2 == 0) FloorLight else FloorDark
            drawPath(path, tileColor)
            drawPath(path, GridLine, style = Stroke(width = 1f))
        }
    }

    // Left wall (along row=0 axis, going down-left)
    for (col in 0 until widthM) {
        val tl = isoToScreen(col.toFloat(),      0f, tileW, tileH, originX, originY)
        val tr = isoToScreen(col.toFloat() + 1f, 0f, tileW, tileH, originX, originY)
        val wallHeight = tileH * 1.8f

        val leftFace = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tl.x, tl.y - wallHeight)
            lineTo(tr.x, tr.y - wallHeight)
            lineTo(tr.x, tr.y)
            close()
        }
        drawPath(leftFace, WallLeft)
        drawPath(leftFace, GridLine, style = Stroke(width = 0.8f))

        // Top cap
        val topFace = Path().apply {
            val topTL = isoToScreen(col.toFloat(),      0f, tileW, tileH, originX, originY - wallHeight)
            val topTR = isoToScreen(col.toFloat() + 1f, 0f, tileW, tileH, originX, originY - wallHeight)
            moveTo(topTL.x, topTL.y)
            lineTo(topTR.x, topTR.y)
            lineTo(tr.x, tr.y - wallHeight)
            lineTo(tl.x, tl.y - wallHeight)
            close()
        }
        drawPath(topFace, WallTop)
    }

    // Right wall (along col=0 axis, going down-right)
    for (row in 0 until heightM) {
        val tl = isoToScreen(0f, row.toFloat(),       tileW, tileH, originX, originY)
        val bl = isoToScreen(0f, row.toFloat() + 1f,  tileW, tileH, originX, originY)
        val wallHeight = tileH * 1.8f

        val rightFace = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tl.x, tl.y - wallHeight)
            lineTo(bl.x, bl.y - wallHeight)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(rightFace, WallRight)
        drawPath(rightFace, GridLine, style = Stroke(width = 0.8f))
    }

    // Room border outline
    val topLeft     = isoToScreen(0f,            0f,              tileW, tileH, originX, originY)
    val topRight    = isoToScreen(widthM.toFloat(), 0f,           tileW, tileH, originX, originY)
    val bottomRight = isoToScreen(widthM.toFloat(), heightM.toFloat(), tileW, tileH, originX, originY)
    val bottomLeft  = isoToScreen(0f,            heightM.toFloat(), tileW, tileH, originX, originY)

    val border = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topRight.x, topRight.y)
        lineTo(bottomRight.x, bottomRight.y)
        lineTo(bottomLeft.x, bottomLeft.y)
        close()
    }
    drawPath(border, Color.Transparent, style = Stroke(width = 2f))
    drawPath(border, Color(0xFF8A9BAD), style = Stroke(width = 2f))
}

// ── Draw a user-placed wall segment ──────────────────────────────────────────
private fun DrawScope.drawIsoWall(
    wall: WallSegment,
    tileW: Float, tileH: Float,
    originX: Float, originY: Float
) {
    val start  = isoToScreen(wall.startCol.toFloat(), wall.startRow.toFloat(), tileW, tileH, originX, originY)
    val end    = isoToScreen(wall.endCol.toFloat(),   wall.endRow.toFloat(),   tileW, tileH, originX, originY)
    val wallH  = tileH * 1.8f

    // Wall face (parallelogram)
    val face = Path().apply {
        moveTo(start.x, start.y)
        lineTo(start.x, start.y - wallH)
        lineTo(end.x,   end.y   - wallH)
        lineTo(end.x,   end.y)
        close()
    }
    drawPath(face, InnerWall)
    drawPath(face, Color(0xFF7A8A99), style = Stroke(width = 1.5f))

    // Top cap
    val topCap = Path().apply {
        moveTo(start.x, start.y - wallH)
        lineTo(start.x + tileW * 0.1f, start.y - wallH - tileH * 0.2f)
        lineTo(end.x   + tileW * 0.1f, end.y   - wallH - tileH * 0.2f)
        lineTo(end.x,   end.y   - wallH)
        close()
    }
    drawPath(topCap, WallTop)
    drawPath(topCap, Color(0xFF7A8A99), style = Stroke(width = 1f))
}

// ── Placeholder for dimension text (Canvas text needs android.graphics.Paint) ─
private fun DrawScope.drawDimensionLabel(text: String, from: Offset, to: Offset) {
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f + 16f)
    drawCircle(Color(0xFF4F8EF7).copy(alpha = 0.6f), 3f, mid)
}

// ── Small chip button ─────────────────────────────────────────────────────────
@Composable
private fun SmallChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ── Isometric appliance item ──────────────────────────────────────────────────
@Composable
fun IsometricApplianceItem(
    appliance: Appliance,
    engine: com.google.android.filament.Engine,
    modelLoader: ModelLoader,
    isDragging: Boolean
) {
    val glbPath       = "models/${appliance.iconName}.glb"
    val modelInstance = remember(glbPath) {
        modelLoader.createModelInstance(assetFileLocation = glbPath)
    }
    val isActive = appliance.hourUsage > 0

    Box(modifier = Modifier.fillMaxSize()) {
        if (modelInstance == null) {
            // Emoji fallback — centered
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(getEmojiForIcon(appliance.iconName), fontSize = 32.sp)
            }
        } else {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                isOpaque = false,
                childNodes = rememberNodes {
                    add(ModelNode(
                        modelInstance = modelInstance,
                        autoAnimate = false,
                        scaleToUnits = 1.0f
                    ))
                }
            )
        }

        // Power dot
        Surface(
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.TopEnd),
            shape = CircleShape,
            color = if (isActive) Color(0xFF22C55E) else Color(0xFFEF4444),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
        ) {}

        // Name label below
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Text(
                text = appliance.name,
                fontSize = 7.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}