package com.example.empire.game.army

import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.economy.CostTable

/**
 * Shop wrapper – kiểm tra unlock & cost rồi gọi ArmySystem.
 */
class ShopSystem(
    private val army: ArmySystem,
    private val unlocks: Unlocks,
    private val resources: ResourceManager
) {
    fun canBuy(type: UnitType): Boolean = when(type) {
        UnitType.LANCER -> unlocks.lancerUnlocked
        UnitType.ARCHER -> unlocks.archerUnlocked
        UnitType.MONK   -> unlocks.monkUnlocked
        UnitType.WARRIOR -> true
    }

    fun affordable(type: UnitType): Boolean {
        val c = CostTable.get(type)
        return resources.gold >= c.gold && resources.meat >= c.meat
    }

    fun buy(type: UnitType, playerX: Float, playerY: Float): Boolean {
        if (!canBuy(type)) return false
        if (!affordable(type)) return false
        val ok = army.buy(type, playerX, playerY)
        if (ok && type == UnitType.WARRIOR) {
            unlocks.warriorsBought++
        }
        return ok
    }
}
