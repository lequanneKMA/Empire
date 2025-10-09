package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import com.example.empire.game.army.UnitType
import com.example.empire.game.economy.CostTable
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.army.Unlocks

class BuyMenuOverlay(
    private val panelPaint: Paint,
    private val panelBorder: Paint,
    private val highlightPaint: Paint,
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    interface Listener {
        fun onBuy(type: UnitType)
        fun onClose()
    }
    var listener: Listener? = null
    var visible = false
    var selection = 0
    private val entries = listOf(
        UnitType.WARRIOR,
        UnitType.LANCER,
        UnitType.ARCHER,
        UnitType.MONK
    )

    fun open() { visible = true; selection = 0 }
    fun close() { visible = false }
    fun navigate(dy: Int) { if (!visible) return; selection = (selection + dy + entries.size) % entries.size }

    fun draw(canvas: Canvas, resources: ResourceManager, unlocks: Unlocks) {
        if (!visible) return

        // Layout giống MapSelect
        val screenW = canvas.width.toFloat()
        val w = (screenW * 0.72f).coerceAtMost(980f)
        val left = (screenW - w) / 2f
        val top = 64f + uiInset
        val headerH = 56f
        val rowH = 48f
        val footerH = 40f
        val innerPad = 16f
        val h = headerH + (entries.size * (rowH + 6f)) + footerH + innerPad * 2

        val outer = RectF(left, top, left + w, top + h)
        val inner = RectF(outer.left + 6f, outer.top + 6f, outer.right - 6f, outer.bottom - 6f)

        // Backdrop premium
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

        // Title + resources
        val prevSize = hudPaint.textSize
        val prevBold = hudPaint.isFakeBoldText
        val prevColor = hudPaint.color
        val prevAlign = hudPaint.textAlign

        hudPaint.textAlign = Paint.Align.LEFT
        hudPaint.textSize = 30f
        hudPaint.isFakeBoldText = true
        hudPaint.color = Color.WHITE
        canvas.drawText("MUA QUÂN", headerRect.left + 16f, headerRect.centerY() + 9f, hudPaint)

        // Hiển thị tài nguyên hiện có ở góc phải header
        hudPaint.textAlign = Paint.Align.RIGHT
        hudPaint.textSize = 24f
        hudPaint.isFakeBoldText = false
        hudPaint.color = Color.argb(235, 240, 240, 245)
        canvas.drawText("Gold: ${resources.gold}   Meat: ${resources.meat}", headerRect.right - 16f, headerRect.centerY() + 7f, hudPaint)

        // Rows
        val listLeft = inner.left + innerPad
        val listRight = inner.right - innerPad
        var rowTop = headerRect.bottom + 10f
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(36, 255, 255, 255) }
        val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 214, 64) }
        val selectStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = Color.argb(220, 255, 232, 120)
        }

        entries.forEachIndexed { idx, type ->
            val rowRect = RectF(listLeft, rowTop, listRight, rowTop + rowH)
            if (idx % 2 == 0) canvas.drawRoundRect(rowRect, 10f, 10f, stripePaint)
            if (idx == selection) {
                canvas.drawRoundRect(rowRect, 12f, 12f, selectPaint)
                canvas.drawRoundRect(rowRect, 12f, 12f, selectStroke)
            }

            val name = type.name.lowercase().replaceFirstChar { it.uppercase() }
            val cost = CostTable.get(type)
            val unlocked = when (type) {
                UnitType.LANCER -> unlocks.lancerUnlocked
                UnitType.ARCHER -> unlocks.archerUnlocked
                UnitType.MONK -> unlocks.monkUnlocked
                UnitType.WARRIOR -> true
            }
            val affordable = resources.gold >= cost.gold && resources.meat >= cost.meat
            val locked = !unlocked
            val (chipText, chipColor) = when {
                locked -> "LOCK" to Color.argb(220, 236, 80, 80)
                !affordable -> "NO$" to Color.argb(220, 240, 170, 64)
                else -> "OK" to Color.argb(220, 88, 186, 120)
            }

            // Name
            hudPaint.textAlign = Paint.Align.LEFT
            hudPaint.textSize = 25f
            hudPaint.isFakeBoldText = true
            hudPaint.color = Color.WHITE
            canvas.drawText(name, rowRect.left + 14f, rowRect.centerY() + 7f, hudPaint)

            // Cost
            hudPaint.textSize = 21f
            hudPaint.isFakeBoldText = false
            hudPaint.color = Color.argb(220, 210, 220, 230)
            canvas.drawText("G:${cost.gold}  M:${cost.meat}", rowRect.left + 180f, rowRect.centerY() + 6f, hudPaint)

            // Status chip
            val chipW = 80f
            val chipH = 30f
            val chipRect = RectF(rowRect.right - chipW, rowRect.centerY() - chipH/2f, rowRect.right, rowRect.centerY() + chipH/2f)
            val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipColor }
            canvas.drawRoundRect(chipRect, 12f, 12f, chipBg)
            val chipStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
                color = Color.argb(230, 255, 255, 255)
            }
            canvas.drawRoundRect(chipRect, 12f, 12f, chipStroke)
            hudPaint.textAlign = Paint.Align.CENTER
            hudPaint.textSize = 14f
            hudPaint.color = Color.WHITE
            canvas.drawText(chipText, chipRect.centerX(), chipRect.centerY() + 5f, hudPaint)

            rowTop += rowH + 6f
        }
        // Restore
        hudPaint.textAlign = prevAlign
        hudPaint.textSize = prevSize
        hudPaint.isFakeBoldText = prevBold
        hudPaint.color = prevColor
    }

    fun confirm(resources: ResourceManager, unlocks: Unlocks) {
        if (!visible) return
        val type = entries[selection]
        listener?.onBuy(type)
    }
}
