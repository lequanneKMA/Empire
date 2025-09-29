// ui/common/McButton.kt
package com.example.empire.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun McButton(
    text: String,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    height: Dp = 35.dp,
    textSizeSp: Int = 22,
    onClick: () -> Unit,
) {
    val corner = RoundedCornerShape(6.dp)

    Button(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(height),
        shape = corner,
        border = BorderStroke(3.dp, Color(0xFF2B2B2B)),       // viền đậm
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD8D8D8),               // nền xám sáng kiểu Minecraft
            contentColor   = Color(0xFF1C1C1C)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        Text(
            text = text,
            fontFamily = PixelFont,                            // VT323
            fontSize = textSizeSp.sp
        )
    }
}
