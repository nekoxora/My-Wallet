package com.example.mywallet.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mywallet.R
import kotlin.math.roundToInt

@Composable
fun DraggableBotIcon(onClick: () -> Unit = {}) {
    var localOffsetX by remember { mutableStateOf(BotIconSessionState.offsetX) }
    var localOffsetY by remember { mutableStateOf(BotIconSessionState.offsetY) }

    Box(
        modifier = Modifier
            .offset { IntOffset(localOffsetX.roundToInt(), localOffsetY.roundToInt()) }
            .padding(bottom = 240.dp, end = 24.dp)
            .size(65.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()

                        localOffsetX += dragAmount.x
                        localOffsetY += dragAmount.y

                        BotIconSessionState.offsetX = localOffsetX
                        BotIconSessionState.offsetY = localOffsetY
                    }
                )
            }
            .clip(CircleShape)
            .background(Color(0xFF2D2F45).copy(alpha = 0.6f))
            .border(1.5.dp, Color(0xFF06B6D4).copy(alpha = 0.8f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.icon_bot),
            contentDescription = "Bot AI",
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
    }
}
