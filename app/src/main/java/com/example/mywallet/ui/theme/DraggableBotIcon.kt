package com.example.mywallet.ui.theme

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mywallet.R
import kotlin.math.roundToInt

@Composable
fun DraggableBotIcon(onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var offsetX by remember { mutableStateOf(prefs.getFloat("icon_x", 0f)) }
    var offsetY by remember { mutableStateOf(prefs.getFloat("icon_y", 0f)) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(bottom = 125.dp, end = 24.dp)
            .size(65.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        prefs.edit()
                            .putFloat("icon_x", offsetX)
                            .putFloat("icon_y", offsetY)
                            .apply()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .clip(CircleShape)
            .background(Color(0xFF2D2F45).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f), CircleShape)
            .clickable {
                onClick()
            },
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