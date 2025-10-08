package com.example.empire.ui.gameplay

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.empire.game.GameView
import com.example.empire.game.Direction
import com.example.empire.ui.controls.ControlsOverlay
import com.example.empire.ui.gameplay.PauseOverlay

@Composable
fun GameplayScreen(nav: NavController) {
    val context = LocalContext.current
    val gameView = remember { GameView(context) }
    var paused by remember { mutableStateOf(false) }

    fun setPaused(p: Boolean) {
        paused = p
        gameView.setPausedFromCompose(p) // extension method we'll add
    }

    // Lắng nghe thay đổi pause từ GameView (phím P hoặc nút pause trong canvas)
    LaunchedEffect(gameView) {
        gameView.onPauseChange = { p -> paused = p }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { _: Context -> gameView },
            modifier = Modifier.fillMaxSize()
        )

        ControlsOverlay(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            onDirection = { dir -> if (!paused) gameView.setDirection(dir) },
            onStop      = { gameView.stopMove() },
            onAttackDown= { if (!paused) gameView.pressAttack() },
            onAttackUp  = { gameView.releaseAttack() },
            onADown     = { if (paused) setPaused(false) else gameView.pressA() },
            onAUp       = { gameView.releaseA() },
            onBDown     = { if (paused) setPaused(false) else gameView.pressB() },
            onBUp       = { gameView.releaseB() },
            onCDown     = { /* no-op theo yêu cầu: nút C không làm gì */ },
            onCUp       = { gameView.releaseC() }
        )

        PauseOverlay(
            visible = paused,
            onResume = { setPaused(false) },
            onLoad = {
                setPaused(false)
                nav.navigate(com.example.empire.ui.Screen.SelectSave.route)
            },
            onSettings = {
                setPaused(false)
                nav.navigate(com.example.empire.ui.Screen.Settings.route)
            },
            onExit = { nav.popBackStack() }
        )
    }
}
