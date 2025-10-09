package com.example.empire.ui.gameplay

import com.example.empire.game.GameView

/**
 * Simple holder to keep a single GameView instance across navigation screens
 * (e.g., when opening Settings and popping back). This avoids resetting the
 * map/state to defaults when the Gameplay composable is recreated.
 */
object GameViewHolder {
    var gameView: GameView? = null
}
