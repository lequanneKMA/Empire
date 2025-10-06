package com.example.empire.game.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.example.empire.game.player.PlayerStats
import com.example.empire.game.progression.ProgressionManager
import com.example.empire.game.army.ArmySystem
import com.example.empire.game.economy.ResourceManager

/**
 * Draws the on-screen HUD (HP, XP, resources, army counts, map name etc.).
 * Extracted from GameView to keep that file smaller.
 */
class HudRenderer(
    private val hudPaint: Paint,
    private val hpBarPaint: Paint,
    private val hpBarBackPaint: Paint,
    private val xpBarPaint: Paint,
    private val xpBarBackPaint: Paint,
    private val uiInset: Float
) {
    fun draw(
        canvas: Canvas,
        playerStats: PlayerStats,
        progression: ProgressionManager,
        resources: ResourceManager,
        armySystem: ArmySystem,
        currentMapId: String,
        unlocks: Any, // kept generic; caller can format unlock text before refactor
        unlockText: String
    ) {
        val barW = 140f
        val barH = 10f
        val margin = 8f
        val hpX = margin + uiInset
        val hpY = margin + uiInset
        val hpPerc = playerStats.hp / playerStats.maxHp.toFloat()
        canvas.drawRect(hpX, hpY, hpX + barW, hpY + barH, hpBarBackPaint)
        canvas.drawRect(hpX, hpY, hpX + barW * hpPerc, hpY + barH, hpBarPaint)
        canvas.drawText("HP ${playerStats.hp}/${playerStats.maxHp}", hpX, hpY + barH + 14f, hudPaint)

        val xpTop = hpY + barH + 28f
        val xpNeeded = progression.currentThreshold
        val xpPerc = if (xpNeeded > 0) (progression.xp.coerceAtMost(xpNeeded) / xpNeeded.toFloat()) else 1f
        canvas.drawRect(hpX, xpTop, hpX + barW, xpTop + barH, xpBarBackPaint)
        canvas.drawRect(hpX, xpTop, hpX + barW * xpPerc, xpTop + barH, xpBarPaint)
        canvas.drawText("XP ${progression.xp}/${xpNeeded} Tier=${progression.tier}", hpX, xpTop + barH + 14f, hudPaint)

        val resY = xpTop + barH + 34f
        canvas.drawText("Gold: ${resources.gold} Meat: ${resources.meat}", hpX, resY, hudPaint)
        val counts = armySystem.units.groupingBy { it.type }.eachCount()
        canvas.drawText("W:${counts[com.example.empire.game.army.UnitType.WARRIOR]?:0} L:${counts[com.example.empire.game.army.UnitType.LANCER]?:0} A:${counts[com.example.empire.game.army.UnitType.ARCHER]?:0} M:${counts[com.example.empire.game.army.UnitType.MONK]?:0}", hpX, resY + 16f, hudPaint)
        canvas.drawText(unlockText, hpX, resY + 32f, hudPaint)
        canvas.drawText("Map: $currentMapId", hpX, resY + 48f, hudPaint)
    }
}
