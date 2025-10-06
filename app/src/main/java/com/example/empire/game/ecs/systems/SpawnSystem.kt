package com.example.empire.game.ecs.systems

/**
 * Quản lý spawn entities (địch, item…). Tối giản cho m dễ cắm AI sau.
 */
class SpawnSystem {

    // Đổi tên rõ ràng theo asset: FLYBEE, MONSTER, SLIME, WOLF
    enum class EnemyType { FLYBEE, MONSTER, SLIME, WOLF }

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
        var kbTime: Float = 0f
    ) {
        enum class Facing { UP, DOWN, LEFT, RIGHT }
        enum class State { IDLE, WALK, ATTACK, DEAD }
    }

    private val _enemies = mutableListOf<Enemy>()
    val enemies: List<Enemy> get() = _enemies

    // ===== Dynamic spawning (simple incremental spawner) =====
    var maxEnemies = 30
    private var spawnTimer = 0f
    var spawnInterval = 3.5f // sẽ giảm dần theo thời gian
    private var elapsed = 0f
    private var difficultyMul = 1f
    private var damageScale = 1f
    private var autoSpawnEnabled = true

    // ===== Wave Mode (3 waves -> cooldown -> restart) =====
    data class WaveConfig(
        val enemyTypes: List<EnemyType>,
        val waves: Int = 3,
        val countPerType: Int = 5,
        val cooldownAfter: Float = 30f
    )
    private var waveConfig: WaveConfig? = null
    private var currentWave = 0
    private var cycleCooldown = 0f
    private var waveMode = false
    private var mapWidthPx = 0
    private var mapHeightPx = 0
    var onWaveCycleComplete: () -> Unit = {}

    fun setMapBounds(w: Int, h: Int) { mapWidthPx = w; mapHeightPx = h }

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
        if (_enemies.none { it.alive }) {
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
        val minDist = 220f
        var tries = 0
        val w = mapWidthPx.coerceAtLeast(1024)
        val h = mapHeightPx.coerceAtLeast(768)
        while (tries < 20) {
            val x = (0 until w).random().toFloat()
            val y = (0 until h).random().toFloat()
            val dx = (x - targetX)
            val dy = (y - targetY)
            if (dx*dx + dy*dy >= minDist * minDist) return x to y
            tries++
        }
        return (w/2f) to (h/2f) // fallback
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
            EnemyType.SLIME -> 18
            EnemyType.FLYBEE -> 22
            EnemyType.MONSTER -> 36
            EnemyType.WOLF -> 30
        }
        val (w,h) = when(type){
            EnemyType.WOLF -> 56f to 80f // x2 scale (original ~28x40)
            else -> 28f to 40f
        }
        _enemies += Enemy(x, y, w = w, h = h, type = type, maxHp = baseHp, hp = baseHp)
    }

    // Basic AI: wander + chase nearest player snapshot (provided externally via setPlayerTarget)
    private var targetX = 0f
    private var targetY = 0f
    fun setPlayerTarget(px: Float, py: Float) { targetX = px; targetY = py }

    // External callbacks
    var onPlayerHit: (Enemy, Int) -> Unit = { _, _ -> }

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
        EnemyType.MONSTER -> 5
        EnemyType.SLIME -> 2
        EnemyType.FLYBEE -> 3
    }.let { (it * damageScale).toInt().coerceAtLeast(1) }
    private fun attackCooldown(e: Enemy): Float = when(e.type){
        EnemyType.WOLF -> 0.95f
        EnemyType.MONSTER -> 1.25f
        EnemyType.SLIME -> 1.1f
        EnemyType.FLYBEE -> 0.65f
    }
    private fun attackDuration(e: Enemy): Float = when(e.type){
        EnemyType.FLYBEE -> 0.5f // nhanh hơn
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
            // Simple distance check
            val dx = targetX - (e.x + e.w/2f)
            val dy = targetY - (e.y + e.h/2f)
            val dist2 = dx*dx + dy*dy
            val chaseRange = 260f * 260f
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
                    onPlayerHit(e, attackDamage(e))
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
                e.x += e.vx * dt
                e.y += e.vy * dt
                // facing only update when moving / not attacking
                if (kotlin.math.abs(e.vx) >= 1f || kotlin.math.abs(e.vy) >= 1f) {
                    e.facing = if (kotlin.math.abs(e.vx) >= kotlin.math.abs(e.vy)) {
                        if (e.vx >= 0) Enemy.Facing.RIGHT else Enemy.Facing.LEFT
                    } else {
                        if (e.vy >= 0) Enemy.Facing.DOWN else Enemy.Facing.UP
                    }
                }
            }
            // Knockback decay
            if (e.kbTime > 0f) {
                e.kbTime -= dt
                val t = (e.kbTime / KB_DURATION).coerceIn(0f,1f)
                e.x += e.kbVX * t * dt
                e.y += e.kbVY * t * dt
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
}
