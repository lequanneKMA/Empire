package com.example.empire.game.ecs.systems

/**
 * Quản lý spawn entities (địch, item…). Tối giản cho m dễ cắm AI sau.
 */
class SpawnSystem {

    // Đổi tên rõ ràng theo asset: FLYBEE, MONSTER, SLIME, WOLF
    enum class EnemyType { FLYBEE, MONSTER, SLIME, WOLF }

    enum class AttackTarget { PLAYER, ARMY }

    data class Enemy(
        var x: Float,
        var y: Float,
        val w: Float = 28f,
        val h: Float = 40f,
        val type: EnemyType,
        var maxHp: Int = 3,
        var hp: Int = 3,
        var alive: Boolean = true,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var facing: Facing = Facing.DOWN,
        var state: State = State.IDLE,
        var aiTimer: Float = 0f,
        // Attack logic
        var attackCooldown: Float = 0f,
        var attackTimer: Float = 0f,
        var attackDidDamage: Boolean = false,
        // Death sequence
        var deathTimer: Float = 0f,
        // Knockback
        var kbVX: Float = 0f,
        var kbVY: Float = 0f,
        var kbTime: Float = 0f,
        // who this enemy intended to hit when starting an attack
        var lastAttackTarget: AttackTarget = AttackTarget.PLAYER
    ) {
        enum class Facing { UP, DOWN, LEFT, RIGHT }
        enum class State { IDLE, WALK, ATTACK, DEAD }
    }

    private val _enemies = mutableListOf<Enemy>()
    val enemies: List<Enemy> get() = _enemies

    // ===== Dynamic spawning (simple incremental spawner) =====
    var maxEnemies = 30
    private var spawnTimer = 0f
    var spawnInterval = 2.2f // nhanh hơn (giảm từ 3.5) và sẽ giảm dần theo thời gian
    private var elapsed = 0f
    private var difficultyMul = 1f
    private var damageScale = 1f
    private var autoSpawnEnabled = true

    // ===== Wave Mode (3 waves -> cooldown -> restart) =====
    data class WaveConfig(
        val enemyTypes: List<EnemyType>,
        val waves: Int = 2,               // giảm còn 2 wave
        val countPerType: Int = 5,
        val cooldownAfter: Float = 10f    // rút ngắn downtime
    )
    private var waveConfig: WaveConfig? = null
    private var currentWave = 0
    private var cycleCooldown = 0f
    private var waveMode = false
    private var mapWidthPx = 0
    private var mapHeightPx = 0
    // Reference to tile map for collision (optional)
    private var tileMap: com.example.empire.game.map.TileMap? = null
    var onWaveCycleComplete: () -> Unit = {}

    fun setMapBounds(w: Int, h: Int) { mapWidthPx = w; mapHeightPx = h }
    fun setTileMap(map: com.example.empire.game.map.TileMap?) { this.tileMap = map }

    fun enableWaves(cfg: WaveConfig) {
        waveConfig = cfg
        currentWave = 0
        cycleCooldown = 0f
        waveMode = true
        // clear existing enemies & timers
        _enemies.clear()
        spawnTimer = 0f; elapsed = 0f; difficultyMul = 1f
    }
    fun disableWaves() { waveMode = false; waveConfig = null }

    private fun waveUpdate(dt: Float) {
        val cfg = waveConfig ?: return
        // If cooldown active
        if (cycleCooldown > 0f) {
            cycleCooldown -= dt
            if (cycleCooldown <= 0f) {
                // reset for new cycle
                currentWave = 0
            }
            return
        }
        // If no wave started yet
        if (currentWave == 0 && _enemies.isEmpty()) {
            spawnWave(1, cfg)
            currentWave = 1
            return
        }
        // If enemies cleared and more waves remain
        // Consider enemy 'cleared' once it has entered DEAD state (không cần chờ biến mất hẳn)
        if (_enemies.none { it.alive && it.state != Enemy.State.DEAD }) {
            if (currentWave < cfg.waves) {
                val next = currentWave + 1
                spawnWave(next, cfg)
                currentWave = next
            } else {
                // Cycle complete
                onWaveCycleComplete()
                cycleCooldown = cfg.cooldownAfter
                currentWave = 0
            }
        }
    }

