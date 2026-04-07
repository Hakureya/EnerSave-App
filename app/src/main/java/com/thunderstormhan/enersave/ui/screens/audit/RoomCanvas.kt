package com.thunderstormhan.enersave.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {

        // --- LAYER 1: TRASH BUTTON ---
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 200.dp, end = 24.dp)
                .size(64.dp)
                .onGloballyPositioned { coords ->
                    trashBounds = coords.boundsInWindow()
                },
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗑️", fontSize = 26.sp)
            }
        }

        // --- LAYER 2: APPLIANCES ---
        activeAppliances.forEach { appliance ->
            key(appliance.id) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                appliance.positionX.roundToInt(),
                                appliance.positionY.roundToInt()
                            )
                        }
                        .pointerInput(appliance.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    val center = Offset(
                                        appliance.positionX,
                                        appliance.positionY
                                    )
                                    if (trashBounds.contains(center)) {
                                        onDelete(appliance.id)
                                    }
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
                        onClick = { onApplianceClick(appliance) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThreeDItem(
    appliance: Appliance,
    engine: com.google.android.filament.Engine,
    modelLoader: ModelLoader,
    onClick: () -> Unit
) {
    // iconName is now "folder/filename" e.g. "AC/air_conditioner"
    // We append .glb here so the ViewModel stays clean
    val glbPath = "models/${appliance.iconName}.glb"

    val modelInstance = remember(glbPath) {
        modelLoader.createModelInstance(assetFileLocation = glbPath)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable { onClick() }
        ) {
            if (modelInstance == null) {
                // .glb not found — show emoji fallback
                Text(
                    text = getEmojiForIcon(appliance.iconName),
                    fontSize = 40.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = modelLoader,
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

            // Power status dot
            val isActive = appliance.hourUsage > 0
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp),
                shape = CircleShape,
                color = if (isActive) Color(0xFF22C55E) else Color(0xFFEF4444),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
            ) {}
        }

        Text(
            text = appliance.name,
            fontSize = 9.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold
        )
    }
}

// Used for emoji fallback — matches the folder name part of iconName
fun getEmojiForIcon(iconName: String): String {
    val folder = iconName.substringBefore("/").lowercase()
    return when (folder) {
        "ac"             -> "❄️"
        "fan",
        "ceiling_fan"    -> "🌀"
        "computer"       -> "💻"
        "light"          -> "💡"
        "fridge"         -> "🧊"
        "blender"        -> "🫙"
        "hair_dryer"     -> "💨"
        "iron"           -> "👕"
        "printer"        -> "🖨️"
        "rice_cooker"    -> "🍚"
        "security_camera"-> "📷"
        "tv"             -> "📺"
        "washing_machine"-> "🫧"
        else             -> "🔌"
    }
}