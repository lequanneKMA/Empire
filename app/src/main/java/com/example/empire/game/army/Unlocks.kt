package com.example.empire.game.army

/**
 * Theo dõi điều kiện mở khoá unit.
 */
class Unlocks {
    var warriorsBought = 0
    var lancerUnlocked = false
    var archerUnlocked = false
    var monkUnlocked = false

    fun evaluate(totalXp: Int, attackTier: Int) {
        if (warriorsBought >= 2) lancerUnlocked = true
        if (attackTier >= 1) archerUnlocked = true
        if (totalXp >= 100) monkUnlocked = true
    }
}
