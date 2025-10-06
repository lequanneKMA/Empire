package com.example.empire.game.army

/**
 * Skeleton quản lý projectile (sẽ dùng cho Archer sau).
 */
class ProjectileSystem {
    data class Projectile(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var damage: Int,
        var alive: Boolean = true
    )

    private val _projectiles = mutableListOf<Projectile>()
    val projectiles: List<Projectile> get() = _projectiles

    fun spawn(x: Float, y: Float, vx: Float, vy: Float, damage: Int) {
        _projectiles += Projectile(x,y,vx,vy,damage)
    }

    fun update(dt: Float) {
        if (_projectiles.isEmpty()) return
        val it = _projectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            // TODO: collision với enemy / map
            if (!p.alive) it.remove()
        }
    }
}
