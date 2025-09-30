package com.example.empire.game.ecs.systems

/**
 * Quản lý spawn entities (địch, item…). Tối giản cho m dễ cắm AI sau.
 */
class SpawnSystem {

    enum class EnemyType { ARCHER, WARRIOR, PAWN }

    data class Enemy(
        var x: Float,
        var y: Float,
        val w: Float = 28f,
        val h: Float = 40f,
        val type: EnemyType,
        var hp: Int = 3,
        var alive: Boolean = true,
        var vx: Float = 0f,
        var vy: Float = 0f
    )

    private val _enemies = mutableListOf<Enemy>()
    val enemies: List<Enemy> get() = _enemies

    fun clear() = _enemies.clear()

    fun spawnEnemy(x: Float, y: Float, type: EnemyType) {
        _enemies += Enemy(x, y, type = type)
    }

    fun update(dt: Float) {
        // để trống: AI/di chuyển sẽ gắn sau (EnemyBehaviors)
        // tạm thời giữ nguyên vị trí
    }

    fun removeDead() {
        _enemies.removeAll { !it.alive || it.hp <= 0 }
    }
}
