package com.example.empire.game.player

/**
 * Thống kê cơ bản của player: HP, maxHP. (Part 1 skeleton)
 */
class PlayerStats(
    var maxHp: Int = 10
) {
    var hp: Int = maxHp
        private set

    fun heal(amount: Int) {
        if (amount <= 0) return
        hp = (hp + amount).coerceAtMost(maxHp)
    }

    fun damage(amount: Int) {
        if (amount <= 0) return
        hp -= amount
        if (hp < 0) hp = 0
    }

    val isDead: Boolean get() = hp <= 0

    fun reset(fullRestore: Boolean = true) {
        if (fullRestore) hp = maxHp
    }
}
