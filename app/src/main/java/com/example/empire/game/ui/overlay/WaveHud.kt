package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF

class WaveHud(
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    private var waveLabel: Bitmap? = null
    private var waveLabelH = 0
    private fun ensureWaveLabel(assets: android.content.res.AssetManager) {
        if (waveLabel != null) return
        try {
            assets.open("ui/Buttons/wave.png").use { s ->
                waveLabel = BitmapFactory.decodeStream(s)
            }
        } catch (_: Exception) { /* silently ignore, will fallback to text only */ }
    }

    fun draw(
        canvas: Canvas,
        waveMode: Boolean,
        current: Int,
        total: Int,
        cooldown: Float,
        inCooldown: Boolean,
        assets: android.content.res.AssetManager? = null
    ) {
        if (!waveMode) return

    val iconH = 56 // bigger icon for readability
    val right = canvas.width - uiInset
    val top = 82f + uiInset

        // Try load icon if assets provided
        assets?.let { ensureWaveLabel(it) }
        val bmp = waveLabel

        // Draw shadow text for readability
        val prevSize = hudPaint.textSize
        val prevColor = hudPaint.color
        hudPaint.textSize = 20f

        var textCx = right - 80f
        var textCy = top + iconH/2f + 6f
        if (bmp != null) {
            val scale = iconH.toFloat() / bmp.height
            val w = (bmp.width * scale).toInt()
            val h = iconH
            val dst = RectF(right - w, top, right.toFloat(), top + h)
            // simple drop shadow
            val shadow = Paint(hudPaint).apply { color = Color.argb(120, 0, 0, 0) }
            canvas.drawBitmap(bmp, null, RectF(dst.left+2, dst.top+2, dst.right+2, dst.bottom+2), shadow)
            canvas.drawBitmap(bmp, null, dst, null)
            textCx = (dst.left + dst.right) / 2f
            textCy = dst.centerY() + 7f
        } else {
            // fallback label
            val label = "WAVE"
            hudPaint.color = Color.BLACK
            canvas.drawText(label, right - 70f + 2f, top + 30f + 2f, hudPaint)
            hudPaint.color = Color.WHITE
            canvas.drawText(label, right - 70f, top + 30f, hudPaint)
            textCx = right - 80f
            textCy = top + 34f
        }

        val text = if (inCooldown) "${String.format("%.1f", cooldown)}s" else "$current/$total"
        // Larger text and centered within the icon area
        hudPaint.textAlign = Paint.Align.CENTER
        hudPaint.textSize = 22f
        hudPaint.color = Color.BLACK
        canvas.drawText(text, textCx + 2f, textCy + 2f, hudPaint)
        hudPaint.color = Color.WHITE
        canvas.drawText(text, textCx, textCy, hudPaint)
        hudPaint.textAlign = Paint.Align.LEFT

        hudPaint.textSize = prevSize
        hudPaint.color = prevColor
    }
}
