package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader

class HousePromptOverlay(
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
        val w = (screenW * 0.72f).coerceAtMost(980f)
        val left = (screenW - w) / 2f
        val top = 64f + uiInset
        val headerH = 56f
        val bodyH = 70f
        val footerH = 38f
        val innerPad = 16f
        val h = headerH + bodyH + footerH + innerPad * 2

        val outer = RectF(left, top, left + w, top + h)
        val inner = RectF(outer.left + 6f, outer.top + 6f, outer.right - 6f, outer.bottom - 6f)

        // Backdrop like MapSelect
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
            intArrayOf(Color.rgb(48, 122, 200), Color.rgb(36, 92, 156)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = headerGrad }
        canvas.drawRoundRect(headerRect, 14f, 14f, headerPaint)
        val headerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(200, 255, 255, 255)
        }
        canvas.drawRoundRect(headerRect, 14f, 14f, headerStroke)

        val prevSize = hudPaint.textSize
        val prevBold = hudPaint.isFakeBoldText
        val prevColor = hudPaint.color
        val prevAlign = hudPaint.textAlign

        hudPaint.textAlign = Paint.Align.CENTER
        hudPaint.textSize = 26f
        hudPaint.isFakeBoldText = true
        hudPaint.color = Color.WHITE
        canvas.drawText("Nhà chính", headerRect.centerX(), headerRect.centerY() + 9f, hudPaint)

        // Body text
        val bodyRect = RectF(inner.left + innerPad, headerRect.bottom + 10f, inner.right - innerPad, headerRect.bottom + 10f + bodyH)
        hudPaint.textAlign = Paint.Align.LEFT
        hudPaint.textSize = 20f
        hudPaint.isFakeBoldText = false
        hudPaint.color = Color.argb(235, 240, 240, 245)
        canvas.drawText("Bạn có muốn vào nhà chính?", bodyRect.left, bodyRect.centerY(), hudPaint)

        // Footer help
        val footerY = inner.bottom - innerPad
        hudPaint.textSize = 16f
        hudPaint.color = Color.argb(220, 230, 230, 236)
        canvas.drawText("A: Vào  B: Hủy", bodyRect.left, footerY - 6f, hudPaint)

        // Restore
        hudPaint.textAlign = prevAlign
        hudPaint.textSize = prevSize
        hudPaint.isFakeBoldText = prevBold
        hudPaint.color = prevColor
    }
}
