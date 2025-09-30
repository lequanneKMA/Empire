package com.example.empire.ui.gameplay

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.empire.game.GameView
import com.example.empire.game.Direction
import com.example.empire.ui.controls.ControlsOverlay

@Composable
fun GameplayScreen(nav: NavController) {
    val context = LocalContext.current
    val gameView = remember { GameView(context) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { _: Context -> gameView },
            modifier = Modifier.fillMaxSize()
        )

        ControlsOverlay(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            onDirection = { dir -> gameView.setDirection(dir) },
            onStop      = { gameView.stopMove() },
            onAttackDown= { gameView.pressAttack() },
            onAttackUp  = { gameView.releaseAttack() },
            onADown     = { gameView.pressA() },
            onAUp       = { gameView.releaseA() },
            onBDown     = { gameView.pressB() },
            onBUp       = { gameView.releaseB() },
            onCDown     = { gameView.pressC() },
            onCUp       = { gameView.releaseC() }
        )
    }
}
