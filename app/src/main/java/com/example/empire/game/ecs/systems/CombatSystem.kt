package com.example.empire.game.ecs.systems

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Combat cơ bản: player tấn công “quét” vòng gần kề.
 * Hook để sau này gắn ProjectileGun, AoE…
 */
class CombatSystem(
    private val spawnSystem: SpawnSystem
) {
    var attackTimer = 0f
    var attackDuration = 0.35f
    var attackRange = 56f           // tầm chém/melee
    var attackCooldown = 0.35f
    private var cdTimer = 0f

    /** Gọi khi nhấn tấn công */
    fun startAttack() {
        if (cdTimer <= 0f && attackTimer <= 0f) {
            attackTimer = attackDuration
            cdTimer = attackCooldown
        }
    }

    /**
     * Update mỗi frame.
     * Player hitbox = (px,py,w,h). Khi đang attack thì gây dmg cho enemy gần nhất trong range.
     */
    fun update(dt: Float, px: Float, py: Float, w: Float, h: Float) {
        if (cdTimer > 0f) cdTimer -= dt
        if (attackTimer > 0f) {
            attackTimer -= dt

            // simple melee hit: tâm player vs tâm enemy
            val cx = px + w / 2f
            val cy = py + h / 2f

            spawnSystem.enemies.forEach { e ->
                if (!e.alive) return@forEach
                val ex = e.x + e.w / 2f
                val ey = e.y + e.h / 2f
                val d = hypot((ex - cx).toDouble(), (ey - cy).toDouble()).toFloat()
                if (d <= attackRange) {
                    e.hp -= 1
                    if (e.hp <= 0) e.alive = false
                }
            }
        }

        spawnSystem.removeDead()
    }
}
