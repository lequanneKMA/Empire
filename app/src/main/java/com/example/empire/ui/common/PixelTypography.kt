package com.example.empire.ui.common

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.empire.R

// file font phải đặt trong res/font với tên thường: press_start_2p_regular.ttf
val PixelFont = FontFamily(
    Font(R.font.vt323, weight = FontWeight.Normal)
)

val PixelTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontFamily = PixelFont),
    titleLarge    = Typography().titleLarge.copy(fontFamily = PixelFont),
    bodyMedium    = Typography().bodyMedium.copy(fontFamily = PixelFont),
    labelLarge    = Typography().labelLarge.copy(fontFamily = PixelFont)
)
