package com.example.empire.game.controls
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.empire.core.input.Direction
import com.example.empire.core.input.InputManager
import com.example.empire.ui.common.PixelFont
import kotlin.math.floor

/**
 * D-pad 4 hướng (không chéo) với cơ chế pad-lock: một ngón giữ quyền, trượt để đổi hướng.
 * Khi nhả -> dừng.
 */
@Composable
fun DPad(
    input: InputManager,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,          // kích thước tổng D-pad (vuông)
    corner: Dp = 12.dp,         // bo nền D-pad
    hitSlopExtra: Dp = 16.dp    // nới vùng chạm xung quanh để trượt không bị tuột
) {
    // trạng thái hiện tại để hiển thị pressed
    var current by remember { mutableStateOf(Direction.None) }

    // màu sắc
    val padBg = Color(0x33000000)      // nền mờ
    val keyIdle = Color(0x66FFFFFF)    // nút thường
    val keyActive = Color(0xCCFFFFFF)  // nút được nhấn
    val keyBorder = Color(0x22000000)

    Box(
        modifier = modifier
            .size(size)
            .background(padBg, RoundedCornerShape(corner))
            .padding(10.dp)
            .dpadPointer(
                onDirection = { dir ->
                    if (dir != current) {
                        current = dir
                        input.setDirection(dir)
                    }
                },
                onStop = {
                    current = Direction.None
                    input.stop()
                },
                hitSlopExtra = hitSlopExtra
            )
    ) {
        // lưới 3x3: chỉ vẽ 4 ô Up/Left/Right/Down, ô giữa rỗng
        val keyModifier = Modifier
            .size((size - 20.dp) / 3f) // trừ padding 10dp mỗi cạnh rồi chia 3
            .padding(2.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: [ ] [UP] [ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(keyModifier)
                DPadKey("↑", current == Direction.Up, keyIdle, keyActive, keyBorder, keyModifier)
                Spacer(keyModifier)
            }
            // Row 2: [LEFT] [ ] [RIGHT]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DPadKey("←", current == Direction.Left, keyIdle, keyActive, keyBorder, keyModifier)
                Spacer(keyModifier)
                DPadKey("→", current == Direction.Right, keyIdle, keyActive, keyBorder, keyModifier)
            }
            // Row 3: [ ] [DOWN] [ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(keyModifier)
                DPadKey("↓", current == Direction.Down, keyIdle, keyActive, keyBorder, keyModifier)
                Spacer(keyModifier)
            }
        }
    }
}

@Composable
private fun DPadKey(
    label: String,
    active: Boolean,
    idleColor: Color,
    activeColor: Color,
    borderColor: Color,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(if (active) activeColor else idleColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = PixelFont,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
    }
}

/* ------------------------- pointer handling ------------------------- */

/**
 * Pointer input cho D-pad (pad-lock):
 * - Nhận ngón đầu tiên chạm trong vùng -> giữ quyền.
 * - Trượt trong vùng -> maps vị trí sang 1 trong 4 hướng (không có chéo).
 * - Nhả -> onStop()
 */
private fun Modifier.dpadPointer(
    onDirection: (Direction) -> Unit,
    onStop: () -> Unit,
    hitSlopExtra: Dp
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        // 1) Bắt ngón đầu tiên chạm vào vùng
        val first = awaitFirstDown(requireUnconsumed = false)
        val activeId: PointerId = first.id

        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val extra = hitSlopExtra.toPx()

        fun map(pos: Offset): Direction {
            val x = pos.x.coerceIn(-extra, w + extra)
            val y = pos.y.coerceIn(-extra, h + extra)
            val cellW = w / 3f
            val cellH = h / 3f
            val cx = floor(x / cellW).toInt().coerceIn(0, 2)
            val cy = floor(y / cellH).toInt().coerceIn(0, 2)
            return when {
                cx == 1 && cy == 0 -> Direction.Up
                cx == 0 && cy == 1 -> Direction.Left
                cx == 2 && cy == 1 -> Direction.Right
                cx == 1 && cy == 2 -> Direction.Down
                else -> Direction.None
            }
        }

        // báo hướng ngay khi chạm lần đầu
        onDirection(map(first.position))

        // 2) Theo dõi NGÓN ĐANG GIỮ cho tới khi nhả
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == activeId } ?: break

            if (change.pressed) {
                onDirection(map(change.position))
                change.consume()
            } else {
                onStop()
                break
            }
        }
    }
}

