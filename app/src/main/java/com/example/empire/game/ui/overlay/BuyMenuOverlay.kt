package com.example.empire.game.ui.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
        val w = canvas.width * 0.5f
        val h = 260f
        val left = (canvas.width - w)/2f
        val top = (canvas.height - h)/2f - uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 14f, 14f, panelPaint)
        canvas.drawRoundRect(rect, 14f, 14f, panelBorder)
        hudPaint.textSize = 20f
        canvas.drawText("MUA QUÂN", left + 20f, top + 34f, hudPaint)
        hudPaint.textSize = 14f
        val lineH = 42f
        entries.forEachIndexed { idx, type ->
            val y = top + 60f + idx * lineH
            if (idx == selection) canvas.drawRect(left + 12f, y - 24f, left + w - 12f, y + 10f, highlightPaint)
            val cost = CostTable.get(type)
            val unlocked = when(type) {
                UnitType.LANCER -> unlocks.lancerUnlocked
                UnitType.ARCHER -> unlocks.archerUnlocked
                UnitType.MONK -> unlocks.monkUnlocked
                UnitType.WARRIOR -> true
            }
            val affordable = resources.gold >= cost.gold && resources.meat >= cost.meat
            val status = if (!unlocked) "LOCK" else if (!affordable) "NO$" else "OK"
            canvas.drawText("${type.name.lowercase().replaceFirstChar{it.uppercase()}}  G:${cost.gold} M:${cost.meat} [$status]", left + 24f, y, hudPaint)
        }
        hudPaint.textSize = 12f
        canvas.drawText("A: Mua  B: Thoát  ↑↓: Chọn", left + 20f, top + h - 20f, hudPaint)
        hudPaint.textSize = 14f
    }

    fun confirm(resources: ResourceManager, unlocks: Unlocks) {
        if (!visible) return
        val type = entries[selection]
        listener?.onBuy(type)
    }
}
