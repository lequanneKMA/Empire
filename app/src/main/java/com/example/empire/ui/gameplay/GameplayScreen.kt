package com.example.empire.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.empire.game.controls.DPad
import com.example.empire.core.input.inputManager
import androidx.compose.ui.viewinterop.AndroidView
import com.example.empire.game.GameView




@Composable
fun GameplayScreen(nav: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Composable game host vẽ player (mình dùng Compose version để dễ debug)
        AndroidView(
            factory = { context -> GameView(context) },
            modifier = Modifier.fillMaxSize()
        )
        // DPad overlay ở góc dưới trái
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            DPad(
                input = inputManager,
                modifier = Modifier
            )
        }
    }
}
