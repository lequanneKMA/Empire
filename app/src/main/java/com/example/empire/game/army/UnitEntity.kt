package com.example.empire.game.army

/**
 * Entity runtime cho unit.
 * Không dùng ECS phức tạp – chỉ data holder.
 */
 data class UnitEntity(
     var x: Float,
     var y: Float,
     val type: UnitType,
     val stats: UnitStats,
     var hp: Int = stats.maxHp,
     var state: UnitState = UnitState.FOLLOW,
     var cooldown: Float = 0f,
     var targetEnemyIndex: Int? = null,
     var anim: AnimState = AnimState.IDLE,
     // Extra timers (Monk heal, v.v.)
     var auxTimer: Float = 0f
 )

 enum class UnitState { FOLLOW, COMBAT }
 enum class AnimState { IDLE, MOVE, ATTACK, CAST }
