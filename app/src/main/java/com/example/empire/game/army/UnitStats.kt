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
     val defense: Int = 0,        // giảm damage nhận vào (đơn giản: dmgTaken = max(1, raw - defense))
     val isRanged: Boolean = false
 )

object UnitStatTable {
    private val table: Map<UnitType, UnitStats> = mapOf(
    UnitType.WARRIOR to UnitStats(maxHp = 180, attack = 6,  moveSpeed = 80f, range = 48f,  cooldown = 0.9f, defense = 2),
    UnitType.LANCER  to UnitStats(maxHp = 140, attack = 9,  moveSpeed = 90f, range = 72f,  cooldown = 1.3f, defense = 1),
    UnitType.ARCHER  to UnitStats(maxHp = 110, attack = 7,  moveSpeed = 85f, range = 200f, cooldown = 1.1f, defense = 0, isRanged = true),
    UnitType.MONK    to UnitStats(maxHp = 120, attack = 2,  moveSpeed = 78f, range = 40f,  cooldown = 1.5f, defense = 1)
    )

    fun get(type: UnitType): UnitStats = table.getValue(type)
}
