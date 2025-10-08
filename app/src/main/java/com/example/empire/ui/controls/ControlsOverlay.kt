package com.example.empire.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.empire.game.Direction
import com.example.empire.ui.common.PixelFont
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp

/**
 * Overlay controls: stateless, chỉ phát callback.
 * Nút "trong suốt": chỉ text (dùng font của PixelButtonPro), chạm cả vùng.
 */
@Composable
fun ControlsOverlay(
    modifier: Modifier = Modifier,
    onDirection: (Direction) -> Unit,
    onStop: () -> Unit,
    onAttackDown: () -> Unit,
    onAttackUp: () -> Unit,
    onADown: () -> Unit,
    onAUp: () -> Unit,
    onBDown: () -> Unit,
    onBUp: () -> Unit,
    onCDown: () -> Unit,
    onCUp: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {

        // ================= D-Pad (left bottom) =================
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(75.dp)
        ) {
            val btnSize = 56.dp
            val gap = 8.dp
            // Up
            SquareDPadButton("↑", btnSize, Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-btnSize - gap/2)),
                onDown = { onDirection(Direction.Up) }, onUp = onStop)
            // Down
            SquareDPadButton("↓", btnSize, Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (btnSize + gap/2)),
                onDown = { onDirection(Direction.Down) }, onUp = onStop)
            // Left
            SquareDPadButton("←", btnSize, Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-btnSize - gap/2)),
                onDown = { onDirection(Direction.Left) }, onUp = onStop)
            // Right
            SquareDPadButton("→", btnSize, Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (btnSize + gap/2)),
                onDown = { onDirection(Direction.Right) }, onUp = onStop)
            // Center (optional neutral) – left blank but could add a dot
            Box(
                Modifier.size(btnSize)
                    .align(Alignment.Center)
            )
        }

        // ================= Action Cluster (right bottom) =================
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            val attackSize = 90.dp
            val smallSize = 60.dp
            val offset = 78.dp

            // Big attack button center
            CircleAttackButton(
                diameter = attackSize,
                label = "",
                modifier = Modifier.offset(y = (offset * 0.4f)),
                fist = true,
                onDown = onAttackDown,
                onUp = onAttackUp
            )
            // Ability buttons around (A left, B top-right, C bottom-right) for balance
            CircleAbilityButton(
                label = "A",
                diameter = smallSize,
                modifier = Modifier.offset(x = (-offset * 0.65f), y = (-offset * 0.45f)),
                onDown = onADown,
                onUp = onAUp
            )
            CircleAbilityButton(
                label = "B",
                diameter = smallSize,
                modifier = Modifier.offset(x = (offset * 0.3f), y = (-offset * 0.90f)),
                onDown = onBDown,
                onUp = onBUp
            )
            CircleAbilityButton(
                label = "C",
                diameter = smallSize,
                modifier = Modifier.offset(x = (-offset * 0.8f), y = (offset * 0.55f)),
                onDown = onCDown,
                onUp = onCUp
            )
        }
    }
}

/**
 * Nút "trong suốt": chỉ text PixelFont; nhận press & hold.
 * width/height theo dp, full vùng là hit area.
 */
// =============== Reusable Buttons ===============

@Composable
private fun PressButton(
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true; onDown()
                        try { tryAwaitRelease() } finally { pressed = false; onUp() }
                    }
                )
            }
            .then( if (pressed) Modifier else Modifier ),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SquareDPadButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    PressButton(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        onDown = onDown,
        onUp = onUp
    ) {
        Text(
            text = label,
            fontFamily = PixelFont,
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CircleAbilityButton(
    label: String,
    diameter: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    PressButton(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind {
                // outer translucent ring (use DrawScope.size for px)
                val px = this.size.minDimension
                drawCircle(Color.White.copy(alpha = 0.15f), style = Stroke(width = px * 0.35f))
                drawCircle(Color.White.copy(alpha = 0.55f), style = Stroke(width = 3f))
            }
            .background(Color.White.copy(alpha = 0.10f), CircleShape),
        onDown = onDown,
        onUp = onUp
    ) {
        Text(
            text = label,
            fontFamily = PixelFont,
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CircleAttackButton(
    diameter: Dp,
    label: String,
    fist: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier
) {
    PressButton(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind {
                // gradient fill + double ring
                drawCircle(Color.White.copy(alpha = 0.10f))
                drawCircle(Color.White.copy(alpha = 0.40f), style = Stroke(width = 5.8f))
                drawCircle(Color.White.copy(alpha = 0.70f), radius = this.size.minDimension/2.1f, style = Stroke(width = 3.8f))
            },
        onDown = onDown,
        onUp = onUp
    ) {
        if (fist) {
            Text(
                text = "⚔️", 
                fontSize = (diameter.value * 0.42f).sp
            )
        } else {
            Text(
                text = label,
                fontFamily = PixelFont,
                fontSize = (diameter.value * 0.25f).sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(
    name = "Controls Overlay – Portrait",
    showBackground = true,
    backgroundColor = 0xFF0E1420
)
@Composable
private fun PreviewControlsOverlayPortrait() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0E1420))) {
        ControlsOverlay(
            onDirection = {},
            onStop = {},
            onAttackDown = {},
            onAttackUp = {},
            onADown = {},
            onAUp = {},
            onBDown = {},
            onBUp = {},
            onCDown = {},
            onCUp = {}
        )
    }
}

@Preview(
    name = "Controls Overlay – Wide",
    widthDp = 800,
    heightDp = 400,
    showBackground = true,
    backgroundColor = 0xFF0E1420
)
@Composable
private fun PreviewControlsOverlayWide() {
    PreviewControlsOverlayPortrait()
}
