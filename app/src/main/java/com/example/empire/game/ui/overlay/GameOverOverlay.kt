package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class GameOverOverlay(
    private val panelPaint: Paint,
    private val panelBorder: Paint,
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    var visible = false
    fun show() { visible = true }
    fun hide() { visible = false }

    fun draw(canvas: Canvas) {
        if (!visible) return
        val w = canvas.width * 0.6f
        val h = 240f
        val left = (canvas.width - w)/2f
        val top = (canvas.height - h)/2f - uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 18f, 18f, panelPaint)
        canvas.drawRoundRect(rect, 18f, 18f, panelBorder)
        hudPaint.textSize = 36f
        canvas.drawText("GAME OVER", left + 40f, top + 70f, hudPaint)
        hudPaint.textSize = 16f
        canvas.drawText("A: Quay về nhà chính", left + 40f, top + 120f, hudPaint)
        canvas.drawText("B: Thoát về Start", left + 40f, top + 150f, hudPaint)
        hudPaint.textSize = 14f
    }
}