    private fun spawnWave(index: Int, cfg: WaveConfig) {
        // spawn countPerType of each enemy type
        cfg.enemyTypes.forEach { type ->
            repeat(cfg.countPerType) {
                val (sx, sy) = randomSpawnPosAvoidPlayer()
                spawnEnemy(sx, sy, type)
            }
        }
        println("[WAVE] Spawned wave $index/${cfg.waves} types=${cfg.enemyTypes} totalSpawned=${cfg.enemyTypes.size * cfg.countPerType}")
    }

    // Player target is known (targetX,targetY). Avoid spawning quá gần player
    private fun randomSpawnPosAvoidPlayer(): Pair<Float, Float> {
        val minDist = 90f   // gần hơn
        val maxDist = 320f  // tránh quá xa
        val w = mapWidthPx.coerceAtLeast(1024)
        val h = mapHeightPx.coerceAtLeast(768)
        var tries = 0
        while (tries < 30) {
            val ang = kotlin.random.Random.nextFloat() * (Math.PI * 2).toFloat()
            val r = (minDist + kotlin.random.Random.nextFloat() * (maxDist - minDist))
            val x = (targetX + kotlin.math.cos(ang) * r).coerceIn(0f, w - 1f)
            val y = (targetY + kotlin.math.sin(ang) * r).coerceIn(0f, h - 1f)
            val dx = x - targetX
            val dy = y - targetY
            val d2 = dx*dx + dy*dy
            if (d2 >= minDist*minDist) return x to y
            tries++
        }
        // fallback: spawn hơi lệch so với player
        return (targetX + 200f) to (targetY + 100f)
    }

