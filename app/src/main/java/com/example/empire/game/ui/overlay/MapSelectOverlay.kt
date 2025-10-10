package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import com.example.empire.game.ecs.systems.SpawnSystem

/** Overlay hiển thị danh sách map và cho chọn. */
class MapSelectOverlay(
    private val panelPaint: Paint,
    private val panelBorder: Paint,
    private val highlightPaint: Paint,
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    data class Entry(
        val id: String,
        val levelReq: Int,
        val enemyTypes: List<SpawnSystem.EnemyType>,
        val damageScale: Float
    )

    interface Listener {
        fun onMapSelected(index: Int)
    }

    var listener: Listener? = null
    var visible = false
    var selectedIndex = 0

    fun toggle() { visible = !visible }
    fun hide() { visible = false }

    fun navigate(delta: Int, total: Int) {
        if (!visible || total <= 0) return
        selectedIndex = (selectedIndex + delta + total) % total
    }

    fun draw(canvas: Canvas, maps: List<Entry>, playerLevel: Int, highestClearedIndex: Int, currentMapIndex: Int) {
        if (!visible) return

        // Layout sizing (responsive to screen width)
        val screenW = canvas.width.toFloat()
        val screenH = canvas.height.toFloat()
        val w = (screenW * 0.72f).coerceAtMost(980f)
        val left = (screenW - w) / 2f
        val top = 64f + uiInset
        val headerH = 56f
        val rowH = 44f
        val footerH = 38f
        val innerPad = 16f
        val contentH = headerH + (maps.size * rowH) + footerH + innerPad * 2
        val h = contentH

        val outer = RectF(left, top, left + w, top + h)
        val inner = RectF(outer.left + 6f, outer.top + 6f, outer.right - 6f, outer.bottom - 6f)

        // Backdrop: layered rounded rectangles with soft glow
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
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = grad
        }
        canvas.drawRoundRect(inner, 18f, 18f, innerPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = Color.argb(180, 230, 230, 235)
        }
        canvas.drawRoundRect(inner, 18f, 18f, borderPaint)

        // Header ribbon bar
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
        hudPaint.textSize = 46f
        hudPaint.isFakeBoldText = true
        hudPaint.color = Color.WHITE
        canvas.drawText("CHỌN MAP", headerRect.centerX(), headerRect.centerY() + 9f, hudPaint)

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
        val textPaint = hudPaint
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 36f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.WHITE

        maps.forEachIndexed { idx, m ->
            val rowRect = RectF(listLeft, rowTop, listRight, rowTop + rowH)
            if (idx % 2 == 0) canvas.drawRoundRect(rowRect, 10f, 10f, stripePaint)
            if (idx == selectedIndex) {
                canvas.drawRoundRect(rowRect, 12f, 12f, selectPaint)
                canvas.drawRoundRect(rowRect, 12f, 12f, selectStroke)
            }

            // Left title: MAP NAME
            val name = m.id.uppercase()
            val nameX = rowRect.left + 14f
            val baseY = rowRect.centerY() + 7f
            textPaint.textSize = 40f
            textPaint.color = Color.WHITE
            canvas.drawText(name, nameX, baseY, textPaint)

            // Middle: Level req
            textPaint.textSize = 32f
            textPaint.color = Color.argb(220, 180, 200, 255)
            canvas.drawText("L${m.levelReq}", nameX + 170f, baseY, textPaint)

            // Right: types and status chip
            val types = if (m.enemyTypes.isEmpty()) "He He He" else m.enemyTypes.joinToString("/") { it.name.substring(0,3) }
            textPaint.color = Color.argb(220, 210, 220, 230)
            canvas.drawText(types, nameX + 220f, baseY, textPaint)

            val lockedByLevel = playerLevel < m.levelReq
            val lockedByGate = idx > (highestClearedIndex + 1)
            val (chipText, chipColor) = when {
                idx == currentMapIndex -> "CURRENT" to Color.argb(220, 90, 170, 255)
                idx <= highestClearedIndex -> "CLEAR" to Color.argb(220, 88, 186, 120)
                lockedByLevel -> ("LOCK LV${m.levelReq}") to Color.argb(220, 236, 80, 80)
                lockedByGate -> "LOCK PROG" to Color.argb(220, 240, 170, 64)
                else -> "NEXT" to Color.argb(220, 255, 214, 64)
            }
            val chipW = 66f
            val chipH = 26f
            val chipRect = RectF(rowRect.right - chipW, rowRect.centerY() - chipH/2f, rowRect.right, rowRect.centerY() + chipH/2f)
            val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipColor }
            canvas.drawRoundRect(chipRect, 12f, 12f, chipBg)
            val chipStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
                color = Color.argb(230, 255, 255, 255)
            }
            canvas.drawRoundRect(chipRect, 12f, 12f, chipStroke)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 20f
            textPaint.color = Color.WHITE
            canvas.drawText(chipText, chipRect.centerX(), chipRect.centerY() + 5f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            rowTop += rowH + 6f
        }

        // Restore HUD paint state
        hudPaint.textAlign = prevAlign
        hudPaint.textSize = prevSize
        hudPaint.isFakeBoldText = prevBold
        hudPaint.color = prevColor
    }

    fun confirm(total: Int) {
        if (!visible || total == 0) return
        listener?.onMapSelected(selectedIndex)
    }
}
