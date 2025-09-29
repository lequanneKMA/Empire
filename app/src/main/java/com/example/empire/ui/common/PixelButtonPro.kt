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
fun PixelButtonPro(
    text: String,
    modifier: Modifier = Modifier,
    // >>> tham số mới để dễ chỉnh
    minWidthDp: Dp = 150.dp,
    heightDp: Dp = 56.dp,
    fontSizeSp: Int = 25,
    onClick: () -> Unit
) {
    val shape  = RoundedCornerShape(6.dp)
    val bg     = Color(0xFF6E56CF)   // tím lavender
    val border = Color(0xFF2B234B)   // viền tím đậm
    val content= Color(0xFFF4EFFF)   // chữ sáng

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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontFamily = PixelFont,          // VT323 (hỗ trợ tiếng Việt)
            fontWeight = FontWeight.Normal,
            fontSize = fontSizeSp.sp
        )
    }
}
