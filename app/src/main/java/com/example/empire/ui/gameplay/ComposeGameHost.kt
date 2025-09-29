package com.example.empire.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.empire.core.input.inputManager
import kotlin.math.roundToInt
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun ComposeGameHost() {
    // player pixel coordinates (px)
    var px by remember { mutableStateOf(0f) }
    var py by remember { mutableStateOf(0f) }

    var fps by remember { mutableStateOf(0) }

    // container size in pixels (updated by onSizeChanged)
    var container by remember { mutableStateOf(IntSize.Zero) }

    // player size in px (we'll use 48.dp)
    val playerDp: Dp = 48.dp
    val density = LocalDensity.current
    val playerPx = with(density) { playerDp.toPx() }

    // center initial when container known
    LaunchedEffect(container) {
        if (container.width > 0 && px == 0f && py == 0f) {
            px = (container.width - playerPx) / 2f
            py = (container.height - playerPx) / 2f
        }
    }

    // game loop (frame synced)
    LaunchedEffect(Unit) {
        var prev = System.nanoTime()
        val speed = 220f // pixels per second
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - prev) / 1_000_000_000f
            prev = now
            val d = inputManager.dpad.value
            // move by integer dx/dy (-1/0/1)
            px += d.dx * speed * dt
            py += d.dy * speed * dt

            // clamp inside container
            val maxX = (container.width - playerPx).coerceAtLeast(0f)
            val maxY = (container.height - playerPx).coerceAtLeast(0f)
            if (px < 0f) px = 0f
            if (py < 0f) py = 0f
            if (px > maxX) px = maxX
            if (py > maxY) py = maxY

            fps = if (dt > 0f) (1f / dt).roundToInt() else 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .onSizeChanged { container = it } // cập nhật kích thước container
    ) {
        // Draw player as white square on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.White,
                topLeft = Offset(px, py),
                size = androidx.compose.ui.geometry.Size(playerPx, playerPx)
            )
        }

        Text(
            text = "FPS: $fps",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}
