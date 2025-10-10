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
            // ignore
        }
    }

    fun draw(canvas: Canvas, assets: AssetManager) {
        if (!visible) return

        ensureBanner(assets)
        val bmp = banner // Lấy banner ra trước

        // --- Bắt đầu tính toán để căn giữa ---

        // 1. Tính kích thước banner sau khi scale
        val bannerWidth = canvas.width * 0.8f
        val bannerHeight = if (bmp != null) {
            (bannerWidth / bmp.width) * bmp.height
        } else {
            0f // Nếu không có banner thì chiều cao là 0
        }

        // 2. Định nghĩa kích thước khung text
        val panelWidth = canvas.width * 0.8f
        val panelHeight = 180f
        val spacing = 20f // Khoảng cách giữa banner và khung text

        // 3. Tính tổng chiều cao và căn giữa toàn bộ giao diện
        val totalHeight = bannerHeight + spacing + panelHeight
        var currentY = (canvas.height - totalHeight) / 2f // Đây là tọa độ Y bắt đầu của cả khối

        // 4. Vẽ banner "Congratulations!"
        if (bmp != null) {
            val bannerLeft = (canvas.width - bannerWidth) / 2f
            val bannerRect = RectF(bannerLeft, currentY, bannerLeft + bannerWidth, currentY + bannerHeight)
            canvas.drawBitmap(bmp, null, bannerRect, null)
            currentY += bannerHeight + spacing // Cập nhật tọa độ Y cho phần tiếp theo
        }

//        // 5. Vẽ khung đen chứa text
//        val panelLeft = (canvas.width - panelWidth) / 2f
//        val panelRect = RectF(panelLeft, currentY, panelLeft + panelWidth, currentY + panelHeight)
//        canvas.drawRoundRect(panelRect, 18f, 18f, panelPaint)
//        canvas.drawRoundRect(panelRect, 18f, 18f, panelBorder)
//
//        // 6. Căn giữa text trong khung
//        hudPaint.textAlign = Paint.Align.CENTER
//        val centerX = panelLeft + panelWidth / 2f
//
//        hudPaint.textSize = 50f
//        canvas.drawText("CHÚC MỪNG!", centerX, currentY + 60f, hudPaint)
//
//        hudPaint.textSize = 40f
//        canvas.drawText("A: Về Main Menu", centerX, currentY + 120f, hudPaint)
//
//        // Reset lại textAlign để không ảnh hưởng đến các phần UI khác
//        hudPaint.textAlign = Paint.Align.LEFT
    }
}