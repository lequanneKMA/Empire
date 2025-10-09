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
    var totalXp: Int = 0
        private set

    val currentThreshold: Int
        get() = thresholds.getOrNull(tier) ?: thresholds.lastOrNull() ?: 0

    private fun thresholdForTier(t: Int): Int {
        return thresholds.getOrNull(t) ?: thresholds.lastOrNull() ?: 0
    }

    /** XP hiện tại trong cấp (đã trừ các ngưỡng cũ). */
    val currentLevelXp: Int
        get() = xp

    /** Ngưỡng cần cho cấp hiện tại. */
    val nextLevelThreshold: Int
        get() = currentThreshold

    /**
     * Thêm XP, trả về true nếu vừa nâng cấp tier.
     */
    fun addXp(amount: Int): Boolean {
        if (amount <= 0) return false
        xp += amount
        totalXp += amount
        var upgraded = false
        var guard = 0
        while (true) {
            val need = thresholdForTier(tier)
            if (need <= 0) break // tránh vòng lặp vô hạn nếu cấu hình sai
            if (xp < need) break
            xp -= need // reset XP về phần dư khi lên cấp
            tier++
            upgraded = true
            guard++
            if (guard > 100) break // an toàn
        }
        return upgraded
    }

    fun reset() {
        xp = 0
        totalXp = 0
        tier = 0
    }
}
