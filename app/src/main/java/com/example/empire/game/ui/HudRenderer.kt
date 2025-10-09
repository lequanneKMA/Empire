package com.example.empire.game.ui

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.empire.R
import com.example.empire.game.army.ArmySystem
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.player.PlayerStats
import com.example.empire.game.progression.ProgressionManager
import android.graphics.Paint.FontMetrics
import android.content.Context
import androidx.core.content.res.ResourcesCompat

/**
 * Draws the on-screen HUD (HP, XP, resources, army counts, map name etc.).
 * Extracted from GameView to keep that file smaller.
 */
class HudRenderer(
    private val res: Resources,
    private val assets: AssetManager,
    private val packageName: String,
    private val hudPaint: Paint,
    private val hpBarPaint: Paint,
    private val hpBarBackPaint: Paint,
    private val xpBarPaint: Paint,
    private val xpBarBackPaint: Paint,
    private val uiInset: Float,
    private val context: Context
) {
    private val pixelTypeface = ResourcesCompat.getFont(context, R.font.vt323)
    init {
        // Dòng quan trọng nhất: áp dụng font pixel cho bút vẽ HUD
        hudPaint.typeface = pixelTypeface
    }
    // Load drawables by name to tolerate possible naming (hp_bar_full vs hp_bar_Full)
    private val hpEmptyRaw: Bitmap? by lazy {
        val id1 = res.getIdentifier("hp_bar_empty", "drawable", packageName)
        val id = if (id1 != 0) id1 else res.getIdentifier("hp_bar_Empty", "drawable", packageName)
        if (id != 0) BitmapFactory.decodeResource(res, id) else null
    }
    private val hpFullRaw: Bitmap? by lazy {
        val id1 = res.getIdentifier("hp_bar_full", "drawable", packageName)
        val id = if (id1 != 0) id1 else res.getIdentifier("hp_bar_Full", "drawable", packageName)
        if (id != 0) BitmapFactory.decodeResource(res, id) else null
    }
    private var scaledW = 0
    private var scaledH = 0
    private var hpEmptyScaled: Bitmap? = null
    private var hpFullScaled: Bitmap? = null

    // Resource icons and ribbon from assets (with multiple fallbacks)
    private fun loadAssetBitmap(vararg candidates: String): Bitmap? {
        for (p in candidates) {
            try {
                assets.open(p).use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                // try next candidate
            }
        }
        return null
    }

    private val goldRaw: Bitmap? by lazy {
        loadAssetBitmap(
            "sprites/spawn/Gold.png",
        )
    }
    private val meatRaw: Bitmap? by lazy {
        loadAssetBitmap(
            "sprites/spawn/Meat.png", // Added capitalized version
        )
    }
    private val ribbonRaw: Bitmap? by lazy {
        loadAssetBitmap(
            "ui/Ribbon.png"
        )
    }
    // NEW: Load background for Level display
    private var levelButtonScaled: Bitmap? = null
    private var levelButtonScaledW = 0
    private var levelButtonScaledH = 0
    private val levelButtonRaw: Bitmap? by lazy {
        loadAssetBitmap(
            "ui/level_button.png"
        )
    }

    private var goldScaled: Bitmap? = null
    private var meatScaled: Bitmap? = null
    private var goldScaledH = 0
    private var meatScaledH = 0
    private var ribbonScaled: Bitmap? = null
    private var ribbonScaledW = 0
    private var ribbonScaledH = 0



    private fun ensureHpScaled(targetW: Int, targetH: Int) {
        if (scaledW == targetW && scaledH == targetH) return
        hpEmptyScaled = hpEmptyRaw?.let { Bitmap.createScaledBitmap(it, targetW, targetH, true) }
        hpFullScaled = hpFullRaw?.let { Bitmap.createScaledBitmap(it, targetW, targetH, true) }
        scaledW = targetW; scaledH = targetH
    }
    /**
     * Hàm phụ để vẽ một mục tài nguyên (icon và chữ)
     * @return Vị trí X tiếp theo để vẽ mục kế tiếp
     */
    private fun drawResourceItem(
        canvas: Canvas,
        icon: Bitmap?,
        text: String,
        x: Float,
        y: Float,
        iconHeight: Int,
        textGap: Float
    ): Float {
        var currentX = x
        icon?.let {
            // Tính toán để căn icon và chữ thẳng hàng với nhau theo chiều dọc
            val iconCenterY = y + it.height / 2f
            val textBaselineY = iconCenterY - (hudPaint.descent() + hudPaint.ascent()) / 2f

            // Vẽ icon
            canvas.drawBitmap(it, currentX, y, null)
            currentX += it.width + textGap // Di chuyển X sang phải

            // Vẽ chữ
            canvas.drawText(text, currentX, textBaselineY, hudPaint)
            currentX += hudPaint.measureText(text) // Di chuyển X sang phải
        }
        return currentX
    }

    fun draw(
        canvas: Canvas,
        playerStats: PlayerStats,
        progression: ProgressionManager,
        resources: ResourceManager,
        armySystem: ArmySystem,
        currentMapId: String,
        screenW: Int,
        screenH: Int
    ) {
        // --- Các biến chung ---
        val barW = 320f
        val margin = 16f
        val hpX = margin + uiInset
        var currentY = margin + uiInset

        // Lưu và khôi phục trạng thái Paint
        val prevAlign = hudPaint.textAlign
        val prevSize = hudPaint.textSize
        val prevBold = hudPaint.isFakeBoldText
        val prevColor = hudPaint.color

        // --- 1. Thanh máu (Giữ nguyên) ---
        val barH = 80f
        ensureHpScaled(barW.toInt(), barH.toInt())
        // ... (Toàn bộ code vẽ thanh máu được giữ nguyên)
        val hpPerc = (playerStats.hp.toFloat() / playerStats.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
        if (hpEmptyScaled != null) {
            canvas.drawBitmap(hpEmptyScaled!!, hpX, currentY, null)
            if (hpPerc > 0f && hpFullScaled != null) {
                val save = canvas.save()
                canvas.clipRect(hpX, currentY, hpX + barW * hpPerc, currentY + barH)
                canvas.drawBitmap(hpFullScaled!!, hpX, currentY, null)
                canvas.restoreToCount(save)
            }
        } else {
            // Fallback: draw simple rectangle HP bar if images are missing
            canvas.drawRect(hpX, currentY, hpX + barW, currentY + barH, hpBarBackPaint)
            if (hpPerc > 0f) {
                canvas.drawRect(hpX, currentY, hpX + barW * hpPerc, currentY + barH, hpBarPaint)
            }
        }
        hudPaint.textAlign = Paint.Align.CENTER
        hudPaint.textSize = 27f
        hudPaint.isFakeBoldText = true
        hudPaint.color = Color.WHITE
        val hpText = "${playerStats.hp}/${playerStats.maxHp}"
        canvas.drawText(hpText, hpX + barW / 2f, currentY + barH * 0.65f, hudPaint)
        currentY += barH + margin / 2


        // --- 2. Nút hiển thị Level (Mới) ---
        val levelButtonW = 120f
        val levelButtonH = 50f
        if (levelButtonScaled == null || levelButtonScaledW != levelButtonW.toInt() || levelButtonScaledH != levelButtonH.toInt()) {
            levelButtonRaw?.let {
                levelButtonScaled = Bitmap.createScaledBitmap(it, levelButtonW.toInt(), levelButtonH.toInt(), true)
            }
            levelButtonScaledW = levelButtonW.toInt()
            levelButtonScaledH = levelButtonH.toInt()
        }
        levelButtonScaled?.let {
            canvas.drawBitmap(it, hpX, currentY, null)
            hudPaint.textSize = 28f
            hudPaint.isFakeBoldText = true
            val levelText = "LV ${progression.tier + 1}"
            val levelTextY = (currentY + levelButtonH / 2f) - ((hudPaint.descent() + hudPaint.ascent()) / 2f)
            canvas.drawText(levelText, hpX + levelButtonW / 2f, levelTextY, hudPaint)
        }
        currentY += levelButtonH + margin / 2


        // --- 3. Thanh kinh nghiệm (Mới) ---
        currentY += 20f
        val xpBarH = 30f
    val xpNeeded = progression.currentThreshold
    val xpPerc = if (xpNeeded > 0) (progression.currentLevelXp.toFloat() / xpNeeded) else 1f
        canvas.drawRect(hpX, currentY, hpX + barW, currentY + xpBarH, xpBarBackPaint)
        canvas.drawRect(hpX, currentY, hpX + barW * xpPerc, currentY + xpBarH, xpBarPaint)
        hudPaint.textSize = 28f
        hudPaint.isFakeBoldText = false
    val expText = "EXP ${progression.currentLevelXp}/${xpNeeded}"
        val expTextY = (currentY + xpBarH / 2f) - ((hudPaint.descent() + hudPaint.ascent()) / 2f)
        canvas.drawText(expText, hpX + barW / 2f, expTextY, hudPaint)
        currentY += xpBarH + margin


        // --- 4. Hàng tài nguyên (Phiên bản cải tiến, dễ căn chỉnh) ---

// =================== KHU VỰC TÙY CHỈNH CHÍNH ===================
        val resIconH = 80f       // Chiều cao của icon (giữ nguyên)
        val resTextSize = 30f    // Cỡ chữ (giữ nguyên)
        val textVerticalAlign = 0.75f
        val leftPadding = 0f    // Khoảng cách từ lề trái
        val textGap = 0f        // Khoảng cách giữa icon và chữ
        val itemGap = 20f        // Khoảng cách giữa cụm Vàng và cụm Thịt
// =============================================================

        var resX = hpX + leftPadding
        hudPaint.textAlign = Paint.Align.LEFT
        hudPaint.textSize = resTextSize
        hudPaint.isFakeBoldText = true

        // Scale icons (phần này không cần chỉnh)
        val targetIconH = resIconH.toInt()
        if (goldScaled == null || goldScaledH != targetIconH) {
            goldScaled = goldRaw?.let { Bitmap.createScaledBitmap(it, (it.width * (resIconH / it.height)).toInt(), targetIconH, true) }
            goldScaledH = targetIconH
        }
        if (meatScaled == null || meatScaledH != targetIconH) {
            meatScaled = meatRaw?.let { Bitmap.createScaledBitmap(it, (it.width * (resIconH / it.height)).toInt(), targetIconH, true) }
            meatScaledH = targetIconH
        }

        // Căn chỉnh Vàng
        goldScaled?.let {
            // Logic căn chỉnh mới, đơn giản hơn
            val textBaselineY = currentY + (it.height * textVerticalAlign)
            canvas.drawBitmap(it, resX, currentY, null)
            resX += it.width + textGap
            canvas.drawText("${resources.gold}", resX, textBaselineY, hudPaint)
            resX += hudPaint.measureText("${resources.gold}") + itemGap
        }

        // Căn chỉnh Thịt
        meatScaled?.let {
            // Logic căn chỉnh mới, đơn giản hơn
            val textBaselineY = currentY + (it.height * textVerticalAlign)
            canvas.drawBitmap(it, resX, currentY, null)
            resX += it.width + textGap
            canvas.drawText("${resources.meat}", resX, textBaselineY, hudPaint)
        }

        // ---- Top Center map ribbon ----
        val ribbonTargetW = 350
        if (ribbonScaled == null || ribbonScaledW != ribbonTargetW) {
            ribbonScaled = ribbonRaw?.let { src ->
                val scale = ribbonTargetW.toFloat() / src.width
                val h = (src.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, ribbonTargetW, h, true)
            }
            ribbonScaledW = ribbonTargetW
            ribbonScaledH = ribbonScaled?.height ?: 0
        }
        ribbonScaled?.let { bmp ->
            // --- LOGIC CĂN GIỮA MỚI ---

            val leftHudWidth = barW
            val rightHudWidth = 260f
            val centerOffset = (leftHudWidth - rightHudWidth) / 2f
            val visualCx = screenW / 2f + centerOffset
            val top = margin + uiInset
            val left = visualCx - bmp.width / 2f // Dùng vị trí trung tâm mới
            canvas.drawBitmap(bmp, left, top, null)

            val title = if (currentMapId.equals("main", ignoreCase = true)) "Nhà chính" else currentMapId
            val saveSize = hudPaint.textSize
            val saveBold = hudPaint.isFakeBoldText
            val saveAlign2 = hudPaint.textAlign

            hudPaint.textSize = 40f
            hudPaint.isFakeBoldText = true
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.color = Color.WHITE

            val yPos = (top + bmp.height / 2f) - ((hudPaint.descent() + hudPaint.ascent()) / 2f) - 4f
            canvas.drawText(title, visualCx, yPos, hudPaint) // Dùng vị trí trung tâm mới

            hudPaint.textAlign = prevAlign
            hudPaint.textSize = prevSize
            hudPaint.isFakeBoldText = prevBold
            hudPaint.color = prevColor
        }
    }
}