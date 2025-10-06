package com.example.empire.game.economy

import com.example.empire.game.army.UnitType

/** Chi phí mua 1 unit. */
 data class Cost(val gold: Int, val meat: Int)

object CostTable {
    private val map = mapOf(
        UnitType.WARRIOR to Cost(20,1),
        UnitType.LANCER  to Cost(35,2),
        UnitType.ARCHER  to Cost(45,2),
        UnitType.MONK    to Cost(40,3)
    )
    fun get(type: UnitType): Cost = map.getValue(type)
}
