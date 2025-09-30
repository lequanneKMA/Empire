package com.example.empire.ui.controls

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.empire.game.Direction
import com.example.empire.ui.common.PixelFont
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

        // --- DPad (trái dưới) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GhostButton("↑",
                onDown = { onDirection(Direction.Up) },
                onUp   = { onStop() }
            )
            Spacer(Modifier.height(8.dp))
            Row {
                GhostButton("←",
                    onDown = { onDirection(Direction.Left) },
                    onUp   = { onStop() }
                )
                Spacer(Modifier.width(12.dp))
                GhostButton("→",
                    onDown = { onDirection(Direction.Right) },
                    onUp   = { onStop() }
                )
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("↓",
                onDown = { onDirection(Direction.Down) },
                onUp   = { onStop() }
            )
        }

        // --- Action pad (phải dưới): Atk lớn + A/B/C ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // hàng trên: B
            GhostButton("B", width = 84, height = 52,
                onDown = onBDown, onUp = onBUp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    GhostButton("C", width = 84, height = 52, onDown = onCDown, onUp = onCUp)
                    Spacer(Modifier.height(10.dp))
                    GhostButton("A", width = 84, height = 52, onDown = onADown, onUp = onAUp)
                }
                Spacer(Modifier.width(12.dp))
                GhostButton("Atk", width = 120, height = 72, fontSize = 20,
                    onDown = onAttackDown, onUp = onAttackUp
                )
            }
        }
    }
}

/**
 * Nút "trong suốt": chỉ text PixelFont; nhận press & hold.
 * width/height theo dp, full vùng là hit area.
 */
@Composable
private fun GhostButton(
    label: String,
    width: Int = 96,
    height: Int = 56,
    fontSize: Int = 22,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    // Vẽ chữ ở giữa – không có nền/viền
    Box(
        modifier = Modifier
            .size(width.dp, height.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onDown()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onUp()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = PixelFont,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )
    }
}
