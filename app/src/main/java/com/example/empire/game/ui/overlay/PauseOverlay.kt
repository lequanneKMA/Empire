package com.example.empire.game.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.empire.ui.common.McButton

/** Simple pause menu overlay reusing McButton style. */
@Composable
fun PauseOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    onLoad: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            McButton("Tiếp tục") { onResume() }
            Spacer(Modifier.height(14.dp))
            McButton("Load") { onLoad() }
            Spacer(Modifier.height(14.dp))
            McButton("Cài đặt") { onSettings() }
            Spacer(Modifier.height(14.dp))
            McButton("Thoát") { onExit() }
        }
    }
}
