package com.example.empire.game.progression

/**
 * Quản lý XP & tier nâng cấp tấn công.
 * Phần 1: skeleton + logic cơ bản addXp / check upgrade.
 */
class ProgressionManager(
    private val thresholds: IntArray = intArrayOf(50) // chỉ 1 mốc cho Attack2 demo
) {
    var xp: Int = 0
        private set
    var tier: Int = 0
        private set

    val currentThreshold: Int
        get() = thresholds.getOrNull(tier) ?: thresholds.lastOrNull() ?: 0

    /**
     * Thêm XP, trả về true nếu vừa nâng cấp tier.
     */
    fun addXp(amount: Int): Boolean {
        if (amount <= 0) return false
        xp += amount
        var upgraded = false
        while (tier < thresholds.size && xp >= thresholds[tier]) {
            tier++
            upgraded = true
        }
        return upgraded
    }

    fun reset() {
        xp = 0
        tier = 0
    }
}