    // call periodically from outside if needed; or integrate in update()
    private fun autoSpawn(dt: Float) {
        elapsed += dt
        // scale difficulty nhẹ: mỗi 30s giảm interval 10% (clamp)
        if (elapsed > 30f) {
            elapsed = 0f
            spawnInterval = (spawnInterval * 0.9f).coerceAtLeast(1.2f)
            difficultyMul = (difficultyMul + 0.1f).coerceAtMost(2.0f)
        }
        if (_enemies.size >= maxEnemies) return
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = spawnInterval
            // spawn 1-2 enemy dựa trên difficulty
            val batch = if (difficultyMul > 1.5f) 2 else 1
            repeat(batch) {
                spawnRandom()
            }
        }
    }

    private fun spawnRandom() {
        // simple random around center (0,0) offset; thực chiến sẽ truyền map bounds từ ngoài
        val cx = 800f; val cy = 600f
        val offX = (-500..500).random().toFloat()
        val offY = (-400..400).random().toFloat()
        val pick = when((0..99).random()) {
            in 0..35 -> EnemyType.SLIME
            in 36..60 -> EnemyType.FLYBEE
            in 61..80 -> EnemyType.WOLF
            else -> EnemyType.MONSTER
        }
        spawnEnemy(cx + offX, cy + offY, pick)
    }

    fun clear() = _enemies.clear()

    fun spawnEnemy(x: Float, y: Float, type: EnemyType) {
        val baseHp = when(type){
            EnemyType.SLIME -> 40    // buff từ 18
            EnemyType.FLYBEE -> 24
            EnemyType.MONSTER -> 80  // buff từ 36
            EnemyType.WOLF -> 34
        }
        val (w,h) = when(type){
            EnemyType.SLIME -> (28f*1.5f) to (40f*1.5f)      // giảm còn 1.5x (trước 3x)
            EnemyType.MONSTER -> (28f*2f) to (40f*2f)        // giảm còn 2x (trước 4x)
            EnemyType.WOLF -> 56f to 80f                     // giữ
            EnemyType.FLYBEE -> 28f to 40f
        }
        val scaledHp = (baseHp * damageScale).toInt().coerceAtLeast(1)
        val (fx, fy) = findFreePositionForSpawn(x, y, w, h, type)
        _enemies += Enemy(fx, fy, w = w, h = h, type = type, maxHp = scaledHp, hp = scaledHp)
    }

    // Basic AI: wander + chase nearest player snapshot (provided externally via setPlayerTarget)
    private var targetX = 0f
    private var targetY = 0f
    fun setPlayerTarget(px: Float, py: Float) { targetX = px; targetY = py }

    // External callbacks
    var onPlayerHit: (Enemy, Int) -> Unit = { _, _ -> }
    // Callback khi enemy gây damage (impact) – cho army units
    var onEnemyAttackImpact: (Enemy) -> Unit = { _ -> }
    var onEnemyDeath: (EnemyType) -> Unit = { _ -> }

    // Query to let enemies prefer nearby army units over the player.
    // Should return the center (x,y) of the nearest alive army unit within radius, or null if none.
    var queryNearestArmyCenter: (ex: Float, ey: Float, radius: Float) -> Pair<Float, Float>? = { _, _, _ -> null }

    // Damage application centralization
    fun applyDamage(e: Enemy, dmg: Int): Boolean {
        if (!e.alive || e.state == Enemy.State.DEAD) return false
        e.hp -= dmg
        if (e.hp <= 0) {
            e.hp = 0
            startDeath(e)
            return true
        }
        return false
    }

    private fun startDeath(e: Enemy) {
        e.state = Enemy.State.DEAD
        e.vx = 0f; e.vy = 0f
        e.attackTimer = 0f
        e.deathTimer = deathDuration(e.type)
        e.kbTime = 0f
        onEnemyDeath(e.type)
    }

    private fun deathDuration(type: EnemyType) = when(type){
        EnemyType.WOLF -> 0.8f
        EnemyType.MONSTER -> 0.9f
        EnemyType.FLYBEE -> 0.7f
        EnemyType.SLIME -> 0.6f
    }

    private fun attackRange(e: Enemy): Float = when(e.type){
        EnemyType.WOLF -> 42f
        EnemyType.MONSTER -> 48f
        EnemyType.SLIME -> 34f
        EnemyType.FLYBEE -> 38f // áp sát chích
    }
    private fun attackDamage(e: Enemy): Int = when(e.type){
        EnemyType.WOLF -> 4
        EnemyType.MONSTER -> 8   // buff từ 5
        EnemyType.SLIME -> 4     // buff từ 2
        EnemyType.FLYBEE -> 3
    }.let { (it * damageScale).toInt().coerceAtLeast(1) }
    private fun attackCooldown(e: Enemy): Float = when(e.type){
        EnemyType.WOLF -> 0.95f
        EnemyType.MONSTER -> 1.25f
        EnemyType.SLIME -> 1.1f
        EnemyType.FLYBEE -> 0.65f
    }
    private fun attackDuration(e: Enemy): Float = when(e.type){
        EnemyType.FLYBEE -> 0.5f 
        else -> 0.6f
    }
    private fun attackHitPoint(e: Enemy): Float = 0.3f // seconds into animation (FlyBee vẫn dùng)

    fun update(dt: Float) {
        if (waveMode) waveUpdate(dt) else if (autoSpawnEnabled) autoSpawn(dt)
        _enemies.forEach { e ->
            if (!e.alive) return@forEach
            if (e.state == Enemy.State.DEAD) {
                e.deathTimer -= dt
                if (e.deathTimer <= 0f) {
                    e.alive = false
                }
                return@forEach
            }
            // Decide target each frame: prefer nearest army unit if any within chase range
            val ex = e.x + e.w/2f
            val ey = e.y + e.h/2f
            val chaseRange = 260f * 260f
            var targetCx = targetX
            var targetCy = targetY
            var targetKind = AttackTarget.PLAYER
            queryNearestArmyCenter(ex, ey, kotlin.math.sqrt(chaseRange.toFloat())).let { pair ->
                if (pair != null) {
                    targetCx = pair.first
                    targetCy = pair.second
                    targetKind = AttackTarget.ARMY
                }
            }
            val dx = targetCx - ex
            val dy = targetCy - ey
            val dist2 = dx*dx + dy*dy
            val speed = when(e.type){
                EnemyType.WOLF -> 70f   // tăng hunt cảm giác đe doạ
                EnemyType.SLIME -> 42f  // nhanh hơn chút
                EnemyType.FLYBEE -> 60f // bay nhanh áp sát
                EnemyType.MONSTER -> 34f
            }
            // Attack state update
            if (e.state == Enemy.State.ATTACK) {
                e.attackTimer -= dt
                val triggerTime = attackDuration(e) - attackHitPoint(e)
                if (!e.attackDidDamage && e.attackTimer <= triggerTime) {
                    e.attackDidDamage = true
                    // Gây damage đúng đối tượng đã khoá khi bắt đầu tấn công
                    if (e.lastAttackTarget == AttackTarget.PLAYER) {
                        onPlayerHit(e, attackDamage(e))
                    } else {
                        // Báo impact để ArmySystem xử lý lựa chọn unit trúng
                        onEnemyAttackImpact(e)
                    }
                }
                if (e.attackTimer <= 0f) {
                    e.state = Enemy.State.IDLE
                    e.attackCooldown = attackCooldown(e)
                    e.attackDidDamage = false
                }
            } else {
                // Movement / decision
                if (dist2 < chaseRange) {
                    val d = kotlin.math.sqrt(dist2)
                    val dist = d
                    val range = attackRange(e)
                    if (dist > range) {
                        e.vx = (dx / d) * speed
                        e.vy = (dy / d) * speed
                        e.state = Enemy.State.WALK
                    } else {
                        // attempt attack
                        e.vx = 0f; e.vy = 0f
                        if (e.attackCooldown <= 0f) {
                            e.state = Enemy.State.ATTACK
                            e.attackTimer = attackDuration(e)
                            e.attackDidDamage = false
                            e.lastAttackTarget = targetKind
                        } else {
                            e.state = Enemy.State.IDLE
                        }
                    }
                } else {
                    // wander
                    e.aiTimer -= dt
                    if (e.aiTimer <= 0f) {
                        e.aiTimer = 1.5f + kotlin.random.Random.nextFloat()*2f
                        val ang = kotlin.random.Random.nextFloat()* (Math.PI*2).toFloat()
                        e.vx = kotlin.math.cos(ang) * speed * 0.4f
                        e.vy = kotlin.math.sin(ang) * speed * 0.4f
                    }
                    e.state = if (kotlin.math.abs(e.vx) > 1f || kotlin.math.abs(e.vy) > 1f) Enemy.State.WALK else Enemy.State.IDLE
                }
            }
            if (e.attackCooldown > 0f) e.attackCooldown -= dt
            // Apply movement (freeze while ATTACK)
            if (e.state != Enemy.State.ATTACK) {
                applyMovementWithCollision(e, dt)
            }
            // Knockback decay
            if (e.kbTime > 0f) {
                e.kbTime -= dt
                val t = (e.kbTime / KB_DURATION).coerceIn(0f,1f)
                if (t > 0f) applyKnockbackMovement(e, t * dt)
                if (e.kbTime <= 0f) { e.kbVX = 0f; e.kbVY = 0f }
            }
        }
        applySeparation()
    }

    fun removeDead() { _enemies.removeAll { !it.alive } }

    // Simple separation to reduce stacking
    private fun applySeparation() {
        val list = _enemies
        val n = list.size
        if (n < 2) return
        for (i in 0 until n) {
            val a = list[i]
            if (!a.alive || a.state == Enemy.State.DEAD) continue
            for (j in i+1 until n) {
                val b = list[j]
                if (!b.alive || b.state == Enemy.State.DEAD) continue
                val dx = (a.x + a.w/2f) - (b.x + b.w/2f)
                val dy = (a.y + a.h/2f) - (b.y + b.h/2f)
                val overlapX = (a.w + b.w)/2f - kotlin.math.abs(dx)
                val overlapY = (a.h + b.h)/2f - kotlin.math.abs(dy)
                if (overlapX > 0f && overlapY > 0f) {
                    val push = kotlin.math.min(overlapX, overlapY) * 0.25f
                    val nx = if (dx == 0f) 0f else kotlin.math.sign(dx)
                    val ny = if (dy == 0f) 0f else kotlin.math.sign(dy)
                    a.x += nx * push
                    a.y += ny * push
                    b.x -= nx * push
                    b.y -= ny * push
                }
            }
        }
    }

    fun applyKnockback(e: Enemy, dirX: Float, dirY: Float, strength: Float) {
        if (!e.alive || e.state == Enemy.State.DEAD) return
        e.kbVX = dirX * strength
        e.kbVY = dirY * strength
        e.kbTime = KB_DURATION
    }

    fun setDamageScale(scale: Float) { damageScale = scale }
    fun setAutoSpawnEnabled(flag: Boolean) { autoSpawnEnabled = flag }

    // ---- Wave status queries (read-only) for HUD ----
    fun isWaveMode(): Boolean = waveMode
    fun currentWave(): Int = currentWave
    fun totalWaves(): Int = waveConfig?.waves ?: 0
    fun inCycleCooldown(): Boolean = cycleCooldown > 0f && waveMode
    fun cooldownRemaining(): Float = cycleCooldown.coerceAtLeast(0f)
    fun waveEnemyTypes(): List<EnemyType> = waveConfig?.enemyTypes ?: emptyList()

    companion object {
        private const val KB_DURATION = 0.2f
    }

    // ================= Collision Helpers =================
    private fun canPassThrough(type: EnemyType): Boolean = when(type) {
        EnemyType.FLYBEE -> true
        else -> false
    }

    private fun applyMovementWithCollision(e: Enemy, dt: Float) {
        val map = tileMap
        if (map == null || canPassThrough(e.type)) {
            e.x += e.vx * dt
            e.y += e.vy * dt
        } else {
            val nx = e.x + e.vx * dt
            if (!rectCollidesMap(nx, e.y, e.w, e.h, map)) {
                e.x = nx
            }
            val ny = e.y + e.vy * dt
            if (!rectCollidesMap(e.x, ny, e.w, e.h, map)) {
                e.y = ny
            }
        }
        // facing update
        if (kotlin.math.abs(e.vx) >= 1f || kotlin.math.abs(e.vy) >= 1f) {
            e.facing = if (kotlin.math.abs(e.vx) >= kotlin.math.abs(e.vy)) {
                if (e.vx >= 0) Enemy.Facing.RIGHT else Enemy.Facing.LEFT
            } else {
                if (e.vy >= 0) Enemy.Facing.DOWN else Enemy.Facing.UP
            }
        }
    }

    private fun applyKnockbackMovement(e: Enemy, scaledDt: Float) {
        val map = tileMap
        if (map == null || canPassThrough(e.type)) {
            e.x += e.kbVX * scaledDt
            e.y += e.kbVY * scaledDt
        } else {
            val nx = e.x + e.kbVX * scaledDt
            if (!rectCollidesMap(nx, e.y, e.w, e.h, map)) e.x = nx
            val ny = e.y + e.kbVY * scaledDt
            if (!rectCollidesMap(e.x, ny, e.w, e.h, map)) e.y = ny
        }
    }

    private fun rectCollidesMap(x: Float, y: Float, w: Float, h: Float, map: com.example.empire.game.map.TileMap): Boolean {
        val ts = map.tileSize
        val left = (x / ts).toInt()
        val right = ((x + w - 1) / ts).toInt()
        val top = (y / ts).toInt()
        val bottom = ((y + h - 1) / ts).toInt()
        for (ty in top..bottom) {
            for (tx in left..right) {
                if (map.isSolidAt(tx, ty)) return true
            }
        }
        return false
    }

    private fun findFreePositionForSpawn(x: Float, y: Float, w: Float, h: Float, type: EnemyType): Pair<Float, Float> {
        val map = tileMap ?: return x to y
        if (canPassThrough(type)) return x to y
        if (!rectCollidesMap(x, y, w, h, map)) return x to y
        val ts = map.tileSize.toFloat()
        val maxRadiusTiles = 10
        // Spiral / ring search expanding outward
        for (r in 1..maxRadiusTiles) {
            val samples = 16
            val radius = r * ts
            for (i in 0 until samples) {
                val ang = (i.toFloat() / samples) * (Math.PI * 2).toFloat()
                val nx = (x + kotlin.math.cos(ang) * radius).coerceIn(0f, mapWidthPx - w - 1f)
                val ny = (y + kotlin.math.sin(ang) * radius).coerceIn(0f, mapHeightPx - h - 1f)
                if (!rectCollidesMap(nx, ny, w, h, map)) return nx to ny
            }
        }
        // Fallback: just return original even if colliding (rare if map mostly blocked) – enemy will be clamped by movement later
        return x to y
    }
}
