package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

    fun draw(canvas: Canvas, maps: List<Entry>, playerLevel: Int) {
        if (!visible) return
        val w = canvas.width * 0.52f
        val h = 250f
        val left = (canvas.width - w)/2f
        val top = 70f + uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 18f, 18f, panelPaint)
        canvas.drawRoundRect(rect, 18f, 18f, panelBorder)
        hudPaint.textSize = 22f
        canvas.drawText("CHỌN MAP", left + 22f, top + 40f, hudPaint)
        hudPaint.textSize = 14f
        val listStart = top + 66f
        val lineH = 30f
        maps.forEachIndexed { idx, m ->
            val y = listStart + idx * lineH
            val locked = playerLevel < m.levelReq
            if (idx == selectedIndex) canvas.drawRect(left + 14f, y - 20f, left + w - 14f, y + 6f, highlightPaint)
            val types = if (m.enemyTypes.isEmpty()) "No quái" else m.enemyTypes.joinToString("/") { it.name.substring(0,3) }
            val status = if (locked) "LOCK" else "OK"
            canvas.drawText("${m.id.uppercase()}  L${m.levelReq}  ${status}  ${types}", left + 26f, y, hudPaint)
        }
        hudPaint.textSize = 12f
        canvas.drawText("A: Chọn  B: Đóng  ↑↓: Di chuyển", left + 26f, top + h - 32f, hudPaint)
        canvas.drawText("Level: $playerLevel", left + 26f, top + h - 14f, hudPaint)
    }

    fun confirm(total: Int) {
        if (!visible || total == 0) return
        listener?.onMapSelected(selectedIndex)
    }
}
