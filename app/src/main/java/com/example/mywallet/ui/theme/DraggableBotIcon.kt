package com.example.mywallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.mywallet.R
import kotlin.math.roundToInt

@Composable
fun DraggableBotIcon(onClick: () -> Unit = {}) {
    val density = LocalDensity.current

    val paddingBottomPx = with(density) { 125.dp.toPx() }
    val paddingEndPx = with(density) { 24.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        var iconSize by remember { mutableStateOf(IntSize.Zero) }

        var localOffsetX by remember { mutableStateOf(BotIconSessionState.offsetX) }
        var localOffsetY by remember { mutableStateOf(BotIconSessionState.offsetY) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .onSizeChanged { iconSize = it }
                .offset { IntOffset(localOffsetX.roundToInt(), localOffsetY.roundToInt()) }
                .padding(bottom = 235.dp, end = 24.dp)
                .size(65.dp)
                .pointerInput(iconSize, maxWidthPx, maxHeightPx) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val iconW = iconSize.width.toFloat()
                            val iconH = iconSize.height.toFloat()

                            val minX = -(maxWidthPx - paddingEndPx - iconW)
                            val maxX = paddingEndPx

                            val minY = -(maxHeightPx - paddingBottomPx - iconH)
                            val maxY = paddingBottomPx

                            localOffsetX = (localOffsetX + dragAmount.x).coerceIn(minX, maxX)
                            localOffsetY = (localOffsetY + dragAmount.y).coerceIn(minY, maxY)

                            BotIconSessionState.offsetX = localOffsetX
                            BotIconSessionState.offsetY = localOffsetY
                        }
                    )
                }
                .clip(CircleShape)
                .background(Color(0xFF2D2F45).copy(alpha = 0.5f))
                .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.graph_ui),
                contentDescription = "Bot AI",
                tint = Color(0xFF06B6D4),
                modifier = Modifier.size(35.dp)
            )
        }
    }
}
