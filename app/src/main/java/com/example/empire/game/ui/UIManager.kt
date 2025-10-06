package com.example.empire.game.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.empire.game.army.UnitType
import com.example.empire.game.army.Unlocks
import com.example.empire.game.economy.CostTable
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.ecs.systems.SpawnSystem

/** Consolidated UI manager to reduce GameView size. */
class UIManager(
    private val panelPaint: Paint,
    private val panelBorder: Paint,
    private val highlightPaint: Paint,
    private val hudPaint: Paint,
    private val uiInset: Float
) {
    // States
    var showHousePrompt = false
    var showBuyMenu = false
    var showGameOver = false
    var showMapSelect = false
    var mapSelectIndex = 0
    var buySelection = 0

    interface Callbacks {
        fun onBuy(type: UnitType)
        fun onMapChosen(index: Int)
        fun onRespawn()
    }
    var callbacks: Callbacks? = null

    fun resetSession() {
        showHousePrompt = false
        showBuyMenu = false
        showGameOver = false
        showMapSelect = false
        buySelection = 0
    }

    // ---------- Input helpers ----------
    fun pressA(mapsSize: Int, unlocked: (Int)->Boolean) {
        when {
            showGameOver -> callbacks?.onRespawn()
            showBuyMenu -> callbacks?.onBuy(currentBuyType())
            showHousePrompt -> { showHousePrompt = false; showBuyMenu = true }
            showMapSelect -> {
                if (unlocked(mapSelectIndex)) callbacks?.onMapChosen(mapSelectIndex)
            }
        }
    }
    fun pressB() {
        when {
            showGameOver -> {}
            showBuyMenu -> showBuyMenu = false
            showHousePrompt -> showHousePrompt = false
            showMapSelect -> showMapSelect = false
        }
    }
    fun navigate(dy: Int, mapsSize: Int) {
        if (showBuyMenu) {
            val max = 3
            buySelection = (buySelection + dy + (max+1)) % (max+1)
        } else if (showMapSelect) {
            if (mapsSize > 0) {
                mapSelectIndex = (mapSelectIndex + dy + mapsSize) % mapsSize
            }
        }
    }
    private fun currentBuyType(): UnitType = when(buySelection){
        0 -> UnitType.WARRIOR
        1 -> UnitType.LANCER
        2 -> UnitType.ARCHER
        else -> UnitType.MONK
    }

    // ---------- Draw ----------
    fun drawHousePrompt(canvas: Canvas) {
        if (!showHousePrompt) return
        val w = canvas.width * 0.6f
        val h = 100f
        val left = (canvas.width - w)/2f
        val top = canvas.height - h - 40f - uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 12f, 12f, panelPaint)
        canvas.drawRoundRect(rect, 12f, 12f, panelBorder)
        hudPaint.textSize = 24f
        canvas.drawText("Vào nhà chính?", left + 20f, top + 40f, hudPaint)
        hudPaint.textSize = 16f
        canvas.drawText("A: Đồng ý  B: Hủy", left + 20f, top + 70f, hudPaint)
    }

    fun drawBuyMenu(canvas: Canvas, resources: ResourceManager, unlocks: Unlocks) {
        if (!showBuyMenu) return
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
        val entries = listOf(UnitType.WARRIOR, UnitType.LANCER, UnitType.ARCHER, UnitType.MONK)
        val lineH = 42f
        entries.forEachIndexed { idx, type ->
            val y = top + 60f + idx * lineH
            if (idx == buySelection) canvas.drawRect(left + 12f, y - 24f, left + w - 12f, y + 10f, highlightPaint)
            val cost = CostTable.get(type)
            val unlockedFlag = when(type){
                UnitType.LANCER -> unlocks.lancerUnlocked
                UnitType.ARCHER -> unlocks.archerUnlocked
                UnitType.MONK -> unlocks.monkUnlocked
                UnitType.WARRIOR -> true
            }
            val affordable = resources.gold >= cost.gold && resources.meat >= cost.meat
            val status = if (!unlockedFlag) "LOCK" else if (!affordable) "NO$" else "OK"
            canvas.drawText("${type.name.lowercase().replaceFirstChar{it.uppercase()}}  G:${cost.gold} M:${cost.meat} [$status]", left + 24f, y, hudPaint)
        }
        hudPaint.textSize = 12f
        canvas.drawText("A: Mua  B: Thoát  ↑↓: Chọn", left + 20f, top + h - 20f, hudPaint)
    }

    fun drawGameOver(canvas: Canvas) {
        if (!showGameOver) return
        val w = canvas.width * 0.6f
        val h = 240f
        val left = (canvas.width - w)/2f
        val top = (canvas.height - h)/2f - uiInset
        val rect = RectF(left, top, left + w, top + h)
        canvas.drawRoundRect(rect, 18f, 18f, panelPaint)
        canvas.drawRoundRect(rect, 18f, 18f, panelBorder)
        hudPaint.textSize = 36f
        canvas.drawText("GAME OVER", left + 40f, top + 70f, hudPaint)
        hudPaint.textSize = 16f
        canvas.drawText("A: Respawn", left + 40f, top + 120f, hudPaint)
        hudPaint.textSize = 14f
    }

    data class MapEntry(val id: String, val levelReq: Int, val enemyTypes: List<SpawnSystem.EnemyType>)
    fun drawMapSelect(canvas: Canvas, entries: List<MapEntry>, playerLevel: Int, unlocked: (Int)->Boolean) {
        if (!showMapSelect) return
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
        entries.forEachIndexed { idx, e ->
            val y = listStart + idx * lineH
            val locked = !unlocked(idx)
            if (idx == mapSelectIndex) canvas.drawRect(left + 14f, y - 20f, left + w - 14f, y + 6f, highlightPaint)
            val types = if (e.enemyTypes.isEmpty()) "No quái" else e.enemyTypes.joinToString("/") { it.name.substring(0,3) }
            val status = if (locked) "LOCK" else "OK"
            canvas.drawText("${e.id.uppercase()}  L${e.levelReq}  $status  $types", left + 26f, y, hudPaint)
        }
        hudPaint.textSize = 12f
        canvas.drawText("A: Chọn  B: Đóng  ↑↓: Di chuyển", left + 26f, top + h - 32f, hudPaint)
        canvas.drawText("Level: $playerLevel", left + 26f, top + h - 14f, hudPaint)
    }
}
