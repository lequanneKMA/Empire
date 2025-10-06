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
    var attackRange = 56f           // sẽ được set theo tier mỗi lần startAttack
    private var cdTimer = 0f

    // Tier-based config (có thể chỉnh từ ngoài)
    // Nerf damage & tweak progression: base=1, tier1=2, tier2=3 (có thể mở rộng)
    // Nerf damage: slower growth to avoid steamroll
    var damagePerTier = intArrayOf(1, 2, 2, 3)
    var cooldownPerTier = floatArrayOf(0.60f, 0.54f, 0.50f, 0.45f)
    var rangePerTier = floatArrayOf(56f, 60f, 64f, 70f)

    // Provider lấy tier hiện tại của player
    var tierProvider: () -> Int = { 0 }

    // Callback khi enemy chết
    var onEnemyKilled: (SpawnSystem.EnemyType) -> Unit = {}

    // damage active của cú đánh hiện tại
    private var activeDamage = 1

    /** Gọi khi nhấn tấn công */
    fun startAttack() {
        if (cdTimer <= 0f && attackTimer <= 0f) {
            val t = tierProvider().coerceAtLeast(0)
            val idx = t.coerceAtMost(damagePerTier.lastIndex)
            activeDamage = damagePerTier[idx]
            attackRange = rangePerTier.getOrElse(idx) { attackRange }
            val cd = cooldownPerTier.getOrElse(idx) { 0.5f }
            attackTimer = attackDuration
            cdTimer = cd
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
                if (!e.alive || e.state == SpawnSystem.Enemy.State.DEAD) return@forEach
                val ex = e.x + e.w / 2f
                val ey = e.y + e.h / 2f
                val d = hypot((ex - cx).toDouble(), (ey - cy).toDouble()).toFloat()
                if (d <= attackRange) {
                    // direction for knockback
                    var dx = ex - cx
                    var dy = ey - cy
                    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (len > 0.0001f) { dx /= len; dy /= len } else { dx = 0f; dy = -1f }
                    // Apply damage first
                    val killed = spawnSystem.applyDamage(e, activeDamage)
                    if (killed) {
                        onEnemyKilled(e.type)
                    } else {
                        // Apply light knockback (stagger feel)
                        spawnSystem.applyKnockback(e, dx, dy, 140f)
                    }
                }
            }
        }
        spawnSystem.removeDead()
    }
}
