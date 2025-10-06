package com.example.empire.game.army

import com.example.empire.game.economy.CostTable
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.ecs.systems.SpawnSystem
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quản lý danh sách unit của player.
 * Bổ sung: targeting + melee combat đơn giản cho WARRIOR & LANCER (v1).
 * Archer/Monk sẽ xử lý ở bước sau.
 */
class ArmySystem(
    private val resource: ResourceManager,
    private val spawnSystem: SpawnSystem, // để truy cập enemies
    private val projectileSystem: ProjectileSystem // bắn tên cho Archer
) {
    private val _units = mutableListOf<UnitEntity>()
    val units: List<UnitEntity> get() = _units

    fun clear() { _units.clear() }

    var maxUnits = 20

    /** Callback khi spawn (để GameView có thể play SFX, v.v.) */
    var onUnitSpawned: (UnitEntity) -> Unit = {}

    /** Mua và spawn quanh player. */
    fun buy(type: UnitType, playerX: Float, playerY: Float): Boolean {
        if (_units.size >= maxUnits) return false
        val cost = CostTable.get(type)
        if (!resource.spend(cost)) return false
        spawnUnit(type, playerX, playerY)
        return true
    }

    private fun spawnUnit(type: UnitType, px: Float, py: Float) {
        val stats = UnitStatTable.get(type)
        // vòng tròn spawn đơn giản dựa vào số lượng
        val idx = _units.size
        val angle = (idx % 8) * (Math.PI / 4.0) // 8 hướng; nếu >8 thì chồng vòng
        val radius = 64f + (idx / 8) * 16f
        val ux = px + cos(angle).toFloat() * radius
        val uy = py + sin(angle).toFloat() * radius
        val unit = UnitEntity(ux, uy, type, stats)
        _units += unit
        onUnitSpawned(unit)
    }

    /** Update: follow + combat cho melee units. */
    fun update(dt: Float, playerX: Float, playerY: Float) {
        if (_units.isEmpty()) return

        // Giảm spam target: accumulator nhỏ
        targetAccum -= dt
        val refreshTarget = targetAccum <= 0f
        if (refreshTarget) targetAccum = targetRefreshInterval

        val enemies = spawnSystem.enemies
        val total = _units.size
        _units.forEachIndexed { i, u ->
            if (u.hp <= 0) return@forEachIndexed
            // Cooldown giảm
            if (u.cooldown > 0f) u.cooldown -= dt

            when (u.type) {
                UnitType.WARRIOR, UnitType.LANCER -> updateMelee(u, dt, playerX, playerY, i, total, enemies, refreshTarget)
                UnitType.ARCHER -> updateRanged(u, dt, playerX, playerY, i, total, enemies, refreshTarget)
                else -> {
                    // Tạm thời những unit khác vẫn chỉ follow
                    formationFollow(u, dt, playerX, playerY, i, total)
                }
            }
        }
    }

    private var targetAccum = 0f
    private val targetRefreshInterval = 0.18f

    private fun updateMelee(
        u: UnitEntity,
        dt: Float,
        playerX: Float,
        playerY: Float,
        idx: Int,
        total: Int,
        enemies: List<SpawnSystem.Enemy>,
        refreshTarget: Boolean
    ) {
        val target = acquireOrValidateTarget(u, enemies, refreshTarget)
        if (target == null) {
            // Không có mục tiêu => follow formation
            formationFollow(u, dt, playerX, playerY, idx, total)
            return
        }
        // Di chuyển / Attack
        val cx = u.x
        val cy = u.y
        val ex = target.x + target.w/2f
        val ey = target.y + target.h/2f
        val dx = ex - cx
        val dy = ey - cy
        val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val reach = u.stats.range
        if (dist > reach * 0.85f) {
            // tiến lại gần
            val speed = u.stats.moveSpeed
            if (dist > 2f) {
                u.x += (dx / dist) * speed * dt
                u.y += (dy / dist) * speed * dt
                u.anim = AnimState.MOVE
            }
        } else {
            // trong tầm – tấn công nếu cooldown xong
            if (u.cooldown <= 0f) {
                performMeleeAttack(u, target)
            } else {
                u.anim = AnimState.IDLE
            }
        }
    }

    private fun acquireOrValidateTarget(
        u: UnitEntity,
        enemies: List<SpawnSystem.Enemy>,
        refresh: Boolean
    ): SpawnSystem.Enemy? {
        val idx = u.targetEnemyIndex
        var current: SpawnSystem.Enemy? = null
        if (idx != null && idx in enemies.indices) {
            val e = enemies[idx]
            if (e.alive && e.hp > 0) current = e else u.targetEnemyIndex = null
        }
        if (current != null && !refresh) return current
        // tìm mới
        var best: SpawnSystem.Enemy? = null
        var bestD2 = Float.MAX_VALUE
        val ux = u.x; val uy = u.y
        val range = u.stats.range + 96f // tìm trong vùng lớn hơn 1 chút
        val r2 = range * range
        enemies.forEachIndexed { i, e ->
            if (!e.alive || e.hp <= 0) return@forEachIndexed
            val ex = e.x + e.w/2f
            val ey = e.y + e.h/2f
            val dx = ex - ux
            val dy = ey - uy
            val d2 = dx*dx + dy*dy
            if (d2 < r2 && d2 < bestD2) {
                bestD2 = d2
                best = e
                u.targetEnemyIndex = i
            }
        }
        return best
    }

    private fun performMeleeAttack(u: UnitEntity, enemy: SpawnSystem.Enemy) {
        var dmg = u.stats.attack
        if (u.type == UnitType.LANCER && enemy.hp >= enemy.hp /* placeholder for bonus condition */) {
            // Bonus Lancer lên mục tiêu full HP (ở đây đơn giản: nếu hp == max giả định 3 để test)
            // Vì Enemy chưa lưu maxHp, tạm thời không áp dụng điều kiện thực – có thể thêm field sau.
        }
        val killed = spawnSystem.applyDamage(enemy, dmg)
        u.cooldown = u.stats.cooldown
        u.anim = AnimState.ATTACK
    }

    // ================= Ranged (Archer) =================
    private fun updateRanged(
        u: UnitEntity,
        dt: Float,
        playerX: Float,
        playerY: Float,
        idx: Int,
        total: Int,
        enemies: List<SpawnSystem.Enemy>,
        refreshTarget: Boolean
    ) {
        val target = acquireOrValidateTarget(u, enemies, refreshTarget)
        if (target == null) {
            formationFollow(u, dt, playerX, playerY, idx, total)
            return
        }
        val cx = u.x
        val cy = u.y
        val ex = target.x + target.w/2f
        val ey = target.y + target.h/2f
        val dx = ex - cx
        val dy = ey - cy
        val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val range = u.stats.range
        if (dist > range * 0.95f) {
            // tiến gần đến tầm bắn (giữ khoảng cách – simple)
            val speed = u.stats.moveSpeed
            if (dist > range * 0.8f) { // chỉ tiến nếu còn khá xa
                u.x += (dx / dist) * speed * dt
                u.y += (dy / dist) * speed * dt
                u.anim = AnimState.MOVE
            } else {
                u.anim = AnimState.IDLE
            }
        } else {
            // trong tầm – bắn nếu cooldown xong
            if (u.cooldown <= 0f) {
                performRangedAttack(u, dx, dy, dist)
            } else {
                u.anim = AnimState.IDLE
            }
        }
    }

    private fun performRangedAttack(u: UnitEntity, dx: Float, dy: Float, dist: Float) {
        val speed = 300f
        val nx = if (dist > 0f) dx / dist else 0f
        val ny = if (dist > 0f) dy / dist else 0f
        // Spawn projectile từ vị trí unit hơi lệch lên 8px
        projectileSystem.spawn(
            x = u.x,
            y = u.y - 8f,
            vx = nx * speed,
            vy = ny * speed,
            damage = u.stats.attack
        )
        u.cooldown = u.stats.cooldown
        u.anim = AnimState.ATTACK
    }

    private fun formationFollow(u: UnitEntity, dt: Float, playerX: Float, playerY: Float, idx: Int, total: Int) {
        val angle = (idx.toFloat()/total) * (Math.PI * 2.0).toFloat()
        val radius = 80f
        val tx = playerX + cos(angle) * radius
        val ty = playerY + sin(angle) * radius
        steer(u, tx.toFloat(), ty.toFloat(), dt)
    }

    private fun steer(u: UnitEntity, tx: Float, ty: Float, dt: Float) {
        val dx = tx - u.x
        val dy = ty - u.y
        val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist < 4f) { u.anim = AnimState.IDLE; return }
        val speed = u.stats.moveSpeed
        u.x += (dx / dist) * speed * dt
        u.y += (dy / dist) * speed * dt
        u.anim = AnimState.MOVE
    }
}
