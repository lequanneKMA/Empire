package com.example.empire.game.economy

/**
 * Quản lý tài nguyên Gold / Meat.
 * Thread-safety: không cần đồng bộ phức tạp ở MVP.
 */
class ResourceManager(
    startGold: Int = 20,
    startMeat: Int = 4
) {
    var gold: Int = startGold; private set
    var meat: Int = startMeat; private set

    fun addGold(v: Int) { if (v>0) gold += v }
    fun addMeat(v: Int) { if (v>0) meat += v }

    fun canAfford(cost: Cost): Boolean = gold >= cost.gold && meat >= cost.meat
    fun spend(cost: Cost): Boolean {
        if (!canAfford(cost)) return false
        gold -= cost.gold
        meat -= cost.meat
        return true
    }
}
