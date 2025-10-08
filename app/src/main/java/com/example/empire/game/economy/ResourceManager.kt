package com.example.empire.game.economy

/**
 * Quản lý tài nguyên Gold / Meat.
 * Thread-safety: không cần đồng bộ phức tạp ở MVP.
 */
class ResourceManager(
    startGold: Int = DEFAULT_START_GOLD,
    startMeat: Int = DEFAULT_START_MEAT
) {
    var gold: Int = startGold; public set
    var meat: Int = startMeat; public set

    fun addGold(v: Int) { if (v>0) gold += v }
    fun addMeat(v: Int) { if (v>0) meat += v }

    fun canAfford(cost: Cost): Boolean = gold >= cost.gold && meat >= cost.meat
    fun spend(cost: Cost): Boolean {
        if (!canAfford(cost)) return false
        gold -= cost.gold
        meat -= cost.meat
        return true
    }

    companion object {
        // Central config for starting resources. Change here -> affects all new games (unless explicitly overridden).
        const val DEFAULT_START_GOLD = 2000
        const val DEFAULT_START_MEAT = 400
    }
}
