package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.thunderstormhan.enersave.data.model.Room
import kotlin.math.*
import kotlinx.coroutines.launch

// Scale: 1 meter = 56dp per tile on main canvas
private const val TILE_W_DP = 56f
private const val TILE_H_DP = 28f
private const val WALL_RATIO = 1.6f
private const val SNAP_DP    = 36f

// Colors matching IsometricRoomCanvas
private val MFloorLight = Color(0xFFDFD8C8)
private val MFloorDark  = Color(0xFFCFC8B8)
private val MWallLeft   = Color(0xFFB8C4D0)
private val MWallRight  = Color(0xFFD0DBE5)
private val MWallTop    = Color(0xFFE8EFF5)
private val MGridLine   = Color(0xFFBBB4A4)

@Composable
fun MainRoomCanvas(
    rooms: List<Room>,
    placements: Set<String>,
    savedPositions: Map<String, Pair<Float, Float>>,
    onPositionChanged: (String, Float, Float) -> Unit
) {
    val density  = LocalDensity.current
    val tileWPx  = with(density) { TILE_W_DP.dp.toPx() }
    val tileHPx  = with(density) { TILE_H_DP.dp.toPx() }
    val wallHPx  = tileHPx * WALL_RATIO
    val snapPx   = with(density) { SNAP_DP.dp.toPx() }

    val placedRooms = rooms.filter { placements.contains(it.id) }

    // Seed initial positions synchronously so nothing is empty on first frame
    val initialPositions = remember(savedPositions, placements) {
        val map = mutableMapOf<String, Offset>()
        placedRooms.forEachIndexed { index, room ->
            val saved = savedPositions[room.id]
            map[room.id] = if (saved != null) {
                Offset(saved.first, saved.second)
            } else {
                Offset(
                    x = tileWPx * 3f + index * (room.widthMeters * tileWPx * 0.6f),
                    y = wallHPx * 2f + index * (room.widthMeters * tileHPx * 0.3f)
                )
            }
        }
        map
    }

    // Mutable positions for smooth dragging — initialized from savedPositions
    val positions = remember(savedPositions, placements) {
        mutableStateMapOf<String, Offset>().also { it.putAll(initialPositions) }
    }

    // Clean up removed rooms
    positions.keys.toList().forEach { id ->
        if (!placements.contains(id)) positions.remove(id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        // Dot grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sp = tileWPx / 2f
            var x = sp
            while (x < size.width) {
                var y = sp
                while (y < size.height) {
                    drawCircle(Color(0xFFCDD5E0), 2f, Offset(x, y))
                    y += sp
                }
                x += sp
            }
        }

        // Placed room blocks
        placedRooms.forEach { room ->
            val pos = positions[room.id] ?: return@forEach

            // Isometric room screen dimensions
            val roomScreenW = (room.widthMeters + room.heightMeters) * tileWPx / 2f
            val roomScreenH = (room.widthMeters + room.heightMeters) * tileHPx / 2f + wallHPx

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - roomScreenW / 2f).roundToInt(),
                            pos.y.roundToInt()
                        )
                    }
                    .size(
                        width  = with(density) { roomScreenW.toDp() },
                        height = with(density) { roomScreenH.toDp() }
                    )
                    .pointerInput(room.id) {
                        detectDragGestures(
                            onDragEnd = {
                                // Snap to nearest room edge
                                val cur = positions[room.id] ?: return@detectDragGestures
                                val snapped = snapToRoom(
                                    id      = room.id,
                                    pos     = cur,
                                    room    = room,
                                    all     = placedRooms,
                                    allPos  = positions,
                                    tileWPx = tileWPx,
                                    tileHPx = tileHPx,
                                    snapPx  = snapPx
                                )
                                positions[room.id] = snapped
                                // Save to ViewModel so position persists across tab switches
                                onPositionChanged(room.id, snapped.x, snapped.y)
                            }
                        ) { change, drag ->
                            change.consume()
                            val cur = positions[room.id] ?: Offset.Zero
                            val next = Offset(
                                x = (cur.x + drag.x).coerceAtLeast(roomScreenW / 2f),
                                y = (cur.y + drag.y).coerceAtLeast(0f)
                            )
                            positions[room.id] = next
                            // Save position live while dragging
                            onPositionChanged(room.id, next.x, next.y)
                        }
                    }
            ) {
                // Draw isometric room using Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ox = size.width / 2f   // origin X (top diamond point)
                    val oy = wallHPx            // origin Y

                    drawIsoFloor(room.widthMeters, room.heightMeters, tileWPx, tileHPx, ox, oy)
                    drawIsoWalls(room.widthMeters, room.heightMeters, tileWPx, tileHPx, wallHPx, ox, oy)
                }

                // Room name label at the top wall
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(density) { (wallHPx * 0.2f).toDp() })
                ) {
                    Text(
                        text       = room.name,
                        fontSize   = 9.sp,
                        color      = Color(0xFF4A5568),
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                }

                // Appliance emojis
                room.appliances.forEach { appliance ->
                    val col = (appliance.positionX / 100f * room.widthMeters)
                        .coerceIn(0f, room.widthMeters - 0.5f)
                    val row = (appliance.positionY / 100f * room.heightMeters)
                        .coerceIn(0f, room.heightMeters - 0.5f)

                    val ox  = with(density) { (roomScreenW / 2f).toDp() }
                    val oy  = with(density) { wallHPx.toDp() }
                    val exDp = with(density) {
                        (((col - row) * tileWPx / 2f) + roomScreenW / 2f).toDp()
                    }
                    val eyDp = with(density) {
                        (wallHPx + (col + row) * tileHPx / 2f + tileHPx / 2f).toDp()
                    }

                    Box(modifier = Modifier.offset(x = exDp - 8.dp, y = eyDp - 10.dp)) {
                        Text(getEmojiForIcon(appliance.iconName), fontSize = 11.sp)
                    }
                }
            }
        }

        // Empty state hint
        if (placedRooms.isEmpty()) {
            Text(
                text       = "Ketuk ruangan di bawah\nuntuk memulai denah rumah",
                color      = Color.Gray,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ── Snap room flush to nearest other room edge ────────────────────────────────
// In isometric space:
//   "Right" edge of a room = its bottom-right corner = col=widthM, row=0..heightM
//   "Left"  edge of a room = its top-left corner    = col=0, row=0..heightM
//   We connect rooms by aligning their isometric grid origins.
//
// Room origin (pos) = the isometric TOP DIAMOND POINT (col=0, row=0).
// Width in screen X  = widthMeters  * tileW/2 + heightMeters * tileW/2
// Height in screen Y = widthMeters  * tileH/2 + heightMeters * tileH/2
//
// To attach RIGHT side of A to LEFT side of B:
//   B.origin.x = A.origin.x + A.widthMeters * tileW  (shift along col axis)
//   B.origin.y = A.origin.y + A.widthMeters * tileH  (isometric y also shifts)
//   → new A.origin = B.origin - (A.widthM * tileW, A.widthM * tileH)
//
// To attach BOTTOM of A to TOP of B:
//   B.origin.x = A.origin.x - A.heightMeters * tileW/2 (shift along row axis — x decreases)
//   B.origin.y = A.origin.y + A.heightMeters * tileH/2
//   → new A.origin = B.origin + (A.heightM * tileW/2, -A.heightM * tileH/2) ... wait
//   Actually in iso: moving along ROW axis:  dx = -tileW/2, dy = +tileH/2  per row
//   So A right edge along rows = pos + (heightM * -tileW/2, heightM * tileH/2)

private fun snapToRoom(
    id: String, pos: Offset, room: Room,
    all: List<Room>, allPos: Map<String, Offset>,
    tileWPx: Float, tileHPx: Float, snapPx: Float
): Offset {
    var bestPos  = pos
    var bestDist = Float.MAX_VALUE

    // Isometric axis vectors (per 1 meter):
    // Moving along COL (width)  axis: dx = +tileW/2, dy = +tileH/2
    // Moving along ROW (height) axis: dx = -tileW/2, dy = +tileH/2
    val colDx = tileWPx / 2f;  val colDy = tileHPx / 2f
    val rowDx = -tileWPx / 2f; val rowDy = tileHPx / 2f

    all.forEach { other ->
        if (other.id == id) return@forEach
        val op = allPos[other.id] ?: return@forEach

        // Other room's four corner positions in screen space:
        // other top-left  origin = op
        // other top-right = op + other.widthM * col
        val oTopRight = Offset(
            op.x + other.widthMeters * colDx,
            op.y + other.widthMeters * colDy
        )
        // other bot-left = op + other.heightM * row
        val oBotLeft  = Offset(
            op.x + other.heightMeters * rowDx,
            op.y + other.heightMeters * rowDy
        )

        // room's four attachment offsets from its own origin:
        // room top-right = room.widthM * col axis
        val rTopRightOffset = Offset(room.widthMeters * colDx, room.widthMeters * colDy)
        // room bot-left  = room.heightM * row axis
        val rBotLeftOffset  = Offset(room.heightMeters * rowDx, room.heightMeters * rowDy)

        // 4 snap candidates:
        // 1. room's RIGHT edge (col side) → other's LEFT edge (other origin)
        //    room.origin + rTopRightOffset = op  →  room.origin = op - rTopRightOffset
        val snap1 = op - rTopRightOffset

        // 2. room's LEFT edge (origin) → other's RIGHT edge
        //    room.origin = oTopRight
        val snap2 = oTopRight

        // 3. room's BOTTOM edge (row side) → other's TOP edge (other origin)
        //    room.origin + rBotLeftOffset = op  →  room.origin = op - rBotLeftOffset
        val snap3 = op - rBotLeftOffset

        // 4. room's TOP edge (origin row) → other's BOTTOM edge
        //    room.origin = oBotLeft
        val snap4 = oBotLeft

        listOf(snap1, snap2, snap3, snap4).forEach { candidate ->
            val dist = (candidate - pos).getDistance()
            if (dist < snapPx && dist < bestDist) {
                bestDist = dist
                bestPos  = candidate
            }
        }
    }

    return bestPos
}

// ── Draw isometric floor tiles ────────────────────────────────────────────────
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIsoFloor(
    widthM: Int, heightM: Int, tileW: Float, tileH: Float, ox: Float, oy: Float
) {
    for (row in 0 until heightM) {
        for (col in 0 until widthM) {
            val tl = isoPoint(col.toFloat(),   row.toFloat(),   tileW, tileH, ox, oy)
            val tr = isoPoint(col + 1f,         row.toFloat(),   tileW, tileH, ox, oy)
            val br = isoPoint(col + 1f,         row + 1f,        tileW, tileH, ox, oy)
            val bl = isoPoint(col.toFloat(),    row + 1f,        tileW, tileH, ox, oy)
            val path = Path().apply {
                moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
                lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
            }
            drawPath(path, if ((col + row) % 2 == 0) MFloorLight else MFloorDark)
            drawPath(path, MGridLine, style = Stroke(0.8f))
        }
    }
}

// ── Draw isometric perimeter walls ───────────────────────────────────────────
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIsoWalls(
    widthM: Int, heightM: Int, tileW: Float, tileH: Float, wallH: Float, ox: Float, oy: Float
) {
    // Back wall (top edge)
    for (col in 0 until widthM) {
        val tl = isoPoint(col.toFloat(), 0f, tileW, tileH, ox, oy)
        val tr = isoPoint(col + 1f,      0f, tileW, tileH, ox, oy)
        val face = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tl.x, tl.y - wallH)
            lineTo(tr.x, tr.y - wallH); lineTo(tr.x, tr.y); close()
        }
        drawPath(face, MWallLeft)
        drawPath(face, MGridLine, style = Stroke(0.6f))
        // Top cap
        val top = Path().apply {
            moveTo(tl.x, tl.y - wallH - tileH * 0.3f)
            lineTo(tr.x, tr.y - wallH - tileH * 0.3f)
            lineTo(tr.x, tr.y - wallH); lineTo(tl.x, tl.y - wallH); close()
        }
        drawPath(top, MWallTop)
    }
    // Left wall
    for (row in 0 until heightM) {
        val tl = isoPoint(0f, row.toFloat(), tileW, tileH, ox, oy)
        val bl = isoPoint(0f, row + 1f,      tileW, tileH, ox, oy)
        val face = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tl.x, tl.y - wallH)
            lineTo(bl.x, bl.y - wallH); lineTo(bl.x, bl.y); close()
        }
        drawPath(face, MWallRight)
        drawPath(face, MGridLine, style = Stroke(0.6f))
    }
}

private fun isoPoint(col: Float, row: Float, tileW: Float, tileH: Float, ox: Float, oy: Float) =
    Offset(ox + (col - row) * tileW / 2f, oy + (col + row) * tileH / 2f)