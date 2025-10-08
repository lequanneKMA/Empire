package com.example.empire.game.army

import com.example.empire.game.economy.CostTable
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.ecs.systems.SpawnSystem
import com.example.empire.game.map.TileMap
import com.example.empire.game.ai.ArmyAIController
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

    // Map reference for collision
    private var tileMap: TileMap? = null
    private var aiController: ArmyAIController? = null
    fun setTileMap(map: TileMap?) {
        tileMap = map
        if (map != null) {
            aiController = ArmyAIController(map) { unit, mx, my -> attemptMove(unit, mx, my) }
        } else aiController = null
    }

    // Collision bounds (approx sprite size). Using bottom-centered anchor: u.x = centerX, u.y = bottomY
    // Logical collision size (reduced ~55% for smaller footprint vs original 32x48)
    private val unitW = 18f
    private val unitH = 26f

    fun clear() { _units.clear() }

    var maxUnits = 20

    /** Callback khi spawn (để GameView có thể play SFX, v.v.) */
    var onUnitSpawned: (UnitEntity) -> Unit = {}

    /** Mua và spawn quanh player. */
    fun buy(type: UnitType, playerX: Float, playerY: Float): Boolean {
        // Bỏ mọi điều kiện unlock khác – chỉ cần đủ vàng & thịt.
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
        var ux = px + cos(angle).toFloat() * radius
        var uy = py + sin(angle).toFloat() * radius
        // Avoid spawning inside collision tiles or on top of other units
        val map = tileMap
        if (map != null) {
            val (fx, fy) = findFreeSpawnPosition(ux, uy, map)
            ux = fx; uy = fy
        }
        val unit = UnitEntity(ux, uy, type, stats)
        _units += unit
        onUnitSpawned(unit)
    }

    private fun findFreeSpawnPosition(x0: Float, y0: Float, map: TileMap): Pair<Float, Float> {
        if (!collidesMap(x0, y0, map) && !overlapsOtherUnits(x0, y0)) return x0 to y0
        val maxRings = 10
        val step = 12f
        for (r in 1..maxRings) {
            val samples = 16
            val radius = r * step
            for (i in 0 until samples) {
                val ang = (i.toFloat() / samples) * (Math.PI * 2).toFloat()
                val nx = x0 + kotlin.math.cos(ang) * radius
                val ny = y0 + kotlin.math.sin(ang) * radius
                if (!collidesMap(nx, ny, map) && !overlapsOtherUnits(nx, ny)) return nx to ny
            }
        }
        return x0 to y0
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
            if (u.cooldown > 0f) u.cooldown -= dt
            when (u.type) {
                UnitType.WARRIOR, UnitType.LANCER -> updateMelee(u, dt, playerX, playerY, i, total, enemies, refreshTarget)
                UnitType.ARCHER -> updateRanged(u, dt, playerX, playerY, i, total, enemies, refreshTarget)
                else -> formationFollow(u, dt, playerX, playerY, i, total)
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
        // Hysteresis: enter attack when dist <= reach, exit when dist > reach * 1.15
        if (u.isInAttackRange && dist > reach * 1.15f) u.isInAttackRange = false
        else if (!u.isInAttackRange && dist <= reach) u.isInAttackRange = true

        if (!u.isInAttackRange) {
            val speed = u.stats.moveSpeed
            if (dist > 2f) {
                val mxTry = (dx / dist) * speed * dt
                val myTry = (dy / dist) * speed * dt
                val canDirect = attemptMove(u, mxTry, myTry, previewOnly = true)
                if (!canDirect) aiController?.update(u, ex, ey, frameCounter++, dt, u.stats.moveSpeed) else u.clearPath()
                if (u.path != null) followPathLegacy(u, speed, dt) else attemptMove(u, mxTry, myTry)
                u.vx = mxTry / dt; u.vy = myTry / dt
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
        // TODO: nếu enemy có giáp sau này: dmg = max(1, dmg - enemy.defense)
        val killed = spawnSystem.applyDamage(enemy, dmg)
        // Chọn variant attack (Warrior có Attack1/Attack2, Lancer/Archer 1 sheet)
        u.attackVariantIndex = (u.attackVariantIndex + 1) % 2 // simple toggle giữa 0/1
        u.attackAnimTimer = 0f
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
        // Hysteresis tương tự ranged
        if (u.isInAttackRange && dist > range * 1.1f) u.isInAttackRange = false
        else if (!u.isInAttackRange && dist <= range) u.isInAttackRange = true

        if (!u.isInAttackRange) {
            val speed = u.stats.moveSpeed
            if (dist > range * 0.8f) {
                val mxTry = (dx / dist) * speed * dt
                val myTry = (dy / dist) * speed * dt
                val canDirect = attemptMove(u, mxTry, myTry, previewOnly = true)
                if (!canDirect) aiController?.update(u, ex, ey, frameCounter++, dt, u.stats.moveSpeed) else u.clearPath()
                if (u.path != null) followPathLegacy(u, speed, dt) else attemptMove(u, mxTry, myTry)
                u.vx = mxTry / dt; u.vy = myTry / dt
                u.anim = AnimState.MOVE
            } else u.anim = AnimState.IDLE
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
        u.attackVariantIndex = 0
        u.attackAnimTimer = 0f
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
        val mx = (dx / dist) * speed * dt
        val my = (dy / dist) * speed * dt
        attemptMove(u, mx, my)
        u.anim = AnimState.MOVE
        if (mx < 0f) u.facing = Facing.LEFT else if (mx > 0f) u.facing = Facing.RIGHT
    }

    // ================= Movement + Collision =================
    private fun attemptMove(u: UnitEntity, mx: Float, my: Float, previewOnly: Boolean = false): Boolean {
        val map = tileMap
        if (map == null) {
            if (!previewOnly) { u.x += mx; u.y += my }
            return true
        }
        // Chia nhỏ bước để tránh xuyên tường khi tốc độ cao (simple sub-step)
        val steps = kotlin.math.max(1, kotlin.math.ceil((kotlin.math.max(kotlin.math.abs(mx), kotlin.math.abs(my)) / 8f)).toInt())
        val stepX = mx / steps
        val stepY = my / steps
        for (i in 0 until steps) {
            if (stepX != 0f) {
                val nx = u.x + stepX
                if (!collidesMap(nx, u.y, map)) { if (!previewOnly) u.x = nx } else return false
            }
            if (stepY != 0f) {
                val ny = u.y + stepY
                if (!collidesMap(u.x, ny, map)) { if (!previewOnly) u.y = ny } else return false
            }
        }
        return true
    }

    // Legacy path follow (still used until movement fully moved to AI controller steering)
    private fun followPathLegacy(u: UnitEntity, speed: Float, dt: Float) {
        val map = tileMap ?: return
        val path = u.path ?: return
        if (u.pathIndex >= path.size) { u.clearPath(); return }
        val ts = map.tileSize.toFloat()
        val (tx, ty) = path[u.pathIndex]
        val targetX = tx * ts + ts/2f
        val targetY = ty * ts + unitH
        val dx = targetX - u.x
        val dy = targetY - u.y
        val dist = kotlin.math.sqrt(dx*dx + dy*dy)
        if (dist < 5f) { u.pathIndex++; if (u.pathIndex >= path.size) u.clearPath(); return }
        val mx = (dx / dist) * speed * dt
        val my = (dy / dist) * speed * dt
        attemptMove(u, mx, my)
    }

    private fun collidesMap(cx: Float, by: Float, map: TileMap): Boolean {
        // cx: center X, by: bottom Y (anchor); convert to AABB
        val left = cx - unitW/2f
        val top = by - unitH
        val w = unitW
        val h = unitH
        val ts = map.tileSize
        val tx0 = (left / ts).toInt()
        val tx1 = ((left + w - 1) / ts).toInt()
        val ty0 = (top / ts).toInt()
        val ty1 = ((top + h - 1) / ts).toInt()
        for (ty in ty0..ty1) for (tx in tx0..tx1) if (map.isSolidAt(tx, ty)) return true
        return false
    }

    // Line of sight using simple Bresenham sampling
    private fun hasLineOfSight(map: TileMap, x0:Int, y0:Int, x1:Int, y1:Int): Boolean {
        var dx = kotlin.math.abs(x1 - x0)
        var dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var cx = x0
        var cy = y0
        while (true) {
            if (map.isSolidAt(cx, cy)) return false
            if (cx == x1 && cy == y1) return true
            val e2 = 2*err
            if (e2 >= dy) { err += dy; cx += sx }
            if (e2 <= dx) { err += dx; cy += sy }
        }
    }

    // Extension for UnitEntity used by AI controller
    private fun UnitEntity.hasLineOfSight(map: TileMap, tx:Int, ty:Int): Boolean {
        val ts = map.tileSize
        val sx = (x / ts).toInt()
        val sy = ((y - unitH/2f) / ts).toInt()
        return hasLineOfSight(map, sx, sy, tx, ty)
    }

    private var frameCounter: Int = 0

    private fun overlapsOtherUnits(x: Float, y: Float): Boolean {
        val cx = x; val cy = y - unitH/2f // approximate center for separation
        val r = unitW * 0.5f
        val r2 = (r*1.1f) * (r*1.1f)
        _units.forEach { o ->
            val ocx = o.x; val ocy = o.y - unitH/2f
            val dx = ocx - cx; val dy = ocy - cy
            if (dx*dx + dy*dy < r2) return true
        }
        return false
    }

    // ================= Post-update separation (avoid stacking) =================
    private fun applySeparation() {
        val n = _units.size
        if (n < 2) return
        val map = tileMap
        for (i in 0 until n) {
            val a = _units[i]
            if (a.hp <= 0) continue
            for (j in i+1 until n) {
                val b = _units[j]
                if (b.hp <= 0) continue
                val axc = a.x; val ayc = a.y - unitH/2f
                val bxc = b.x; val byc = b.y - unitH/2f
                val dx = axc - bxc
                val dy = ayc - byc
                val overlapX = (unitW) - kotlin.math.abs(dx)
                val overlapY = (unitH) - kotlin.math.abs(dy)
                if (overlapX > 0f && overlapY > 0f) {
                    val push = kotlin.math.min(overlapX, overlapY) * 0.5f
                    val nx = if (dx == 0f) 0f else kotlin.math.sign(dx)
                    val ny = if (dy == 0f) 0f else kotlin.math.sign(dy)
                    // attempt move a
                    val oldAx = a.x; val oldAy = a.y
                    a.x += nx * push; a.y += ny * push
                    if (map != null && collidesMap(a.x, a.y, map)) { a.x = oldAx; a.y = oldAy }
                    // attempt move b opposite
                    val oldBx = b.x; val oldBy = b.y
                    b.x -= nx * push; b.y -= ny * push
                    if (map != null && collidesMap(b.x, b.y, map)) { b.x = oldBx; b.y = oldBy }
                }
            }
        }
    }

    // Hook separation at end of public update
    fun postUpdate() { applySeparation() }
}
