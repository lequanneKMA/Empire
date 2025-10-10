package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader

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

        val screenW = canvas.width.toFloat()
        val screenH = canvas.height.toFloat()
        val w = (screenW * 0.72f).coerceAtMost(980f)
        val left = (screenW - w) / 2f
        val headerH = 72f
        val bodyH = 140f
        val innerPad = 18f
        val h = headerH + bodyH + innerPad * 2
        val top = (screenH - h) / 2f - uiInset

        val outer = RectF(left, top, left + w, top + h)
        val inner = RectF(outer.left + 6f, outer.top + 6f, outer.right - 6f, outer.bottom - 6f)

        // Backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 14, 18, 26)
            setShadowLayer(20f, 0f, 10f, Color.argb(120, 0, 0, 0))
        }
        canvas.drawRoundRect(outer, 22f, 22f, bgPaint)

        val grad = LinearGradient(
            0f, inner.top, 0f, inner.bottom,
            intArrayOf(Color.argb(200, 24, 30, 44), Color.argb(200, 18, 22, 32)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad }
        canvas.drawRoundRect(inner, 18f, 18f, innerPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(180, 230, 230, 235)
        }
        canvas.drawRoundRect(inner, 18f, 18f, borderPaint)

        // Header ribbon
        val headerRect = RectF(inner.left + innerPad, inner.top + innerPad, inner.right - innerPad, inner.top + innerPad + headerH)
        val headerGrad = LinearGradient(
            headerRect.left, headerRect.top, headerRect.left, headerRect.bottom,
            intArrayOf(Color.rgb(210, 62, 62), Color.rgb(156, 28, 28)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = headerGrad }
        canvas.drawRoundRect(headerRect, 16f, 16f, headerPaint)
        val headerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(220, 255, 255, 255)
        }
        canvas.drawRoundRect(headerRect, 16f, 16f, headerStroke)

        val prevSize = hudPaint.textSize
        val prevBold = hudPaint.isFakeBoldText
        val prevColor = hudPaint.color
        val prevAlign = hudPaint.textAlign

        hudPaint.textAlign = Paint.Align.CENTER
        hudPaint.isFakeBoldText = true
        hudPaint.color = Color.WHITE
        hudPaint.textSize = 56f
        canvas.drawText("GAME OVER", headerRect.centerX(), headerRect.centerY() + 18f, hudPaint)

        // Body content
        val bodyRect = RectF(inner.left + innerPad, headerRect.bottom + 8f, inner.right - innerPad, inner.bottom - innerPad)
        val textY = bodyRect.top + 52f
        hudPaint.textSize = 28f
        hudPaint.isFakeBoldText = true
        hudPaint.textAlign = Paint.Align.LEFT
        hudPaint.color = Color.argb(235, 255, 255, 255)
        canvas.drawText("A: Chơi lại tại nhà chính", bodyRect.left + 16f, textY, hudPaint)
        canvas.drawText("B: Thoát về Start", bodyRect.left + 16f, textY + 40f, hudPaint)

        // Restore HUD paint
        hudPaint.textAlign = prevAlign
        hudPaint.textSize = prevSize
        hudPaint.isFakeBoldText = prevBold
        hudPaint.color = prevColor
    }
}
