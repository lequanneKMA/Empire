package com.example.empire.game.ui.overlay

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class WinnerOverlay(
    private val panelPaint: Paint,
    private val panelBorder: Paint,
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    var visible = false
    private var banner: Bitmap? = null

    fun show() { visible = true }
    fun hide() { visible = false }

    private fun ensureBanner(assets: AssetManager) {
        if (banner != null) return
        try {
            assets.open("ui/winner.png").use { ins ->
                banner = BitmapFactory.decodeStream(ins)
            }
        } catch (_: Exception) {
            // ignore if not found; overlay will just be text-only
        }
    }

    fun draw(canvas: Canvas, assets: AssetManager) {
        if (!visible) return
        ensureBanner(assets)
        val w = canvas.width * 0.7f
        val h = 280f
        val left = (canvas.width - w)/2f
        val top = (canvas.height - h)/2f - uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 18f, 18f, panelPaint)
        canvas.drawRoundRect(rect, 18f, 18f, panelBorder)

        val bmp = banner
        if (bmp != null) {
            val scale = (w * 0.8f) / bmp.width
            val bw = bmp.width * scale
            val bh = bmp.height * scale
            val bx = left + (w - bw)/2f
            val by = top + 30f
            val dst = RectF(bx, by, bx + bw, by + bh)
            canvas.drawBitmap(bmp, null, dst, null)
        }

        hudPaint.textSize = 30f
        canvas.drawText("CHÚC MỪNG!", left + 40f, top + h - 90f, hudPaint)
        hudPaint.textSize = 18f
        canvas.drawText("A: Về Main Menu", left + 40f, top + h - 50f, hudPaint)
        hudPaint.textSize = 14f
    }
}
