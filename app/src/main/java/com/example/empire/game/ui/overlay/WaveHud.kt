package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint

class WaveHud(
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    fun draw(canvas: Canvas, waveMode: Boolean, current: Int, total: Int, cooldown: Float, inCooldown: Boolean) {
        if (!waveMode) return
        hudPaint.textSize = 14f
        val x = canvas.width - 150f - uiInset
        val y = 90f + uiInset
        if (inCooldown) {
            canvas.drawText("Wave cooldown: ${String.format("%.1f", cooldown)}s", x, y, hudPaint)
        } else {
            canvas.drawText("Wave: $current/$total", x, y, hudPaint)
        }
    }
}
