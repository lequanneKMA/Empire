// ui/common/PixelDangerButton.kt
package com.example.empire.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PixelDangerButton(
    text: String = "Xóa",
    modifier: Modifier = Modifier,
    minWidthDp: Dp = 96.dp,
    heightDp: Dp = 48.dp,
    fontSizeSp: Int = 16,
    onClick: () -> Unit
) {
    val shape   = RoundedCornerShape(6.dp)
    val bg      = Color(0xFFE5484D)      // đỏ
    val border  = Color(0xFF7A1D20)      // viền đỏ sẫm
    val content = Color(0xFFFFF6F6)

    ElevatedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = minWidthDp, minHeight = heightDp),
        shape = shape,
        border = BorderStroke(2.dp, border),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp, pressedElevation = 2.dp
        ),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = bg,
            contentColor   = content,
            disabledContainerColor = bg.copy(alpha = 0.4f),
            disabledContentColor   = content.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Normal,
            fontSize = fontSizeSp.sp
        )
    }
}
