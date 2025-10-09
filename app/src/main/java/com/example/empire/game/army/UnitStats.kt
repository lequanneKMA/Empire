package com.example.empire.game.army

/**
 * Thông số tĩnh cho từng UnitType.
 * Có thể cân bằng lại về sau – số đang để gần với đề xuất.
 */
 data class UnitStats(
     val maxHp: Int,
     val attack: Int,
     val moveSpeed: Float,
     val range: Float,
     val cooldown: Float,
     val defense: Int = 0,
     val isRanged: Boolean = false
 )

object UnitStatTable {
    private val table: Map<UnitType, UnitStats> = mapOf(
    // Cooldown đã làm chậm lại ~30-40% so với trước để nhịp tấn công “nặng” hơn
    UnitType.WARRIOR to UnitStats(maxHp = 70, attack = 5,  moveSpeed = 80f, range = 28f,  cooldown = 1.25f, defense = 2),
    UnitType.LANCER  to UnitStats(maxHp = 60, attack = 7,  moveSpeed = 90f, range = 36f,  cooldown = 1.65f, defense = 1),
    UnitType.ARCHER  to UnitStats(maxHp = 55, attack = 6,  moveSpeed = 85f, range = 100f, cooldown = 1.45f, defense = 0, isRanged = true),
    UnitType.MONK    to UnitStats(maxHp = 45, attack = 3,  moveSpeed = 78f, range = 20f,  cooldown = 1.9f, defense = 1)
    )

    fun get(type: UnitType): UnitStats = table.getValue(type)
}
