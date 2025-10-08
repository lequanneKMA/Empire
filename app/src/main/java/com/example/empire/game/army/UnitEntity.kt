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
     var auxTimer: Float = 0f,
     // Pathfinding support (now handled by ArmyAIController)
     var path: MutableList<Pair<Int, Int>>? = null,
     var pathIndex: Int = 0,
     var pathGoalTx: Int = -1,
     var pathGoalTy: Int = -1,
     var lastPosX: Float = x,
     var lastPosY: Float = y
 ) {
     fun clearPath() { path = null; pathIndex = 0; pathGoalTx = -1; pathGoalTy = -1 }
     fun moveToward(tx: Float, ty: Float) {
         val dx = tx - x
         val dy = ty - y
         val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
         if (dist <= 0.0001f) return
         val spd = stats.moveSpeed / 60f // assume update() called once per frame with dt ~ 1/60 for AI usage
         x += (dx / dist) * spd
         y += (dy / dist) * spd
     }

    // Hysteresis state to tránh giật do vào/ra vùng tấn công mỗi frame
    var isInAttackRange: Boolean = false
    // Velocity (optional dùng cho future smoothing / facing)
    var vx: Float = 0f
    var vy: Float = 0f
    var facing: Facing = Facing.RIGHT
    var attackVariantIndex: Int = 0
    var attackAnimTimer: Float = 0f
 }

 enum class UnitState { FOLLOW, COMBAT }
 enum class AnimState { IDLE, MOVE, ATTACK, CAST }
enum class Facing { LEFT, RIGHT }
