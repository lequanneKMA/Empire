package com.example.empire.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import com.example.empire.data.SaveManager
import com.example.empire.game.map.MapLoader
import com.example.empire.game.map.MapRenderer
import com.example.empire.game.map.TileMap
import com.example.empire.core.gfx.PlayerAnimator
import com.example.empire.core.gfx.AnimKind
import com.example.empire.core.gfx.Facing
import com.example.empire.game.ecs.systems.PhysicsSystem
import com.example.empire.game.ecs.systems.RenderSystem
import com.example.empire.game.ecs.systems.SpawnSystem
import com.example.empire.game.ecs.systems.EnemyRenderSystem
import com.example.empire.game.ecs.systems.EnemySpriteLoader
import com.example.empire.game.ecs.systems.CombatSystem
import com.example.empire.game.progression.ProgressionManager
import com.example.empire.game.player.PlayerStats
import com.example.empire.game.economy.ResourceManager
import com.example.empire.game.army.*
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.empire.game.ui.HudRenderer
import com.example.empire.game.ui.overlay.MapSelectOverlay
import com.example.empire.game.ui.overlay.BuyMenuOverlay
import com.example.empire.game.ui.overlay.GameOverOverlay
import com.example.empire.game.ui.overlay.HousePromptOverlay
import com.example.empire.game.ui.overlay.WaveHud
import com.example.empire.audio.SfxManager
import kotlin.math.abs
import kotlin.math.sqrt
import com.example.empire.game.sheep.SheepSystem
import com.example.empire.game.boss.BossSpriteLoader
import com.example.empire.game.boss.BossSystem

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // ---------------- runtime ----------------
    @Volatile private var running = false
    private var gameThread: Thread? = null

    // ---------------- world / map ----------------
    // Multi-map support + progression locked maps
    private data class WorldMap(
        val id: String,
        val file: String,
        val tileset: String,
        val enemyTypes: List<SpawnSystem.EnemyType>,
        val levelRequired: Int,
        val damageScale: Float
    )
    // id "main" is the home base and never spawns enemies
    private val maps: List<WorldMap> = listOf(
        // Main home base: no enemies
        WorldMap("main",   "maps/map_main.json",   "maps/tileset.png",      emptyList(), levelRequired = 0, damageScale = 1f),
        // Map1: Forest (WOLF + FLYBEE)
        WorldMap("forest", "maps/forest.json",     "maps/tile_forest.png",  listOf(SpawnSystem.EnemyType.WOLF, SpawnSystem.EnemyType.FLYBEE), levelRequired = 1, damageScale = 1.00f),
        // Map2: Samac desert (SLIME + MONSTER)
        WorldMap("samac",  "maps/samac.json",      "maps/tile_samac.png",   listOf(SpawnSystem.EnemyType.SLIME, SpawnSystem.EnemyType.MONSTER), levelRequired = 2, damageScale = 1.15f),
        // Map3: mixed set 1
        WorldMap("mix1",   "maps/mix1.json",     "maps/tile_mix1.png",  listOf(SpawnSystem.EnemyType.WOLF, SpawnSystem.EnemyType.FLYBEE, SpawnSystem.EnemyType.SLIME), levelRequired = 3, damageScale = 1.30f),
        // Map4: mixed set 2
        WorldMap("mix2",   "maps/mix2.json",     "maps/tile_mix2.png",  listOf(SpawnSystem.EnemyType.MONSTER, SpawnSystem.EnemyType.WOLF, SpawnSystem.EnemyType.SLIME), levelRequired = 4, damageScale = 1.45f),
        // Map5: final all types ( + future boss )
        WorldMap("final",  "maps/final.json",     "maps/tile_final.png",  listOf(SpawnSystem.EnemyType.MONSTER, SpawnSystem.EnemyType.WOLF, SpawnSystem.EnemyType.SLIME, SpawnSystem.EnemyType.FLYBEE), levelRequired = 5, damageScale = 1.65f)
    )
    // Save system
    private val saveManager = SaveManager(context)
    private val savedState = saveManager.load()

    private var currentMapIndex = savedState?.let { st ->
        val idxById = maps.indexOfFirst { it.id == st.mapId }
        if (idxById >= 0) idxById else st.mapIndex
    } ?: 0
    private var currentMapId = maps[currentMapIndex].id
    private var map: TileMap = MapLoader.loadFromAssets(context, maps[currentMapIndex].file, maps[currentMapIndex].tileset)
    private var mapRenderer = MapRenderer(map)

    // ---------------- player ----------------
    private val playerW = map.tileSize
    private val playerH = map.tileSize
    // Spawn player: lấy từ spawn point 'player' nếu tồn tại, fallback to fixed tile.
    private var playerX: Float
    private var playerY: Float
    init {
        val spawn = map.getSpawn("player")
        val (tx, ty) = spawn ?: (10 to 5) // fallback
        val (cx, cy) = map.tileCenter(tx, ty)
        // căn body 40x40 ở giữa tileSize
        val (initX, initY) = if (savedState != null) {
            savedState.playerX to savedState.playerY
        } else {
            (cx - 20f) to (cy - 20f)
        }
        playerX = initX
        playerY = initY
    }
    // Nerf player movement speed so enemy có cơ hội áp sát
    private val moveSpeed = 120f // px/s (giảm từ 180)

    // physics
    private var physics = PhysicsSystem(map)
    private val playerBody: PhysicsSystem.Body by lazy {
        PhysicsSystem.Body(
            x = playerX, y = playerY,
            w = 40f, h = 40f
        )
    }


    // animator
    private val spriteScale = 2f
    private val drawW = (playerW * spriteScale).toInt()
    private val drawH = (playerH * spriteScale).toInt()
    private val animator = PlayerAnimator(
        ctx = context,
        basePath = "sprites/player",
        frameW = drawW,
        frameH = drawH
    ).also { it.load() }

    // systems
    private val renderSystem = RenderSystem(mapRenderer, animator)
    private val spawnSystem = SpawnSystem().apply {
        // Startup: if beginning on main map (home) absolutely no spawning
        if (currentMapId == "main") {
            setAutoSpawnEnabled(false)
            disableWaves()
            clear()
        }
        setTileMap(map) // provide map reference for enemy collisions
    }
    private val enemySprites = EnemySpriteLoader(context).apply { loadAll() }
    private val enemyRender = EnemyRenderSystem(spawnSystem, enemySprites)
    private val combatSystem = CombatSystem(spawnSystem)
    // Boss (final map only)
    private val bossSprites = BossSpriteLoader(context)
    private val bossSystem = BossSystem(bossSprites, spawnSystem).apply {
        onPlayerHit = { dmg ->
            if (!playerStats.isDead && uiState != UiState.GAME_OVER) {
                playerStats.damage(dmg)
                if (playerStats.isDead) {
                    uiState = UiState.GAME_OVER
                    println("Player died -> GAME OVER. Nhấn A để hồi sinh về main map, nhấn B để về Start.")
                    gameOverOverlay.show()
                }
            }
        }
        onArmyImpact = { cx, cy, range, dmg ->
            var best: UnitEntity? = null
            var bestD2 = Float.MAX_VALUE
            armySystem.units.forEach { u ->
                if (u.hp <= 0) return@forEach
                val dx = u.x - cx
                val dy = u.y - cy
                val d2 = dx*dx + dy*dy
                if (d2 <= range*range && d2 < bestD2) { bestD2 = d2; best = u }
            }
            best?.let { target ->
                target.hp -= dmg
                if (target.hp < 0) target.hp = 0
            }
        }
        onDeath = {
            uiState = UiState.WINNER
            winnerOverlay.show()
            println("[WINNER] Boss defeated! Nhấn A để về Main Menu")
        }
    }
    // progression & stats
    private val progression = ProgressionManager(intArrayOf(50)).also {
        // Restore progression from save (recompute from totalXp)
        savedState?.let { st ->
            it.reset()
            if (st.totalXp > 0) it.addXp(st.totalXp)
        }
    }
    // Nerf: giảm lại HP player để combat lâu nhưng không quá bất tử
    private val playerStats = PlayerStats(maxHp = 60)

    // economy & army
    // Use ResourceManager defaults so changing its companion constants auto-updates starting values
    private val resources = ResourceManager(
        startGold = savedState?.gold ?: ResourceManager.DEFAULT_START_GOLD,
        startMeat = savedState?.meat ?: ResourceManager.DEFAULT_START_MEAT
    )
    private val projectileSystem = ProjectileSystem()
    private val armySystem = ArmySystem(resources, spawnSystem, projectileSystem)
    private val unlocks = Unlocks().also {
        savedState?.let { st ->
            it.warriorsBought = st.warriorsBought
            it.lancerUnlocked = st.lancerUnlocked
            it.archerUnlocked = st.archerUnlocked
            it.monkUnlocked = st.monkUnlocked
            // Ensure derived unlocks are applied if conditions exceeded
            it.evaluate(progression.totalXp, progression.tier)
        }
    }
    private val shopSystem = ShopSystem(armySystem, unlocks, resources)
    private val unitSprites = UnitSpriteLoader(context).apply { loadAll() }
    private val armyRender = ArmyRenderSystem(armySystem, unitSprites)
    // Sheep farm system
    private val sheepSystem = com.example.empire.game.sheep.SheepSystem(context.assets)
    // Sheep spawn rules (main map only)
    private val sheepSpawnPeriod = 20f
    private val sheepSpawnBatch = 5
    private val sheepMaxAlive = 15
    private var sheepSpawnTimer = sheepSpawnPeriod
    // Farm placement 
    private var sheepFarmLeftFrac = 0.67f   // tỉ lệ bề rộng từ mép trái (0.0..1.0)
    private var sheepFarmTopFrac = 0.6f    // tỉ lệ chiều cao từ mép trên (0.0..1.0)
    private var sheepFarmRightMargin = 64f  // lề phải (px)
    private var sheepFarmBottomMargin = 64f // lề dưới (px)
    private fun setSheepFarmBounds() {
        val farmW = map.mapWidth * map.tileSize
        val farmH = map.mapHeight * map.tileSize
        sheepSystem.farmLeft   = farmW * sheepFarmLeftFrac
        sheepSystem.farmTop    = farmH * sheepFarmTopFrac
        sheepSystem.farmRight  = farmW - sheepFarmRightMargin
        sheepSystem.farmBottom = farmH - sheepFarmBottomMargin
    }
    init {
        spawnSystem.onEnemyAttackImpact = { enemy ->
            armySystem.onEnemyAttackImpact(enemy)
        }
        // Enemy target preference: prefer nearby army units over player
        spawnSystem.queryNearestArmyCenter = { ex, ey, radius ->
            val r2 = radius * radius
            var bx: Float? = null
            var by: Float? = null
            var bestD2 = Float.MAX_VALUE
            armySystem.units.forEach { u ->
                if (u.hp <= 0) return@forEach
                val dx = u.x - ex
                val dy = u.y - ey
                val d2 = dx*dx + dy*dy
                if (d2 <= r2 && d2 < bestD2) {
                    bestD2 = d2
                    bx = u.x
                    by = u.y
                }
            }
            if (bx != null && by != null) bx!! to by!! else null
        }
        // Khởi tạo trang trại cừu (chỉ khi bắt đầu ở main map)
        if (currentMapId == "main") {
            setSheepFarmBounds()
            sheepSystem.load()
            sheepSystem.spawnSheep(5)
            sheepSpawnTimer = sheepSpawnPeriod
        }

        // Restore army from save after systems are ready
        savedState?.army?.takeIf { it.isNotEmpty() }?.let { list ->
            armySystem.setTileMap(map)
            val restored = list.map { su ->
                val type = try { UnitType.valueOf(su.type) } catch (_: Exception) { UnitType.WARRIOR }
                UnitEntity(su.x, su.y, type, UnitStatTable.get(type), hp = su.hp)
            }
            armySystem.restoreUnits(restored)
        }
        // Spawn boss if current is final map
        if (currentMapId == maps.last().id) {
            val spawn = map.getSpawn("boss") ?: map.getSpawn("player") ?: (12 to 8)
            val (tx, ty) = spawn
            val (cx, cy) = map.tileCenter(tx, ty)
            bossSystem.spawnAt(cx - 56f, cy - 56f)
        }
    }

    // HUD paint
    private val hudPaint = Paint().apply {
        color = Color.WHITE
        textSize = 14f
        isAntiAlias = true
    }
    private val hpBarPaint = Paint().apply { color = Color.RED }
    private val hpBarBackPaint = Paint().apply { color = Color.argb(120, 80, 0, 0) }
    private val xpBarPaint = Paint().apply { color = Color.rgb(70,160,255) }
    private val xpBarBackPaint = Paint().apply { color = Color.argb(120, 0, 40, 80) }

    // ---------------- camera & zoom ----------------
    private var camX = 0
    private var camY = 0
    private var scaleFactor = 3.0f

    // ---------------- timing ----------------
    private val targetFPS = 60
    private val frameNs = 1_000_000_000L / targetFPS

    // ---------------- input state ----------------
    @Volatile private var dirX = 0f
    @Volatile private var dirY = 0f
    @Volatile private var atkEdge = false
    @Volatile private var atkHeld = false
    @Volatile private var aEdge = false
    @Volatile private var bEdge = false
    @Volatile private var cEdge = false

    // ================= UI State (Main House prompt & Buy menu) =================
    private enum class UiState { NONE, GAME_OVER, WINNER }
    private var uiState = UiState.NONE
    private var wasInsideMainHouse = false
    private val mapOverlay by lazy { MapSelectOverlay(panelPaint, panelBorder, highlightPaint, hudPaint, uiInset) }
    private val buyOverlay by lazy { BuyMenuOverlay(panelPaint, panelBorder, highlightPaint, hudPaint, uiInset) }
    private val gameOverOverlay by lazy { GameOverOverlay(panelPaint, panelBorder, hudPaint, uiInset) }
    private val winnerOverlay by lazy { com.example.empire.game.ui.overlay.WinnerOverlay(panelPaint, panelBorder, hudPaint, uiInset) }
    private val housePromptOverlay by lazy { HousePromptOverlay(panelPaint, panelBorder, hudPaint, uiInset) }
    // Knockback
    private var kbX = 0f
    private var kbY = 0f
    private var kbTime = 0f
    private val kbDuration = 0.18f
    // UI safe inset (bo góc màn hình) – small shift inward
    private val uiInset = 14f
    private val hudRenderer = HudRenderer(

        context.resources,
        context.assets,
        context.packageName,
        hudPaint,
        hpBarPaint,
        hpBarBackPaint,
        xpBarPaint,
        xpBarBackPaint,
        uiInset,
        context
    )
    private val waveHud by lazy { WaveHud(hudPaint, uiInset) }
    private fun applyKnockback(fromX: Float, fromY: Float) {
        val px = playerX + playerBody.w/2f
        val py = playerY + playerBody.h/2f
        var dx = px - fromX
        var dy = py - fromY
        val len = kotlin.math.sqrt(dx*dx + dy*dy)
        if (len > 0.001f) { dx /= len; dy /= len } else { dx = 0f; dy = -1f }
        val strength = 160f
        kbX = dx * strength
        kbY = dy * strength
        kbTime = kbDuration
    }
    // Helper: tạo rect theo tile (bao gồm cạnh phải & dưới của tx2,ty2)
    private fun tileRect(tx1:Int, ty1:Int, tx2:Int, ty2:Int): RectF {
        val ts = map.tileSize.toFloat()
        return RectF(
            tx1 * ts,
            ty1 * ts,
            (tx2 + 1) * ts,
            (ty2 + 1) * ts
        )
    }

    /**
     * Main House entrance: đúng 1 ô cửa tại tile (8,6).
     * Nếu map thay đổi chỉ cần chỉnh lại hai hằng số dưới.
     */
    private val MAIN_ENT_TX1 = 8
    private val MAIN_ENT_TY1 = 6
    private val MAIN_ENT_TX2 = 8
    private val MAIN_ENT_TY2 = 6
    private val mainHouseEntrance = tileRect(MAIN_ENT_TX1, MAIN_ENT_TY1, MAIN_ENT_TX2, MAIN_ENT_TY2)
    private val panelPaint = Paint().apply { color = Color.argb(180, 20, 20, 24) }
    private val panelBorder = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.WHITE }
    private val highlightPaint = Paint().apply { color = Color.argb(150, 255, 215, 0) }
    // Map switch button (HUD)
    private val mapButtonRect = RectF()
    private val pauseButtonRect = RectF()
    private var mapBtnBitmap: Bitmap? = null
    private var pauseBtnBitmap: Bitmap? = null
    private var mapBtnScaled: Bitmap? = null
    private var pauseBtnScaled: Bitmap? = null
    private var mapBtnScaledW = 0
    private var mapBtnScaledH = 0
    private var pauseBtnScaledW = 0
    private var pauseBtnScaledH = 0
    private var mapBtnPressed = false
    private var pauseBtnPressed = false
    private var paused = false
    var onPauseChange: ((Boolean) -> Unit)? = null // callback ra Compose khi pause đổi từ bên trong
    var onGameOverExit: (() -> Unit)? = null // callback yêu cầu thoát về Start khi ở GAME_OVER và nhấn B
    // Gating: phải clear đủ 2 wave (1 cycle) map trước mới mở map tiếp theo
    private var highestClearedMapIndex = savedState?.highestClearedMapIndex ?: 0

    init {
        holder.addCallback(this)
        isFocusable = true

    // Combat hooks
        combatSystem.tierProvider = { progression.tier }
        combatSystem.onEnemyKilled = { type ->
            val gained = when(type){
                SpawnSystem.EnemyType.SLIME -> 5
                SpawnSystem.EnemyType.FLYBEE -> 8
                SpawnSystem.EnemyType.MONSTER -> 12
                SpawnSystem.EnemyType.WOLF -> 10
            }
            val upgraded = progression.addXp(gained)
            println("Enemy killed: $type +$gained XP (total=${progression.xp})")
            if (upgraded) {
                println("UPGRADE! Attack tier = ${progression.tier}")
                saveGame()
            }
            unlocks.evaluate(progression.totalXp, progression.tier)
        }

        // Khi hoàn thành đủ chu kỳ (2 wave) ở map hiện tại => mở map kế
        spawnSystem.onWaveCycleComplete = {
            if (currentMapIndex > highestClearedMapIndex) {
                highestClearedMapIndex = currentMapIndex
                println("[PROGRESS] Map $currentMapId cleared. highestCleared=$highestClearedMapIndex")
                saveGame()
            }
            // Final victory is handled by boss death; do not auto-win on final wave clear.
            if (currentMapIndex == maps.lastIndex) {
                println("[FINAL] Wave cycle complete on final map. Defeat the Boss to win!")
            }
        }
        // Mỗi khi dọn xong 1 wave: +100 vàng, +30 XP, hồi 20 máu
        spawnSystem.onWaveComplete = { clearedWave, total ->
            resources.addGold(100)
            val leveled = progression.addXp(30)
            playerStats.heal(20)
            println("[WAVE REWARD] Cleared wave $clearedWave/$total -> +100 Gold, +30 XP, +20 HP (HP=${playerStats.hp}/${playerStats.maxHp})")
            if (leveled) {
                println("UPGRADE! Attack tier = ${progression.tier}")
            }
            saveGame()
        }

        // Khi enemy đánh trúng player
        spawnSystem.onPlayerHit = { enemy, dmg ->
            if (!playerStats.isDead && uiState != UiState.GAME_OVER) {
                playerStats.damage(dmg)
                applyKnockback(enemy.x + enemy.w/2f, enemy.y + enemy.h/2f)
                if (playerStats.isDead) {
                    uiState = UiState.GAME_OVER
                    println("Player died -> GAME OVER. Nhấn A để hồi sinh về main map, nhấn B để về Start.")
                    gameOverOverlay.show()
                }
            }
            SfxManager.playEnemyAttack(enemy.type.name)
        }
    spawnSystem.onEnemyDeath = { type -> SfxManager.stopEnemyAttackForType(type.name) }

        // Load map switch button bitmap
        try {
            context.assets.open("ui/Buttons/button.png").use { inS ->
                mapBtnBitmap = BitmapFactory.decodeStream(inS)
            }
            // Load pause base (reuse same or different path). If you have custom PNG replace path below.
            context.assets.open("ui/Buttons/button.png").use { inS ->
                pauseBtnBitmap = BitmapFactory.decodeStream(inS)
            }
        } catch (e: Exception) {
            println("[WARN] Cannot load map switch button asset: ${e.message}")
        }

        // Init unlocks baseline
    unlocks.evaluate(progression.totalXp, progression.tier)

    // (Đã có shop nên bỏ auto-buy debug)
        mapOverlay.listener = object : MapSelectOverlay.Listener {
            override fun onMapSelected(index: Int) { attemptSwitchToMap(index) }
        }
        buyOverlay.listener = object : BuyMenuOverlay.Listener {
            override fun onBuy(type: UnitType) { attemptBuy(type) }
            override fun onClose() { buyOverlay.close() }
        }

        // If we loaded into a non-main map from save, ensure wave config is applied
        if (currentMapId != "main") {
            val target = maps[currentMapIndex]
            spawnSystem.setMapBounds(map.mapWidth * map.tileSize, map.mapHeight * map.tileSize)
            if (target.enemyTypes.isNotEmpty()) {
                spawnSystem.enableWaves(
                    SpawnSystem.WaveConfig(
                        target.enemyTypes,
                        waves = 2,
                        countPerType = 5,
                        cooldownAfter = 20f
                    )
                )
                spawnSystem.setDamageScale(target.damageScale)
                spawnSystem.setAutoSpawnEnabled(false)
            } else {
                spawnSystem.disableWaves()
                spawnSystem.setAutoSpawnEnabled(false)
                spawnSystem.setDamageScale(1f)
            }
            sheepSystem.clearAll()
        }
    }

    // =========================================================
    // Surface lifecycle
    // =========================================================
    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        gameThread = Thread(this, "Empire-GameLoop").also { it.start() }
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) = Unit
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Persist on exit
        saveGame()
        running = false
        gameThread?.join(500)
        gameThread = null
    }

    // =========================================================
    // Main loop
    // =========================================================
    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last).coerceAtLeast(0)) / 1_000_000_000f
            last = now

            update(dt)
            holder.lockCanvas()?.let { c ->
                try { drawGame(c) } finally { holder.unlockCanvasAndPost(c) }
            }

            val sleep = frameNs - (System.nanoTime() - now)
            if (sleep > 0) try {
                Thread.sleep(sleep / 1_000_000, (sleep % 1_000_000).toInt())
            } catch (_: InterruptedException) {}
        }
    }

    // =========================================================
    // Update – đã tách physics/combat/spawn
    // =========================================================
    private fun update(dt: Float) {
    if (paused) return // freeze everything while paused (still draws)

    // normalize input dir only when there's actual movement
        var vx = dirX; var vy = dirY
        if (abs(vx) > 0.0001f || abs(vy) > 0.0001f) { // Avoid division by zero
            val l = sqrt(vx*vx + vy*vy)
            if (l > 0.0001f) { // Extra safety check
                vx /= l; vy /= l
            }
        }

        if (kbTime > 0f) {
            kbTime -= dt
            val t = (kbTime / kbDuration).coerceIn(0f,1f)
            playerBody.vx = kbX * t
            playerBody.vy = kbY * t
        } else if (uiState == UiState.GAME_OVER || uiState == UiState.WINNER) {
            playerBody.vx = 0f; playerBody.vy = 0f
        } else {
            // Chuyển input sang velocity cho physics
            playerBody.vx = vx * moveSpeed
            playerBody.vy = vy * moveSpeed
        }

        // Attack
        if (atkEdge) {
            combatSystem.startAttack()
            animator.kind = AnimKind.ATTACK1
            atkEdge = false
        }

        // (Bỏ debug log tile/collision cho nhẹ log)

        // Apply physics
        physics.step(playerBody, dt)

        // Update player position from physics
        playerX = playerBody.x
        playerY = playerBody.y

        // animator state (idle/run)
        if (combatSystem.attackTimer <= 0f && uiState != UiState.GAME_OVER) {
            animator.kind = if (dirX != 0f || dirY != 0f) AnimKind.RUN else AnimKind.IDLE
        }
        // (Chưa đổi animation attack2 ở phần này – sẽ làm sau) 
        if (dirX != 0f || dirY != 0f) {
            animator.facing = when {
                abs(dirX) >= abs(dirY) && dirX < 0f -> Facing.Left
                abs(dirX) >= abs(dirY) && dirX > 0f -> Facing.Right
                dirY < 0f -> Facing.Up
                else      -> Facing.Down
            }
        }
        animator.update(dt)

        // spawn & combat update
        if (currentMapId == "main") {
            // Hard safety: even if something accidentally spawned, wipe it
            if (spawnSystem.enemies.isNotEmpty()) spawnSystem.clear()
        }
    val pxc = playerX + playerBody.w/2f
    val pyc = playerY + playerBody.h/2f
    spawnSystem.setPlayerTarget(pxc, pyc)
    bossSystem.setPlayerTarget(pxc, pyc)
        spawnSystem.update(dt)
        enemyRender.update(dt)
    combatSystem.update(dt, playerX, playerY, playerBody.w, playerBody.h)
    bossSystem.update(dt)
        // Player attack -> làm damage lên cừu + boss trong range nếu đang vung kiếm (attackTimer > 0)
        if (combatSystem.attackTimer > 0f) {
            val cxAtk = playerX + playerBody.w/2f
            val cyAtk = playerY + playerBody.h/2f
            sheepSystem.damageSheep(cxAtk, cyAtk, combatSystem.attackRange, 1) { _, _ -> }
            bossSystem.boss?.let { b ->
                val bx = b.x + b.w/2f
                val by = b.y + b.h/2f
                val dx = bx - cxAtk
                val dy = by - cyAtk
                val d2 = dx*dx + dy*dy
                val r = combatSystem.attackRange
                if (d2 <= r*r) {
                    val killed = bossSystem.applyDamage(1)
                    if (killed) { uiState = UiState.WINNER; winnerOverlay.show() }
                }
            }
        }
        armySystem.update(dt, playerX + playerBody.w/2f, playerY + playerBody.h/2f)
        armySystem.postUpdate()
    armyRender.update(dt)
        // Sheep update and periodic spawn only on main map
        if (currentMapId == "main") {
            sheepSpawnTimer -= dt
            if (sheepSpawnTimer <= 0f) {
                val alive = sheepSystem.aliveCount()
                val canAdd = (sheepMaxAlive - alive).coerceAtLeast(0).coerceAtMost(sheepSpawnBatch)
                if (canAdd > 0) sheepSystem.spawnSheep(canAdd)
                sheepSpawnTimer = sheepSpawnPeriod
            }
            sheepSystem.update(dt)
        }
        // Thu thập thịt nếu player đi qua
        val pcx = playerX + playerBody.w/2f
        val pcy = playerY + playerBody.h/2f
        sheepSystem.tryCollect(pcx, pcy, 34f) { pieces ->
            resources.addMeat(pieces)
            saveGame()
        }
        // Projectiles
        projectileSystem.update(dt)
        handleProjectileCollisions()

        // UI region detection (only if not in buy menu)
        val playerCenterX = playerX + playerBody.w/2f
        val playerCenterY = playerY + playerBody.h/2f
        val inside = currentMapId == "main" && mainHouseEntrance.contains(playerCenterX, playerCenterY)
        if (currentMapId == "main") {
            if (inside && !wasInsideMainHouse && uiState == UiState.NONE) housePromptOverlay.show()
            if (!inside) housePromptOverlay.hide()
        } else housePromptOverlay.hide()
        wasInsideMainHouse = inside

        // Nếu đang ở menu mua lính => khóa movement (giữ nhân vật đứng yên)
        if (buyOverlay.visible || uiState == UiState.GAME_OVER) {
            playerBody.vx = 0f; playerBody.vy = 0f
        }

        // Camera follows player
        if (width > 0 && height > 0) {
            val vw = (width / scaleFactor).toInt()
            val vh = (height / scaleFactor).toInt()
            val mapPxW = map.mapWidth * map.tileSize
            val mapPxH = map.mapHeight * map.tileSize

            // center camera on player
            camX = (playerX - vw / 2f).toInt()
            camY = (playerY - vh / 2f).toInt()

            // clamp camera to map bounds
            if (camX < 0) camX = 0
            if (camY < 0) camY = 0
            if (camX > mapPxW - vw) camX = mapPxW - vw
            if (camY > mapPxH - vh) camY = mapPxH - vh
        }

        // reset 1-shot edges
        aEdge = false; bEdge = false; cEdge = false
    }

    // =========================================================
    // Draw – gọi RenderSystem
    // =========================================================
    private fun drawGame(canvas: Canvas) {
        // Clear screen with solid black
        canvas.drawRGB(0, 0, 0)

        val vw = (width / scaleFactor).toInt()
        val vh = (height / scaleFactor).toInt()

        // Only draw once per frame
        renderSystem.draw(
            canvas = canvas,
            camX = camX, camY = camY,
            vw = vw, vh = vh,
            scaleFactor = scaleFactor,
            playerX = playerX, playerY = playerY,
            playerW = playerW, playerH = playerH,
            spriteScale = spriteScale
        )
    // Enemies
    enemyRender.draw(canvas, camX, camY, scaleFactor)
    // Boss
    bossSystem.draw(canvas, camX, camY, scaleFactor)
        // Army
    armyRender.draw(canvas, camX, camY, scaleFactor)
    drawSheep(canvas)
    drawProjectiles(canvas)

        // HUD (vẽ sau cùng, không scale theo world)
        drawHud(canvas)
    }

    private fun drawHud(canvas: Canvas) {
        if (!paused) {
            hudRenderer.draw(
                canvas,
                playerStats,
                progression,
                resources,
                armySystem,
                currentMapId,
                width,
                height
            )
        }

        // (old small map button block removed – replaced by unified larger map & pause buttons block below)
        // --- Map & Pause buttons layout (bigger) ---
        val rawMap = mapBtnBitmap
        val rawPause = pauseBtnBitmap
        val targetH = 72 // bigger buttons
        if (rawMap != null) {
            val desiredW = (rawMap.width * (targetH.toFloat() / rawMap.height)).toInt()
            if (mapBtnScaled == null || mapBtnScaledW != desiredW || mapBtnScaledH != targetH) {
                mapBtnScaled = Bitmap.createScaledBitmap(rawMap, desiredW, targetH, true)
                mapBtnScaledW = desiredW; mapBtnScaledH = targetH
            }
        }
        if (rawPause != null) {
            val desiredW = (rawPause.width * (targetH.toFloat() / rawPause.height)).toInt()
            if (pauseBtnScaled == null || pauseBtnScaledW != desiredW || pauseBtnScaledH != targetH) {
                pauseBtnScaled = Bitmap.createScaledBitmap(rawPause, desiredW, targetH, true)
                pauseBtnScaledW = desiredW; pauseBtnScaledH = targetH
            }
        }
        val marginR = 12f + uiInset
        val marginT = 12f + uiInset
        val spacing = 16f
        val mapW = (mapBtnScaledW.takeIf { it>0 } ?: 120).toFloat()
        val mapH = (mapBtnScaledH.takeIf { it>0 } ?: targetH).toFloat()
        val pauseW = (pauseBtnScaledW.takeIf { it>0 } ?: 120).toFloat()
        val pauseH = (pauseBtnScaledH.takeIf { it>0 } ?: targetH).toFloat()
        mapButtonRect.set(width - mapW - marginR, marginT, width - marginR, marginT + mapH)
        pauseButtonRect.set(width - mapW - pauseW - spacing - marginR, marginT, width - mapW - spacing - marginR, marginT + pauseH)
        mapBtnScaled?.let { canvas.drawBitmap(it, mapButtonRect.left, mapButtonRect.top, null) }
        pauseBtnScaled?.let { canvas.drawBitmap(it, pauseButtonRect.left, pauseButtonRect.top, null) }
// overlay pressed shade
        if (mapBtnPressed) canvas.drawRect(mapButtonRect, Paint().apply { color = Color.argb(90,0,0,0) })
        if (pauseBtnPressed) canvas.drawRect(pauseButtonRect, Paint().apply { color = Color.argb(90,0,0,0) })

// --- Vẽ icon chuyển đổi cho nút MAP ---
        val iconPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 5f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val cx = mapButtonRect.centerX()
        val cy = mapButtonRect.centerY()
        val radius = mapButtonRect.width() * 0.25f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Mũi tên cong ở trên
        canvas.drawArc(arcRect, 270f, 135f, false, iconPaint)
        val angle1 = Math.toRadians(45.0)
        val arrowX1 = cx + (radius * Math.cos(angle1)).toFloat()
        val arrowY1 = cy + (radius * Math.sin(angle1)).toFloat()
        canvas.drawLine(arrowX1, arrowY1, arrowX1 - 15f, arrowY1 - 5f, iconPaint)
        canvas.drawLine(arrowX1, arrowY1, arrowX1 - 5f, arrowY1 - 15f, iconPaint)

        // Mũi tên cong ở dưới
        canvas.drawArc(arcRect, 90f, 135f, false, iconPaint)
        val angle2 = Math.toRadians(225.0)
        val arrowX2 = cx + (radius * Math.cos(angle2)).toFloat()
        val arrowY2 = cy + (radius * Math.sin(angle2)).toFloat()
        canvas.drawLine(arrowX2, arrowY2, arrowX2 + 15f, arrowY2 + 5f, iconPaint)
        canvas.drawLine(arrowX2, arrowY2, arrowX2 + 5f, arrowY2 + 15f, iconPaint)
        // Pause icon (two rectangles) drawn over pause button
        val barW = pauseButtonRect.width()*0.18f
        val gap = barW*0.6f
        val barH = pauseButtonRect.height()*0.55f
        val barTop = pauseButtonRect.centerY() - barH/2f
        val barLeft1 = pauseButtonRect.centerX() - gap/2f - barW
        val barLeft2 = pauseButtonRect.centerX() + gap/2f
        canvas.drawRoundRect(RectF(barLeft1, barTop, barLeft1+barW, barTop+barH), 6f,6f, iconPaint)
        canvas.drawRoundRect(RectF(barLeft2, barTop, barLeft2+barW, barTop+barH), 6f,6f, iconPaint)
        hudPaint.textSize = 14f

        mapOverlay.draw(
            canvas,
            maps.map { MapSelectOverlay.Entry(it.id, it.levelRequired, it.enemyTypes, it.damageScale) },
            progression.tier + 1,
            highestClearedMapIndex,
            currentMapIndex
        )
        buyOverlay.draw(canvas, resources, unlocks)
    if (uiState == UiState.GAME_OVER) gameOverOverlay.draw(canvas)
    if (uiState == UiState.WINNER) winnerOverlay.draw(canvas, context.assets)
    housePromptOverlay.draw(canvas)
    waveHud.draw(
            canvas,
            spawnSystem.isWaveMode(),
            spawnSystem.currentWave(),
            spawnSystem.totalWaves(),
            spawnSystem.cooldownRemaining(),
            spawnSystem.inCycleCooldown(),
            context.assets
        )
        // Canvas based pause menu removed (Compose McButton overlay handles pause UI)
        // When paused we simply freeze gameplay; Compose layer will draw menu.

    // (Debug entrance overlay đã bỏ)
    }

    // Removed non-Compose pause menu + navigation logic. Compose layer will call setPausedFromCompose(false) to resume.

    // Developer helper removed: buying is handled via BuyMenuOverlay.

    // Public UI bridge: toggle map selection overlay (used by Compose ControlsOverlay C button)
    fun toggleMapOverlay() { mapOverlay.toggle() }

    private fun attemptBuy(type: UnitType) {
        val px = playerX + playerBody.w/2f
        val py = playerY + playerBody.h/2f
        val success = shopSystem.buy(type, px, py)
        if (success) {
            println("[SHOP] Bought $type -> Gold=${resources.gold}, Meat=${resources.meat}")
            unlocks.evaluate(progression.totalXp, progression.tier)
            saveGame()
        } else {
            val can = shopSystem.canBuy(type)
            val aff = shopSystem.affordable(type)
            val cost = com.example.empire.game.economy.CostTable.get(type)
            val reason = when {
                !can -> "LOCKED"
                !aff -> "NOT_ENOUGH (need G${cost.gold} M${cost.meat}, have G${resources.gold} M${resources.meat})"
                else -> "FAILED"
            }
            println("[SHOP] FAIL $type -> $reason")
        }
    }

    // =========================================================
    // Public API (Overlay gọi)
    // =========================================================
    fun setDirection(dir: Direction) {
        // If an overlay is open, interpret direction as navigation instead of movement.
        if (mapOverlay.visible) {
            when (dir) {
                Direction.Up -> mapOverlay.navigate(-1, maps.size)
                Direction.Down -> mapOverlay.navigate(1, maps.size)
                else -> {}
            }
            return
        }
        if (buyOverlay.visible) {
            when (dir) {
                Direction.Up -> buyOverlay.navigate(-1)
                Direction.Down -> buyOverlay.navigate(1)
                else -> {}
            }
            return
        }
        // Reset current direction first
        dirX = 0f
        dirY = 0f

        // Set new direction
        when (dir) {
            Direction.Left -> dirX = -1f
            Direction.Right -> dirX = 1f
            Direction.Up -> dirY = -1f
            Direction.Down -> dirY = 1f
            else -> { /* Do nothing for other cases */ }
        }
    }
    fun stopMove() { dirX = 0f; dirY = 0f }

    fun pressAttack()  { atkEdge = true; atkHeld = true; SfxManager.playPlayerAttack() }
    fun releaseAttack(){ atkHeld = false }

    fun pressA() { handleButtonA() }
    fun releaseA() {}
    fun pressB() { handleButtonB() }
    fun releaseB() {}
    fun pressC() {}
    fun releaseC() {}

    // ================= Input overrides for UI =================
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_BUTTON_A -> { handleButtonA(); return true }
            KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK -> { handleButtonB(); return true }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                if (mapOverlay.visible) mapOverlay.navigate(-1, maps.size) else if (buyOverlay.visible) buyOverlay.navigate(-1) else setDirection(Direction.Up)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                if (mapOverlay.visible) mapOverlay.navigate(1, maps.size) else if (buyOverlay.visible) buyOverlay.navigate(1) else setDirection(Direction.Down)
                return true
            }
            KeyEvent.KEYCODE_C -> { return true } // vô hiệu hóa C
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePauseInternal(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun pressA(action: Boolean = true) { /* placeholder if overlay calls with param */ }

    // Replace empty handlers with UI logic versions
    fun handleButtonA() {
        when (uiState) {
            UiState.NONE -> {
                if (paused) { return } // Pause handled by Compose overlay
                when {
                    housePromptOverlay.visible -> { housePromptOverlay.hide(); buyOverlay.open() }
                    mapOverlay.visible -> mapOverlay.confirm(maps.size)
                    buyOverlay.visible -> buyOverlay.confirm(resources, unlocks)
                }
            }
            UiState.GAME_OVER -> respawnToMain()
            UiState.WINNER -> { onGameOverExit?.invoke() }
        }
    }

    fun handleButtonB() {
        when (uiState) {
            UiState.NONE -> {
                when {
                    buyOverlay.visible -> buyOverlay.close()
                    mapOverlay.visible -> mapOverlay.hide()
                    housePromptOverlay.visible -> housePromptOverlay.hide()
                }
            }
            UiState.GAME_OVER -> { onGameOverExit?.invoke() }
            UiState.WINNER -> { onGameOverExit?.invoke() }
        }
    }

    private fun respawnToMain() {
        // Chuyển về main map và hồi sinh tại spawn, giữ nguyên vàng & thịt (không đụng ResourceManager)
        val main = maps.first()
        currentMapIndex = 0
        currentMapId = main.id
        map = MapLoader.loadFromAssets(context, main.file, main.tileset)
        mapRenderer = MapRenderer(map)
        renderSystem.setRenderer(mapRenderer)
        physics = PhysicsSystem(map)
        spawnSystem.clear()
        spawnSystem.setTileMap(map)
        spawnSystem.disableWaves()
        spawnSystem.setAutoSpawnEnabled(false)
        spawnSystem.setDamageScale(1f)
        spawnSystem.setMapBounds(map.mapWidth * map.tileSize, map.mapHeight * map.tileSize)
    // Ensure boss is removed when returning to main map
    bossSystem.clear()
        armySystem.setTileMap(map)
        armySystem.clear()
        // Setup sheep farm again
        setSheepFarmBounds()
        sheepSystem.load()
        if (sheepSystem.sheep.isEmpty()) sheepSystem.spawnSheep(5)
        sheepSpawnTimer = sheepSpawnPeriod

        // Reset stats
        playerStats.reset()
        val (tx, ty) = map.getSpawn("player") ?: (10 to 5)
        val (cx, cy) = map.tileCenter(tx, ty)
        playerX = cx - 20f
        playerY = cy - 20f
        playerBody.x = playerX
        playerBody.y = playerY
        // Clear knockback & UI
        kbTime = 0f; kbX = 0f; kbY = 0f
        gameOverOverlay.hide()
        winnerOverlay.hide()
        uiState = UiState.NONE
        println("[RESPAWN] Hồi sinh về main map. HP=${playerStats.hp}, giữ nguyên tài nguyên.")
        saveGame()
    }

    // Áp dụng trạng thái vừa load từ save slot khi đang ở giữa game
    fun applyLoadedState(st: SaveManager.SaveState) {
        // Map selection by id preferred
        val idxById = maps.indexOfFirst { it.id == st.mapId }
        currentMapIndex = if (idxById >= 0) idxById else st.mapIndex.coerceIn(0, maps.lastIndex)
        currentMapId = maps[currentMapIndex].id
        val target = maps[currentMapIndex]
        map = MapLoader.loadFromAssets(context, target.file, target.tileset)
        mapRenderer = MapRenderer(map)
        renderSystem.setRenderer(mapRenderer)
        physics = PhysicsSystem(map)

        // Waves / sheep per map
        spawnSystem.clear()
        spawnSystem.setTileMap(map)
        spawnSystem.setMapBounds(map.mapWidth * map.tileSize, map.mapHeight * map.tileSize)
        if (currentMapId == "main") {
            spawnSystem.disableWaves(); spawnSystem.setAutoSpawnEnabled(false); spawnSystem.setDamageScale(1f)
            setSheepFarmBounds(); sheepSystem.load(); if (sheepSystem.sheep.isEmpty()) sheepSystem.spawnSheep(5); sheepSpawnTimer = sheepSpawnPeriod
            // Boss cannot exist on main map
            bossSystem.clear()
        } else {
            sheepSystem.clearAll()
            if (target.enemyTypes.isNotEmpty()) {
                spawnSystem.enableWaves(SpawnSystem.WaveConfig(target.enemyTypes, waves = 2, countPerType = 5, cooldownAfter = 20f))
                spawnSystem.setDamageScale(target.damageScale)
                spawnSystem.setAutoSpawnEnabled(false)
            } else {
                spawnSystem.disableWaves(); spawnSystem.setAutoSpawnEnabled(false); spawnSystem.setDamageScale(1f)
            }
            // Spawn boss only on final map
            if (currentMapId == maps.last().id) {
                val bspawn = map.getSpawn("boss") ?: map.getSpawn("player") ?: (12 to 8)
                val (tbx, tby) = bspawn
                val (cbx, cby) = map.tileCenter(tbx, tby)
                bossSystem.spawnAt(cbx - 56f, cby - 56f)
            } else {
                bossSystem.clear()
            }
        }

        // Progression & unlocks
        progression.reset(); if (st.totalXp > 0) progression.addXp(st.totalXp)
        unlocks.warriorsBought = st.warriorsBought
        unlocks.lancerUnlocked = st.lancerUnlocked
        unlocks.archerUnlocked = st.archerUnlocked
        unlocks.monkUnlocked = st.monkUnlocked
        unlocks.evaluate(progression.totalXp, progression.tier)

        // Resources
        resources.setResources(st.gold, st.meat)

        // Player position
        playerX = st.playerX; playerY = st.playerY
        playerBody.x = playerX; playerBody.y = playerY

        // Army restore
        val restored = st.army.map { su ->
            val type = try { UnitType.valueOf(su.type) } catch (_: Exception) { UnitType.WARRIOR }
            UnitEntity(su.x, su.y, type, UnitStatTable.get(type), hp = su.hp)
        }
        armySystem.setTileMap(map)
        armySystem.restoreUnits(restored)

        // Clear UI overlays & states
        mapOverlay.hide(); buyOverlay.close(); gameOverOverlay.hide(); winnerOverlay.hide()
        uiState = UiState.NONE
        kbTime = 0f; kbX = 0f; kbY = 0f
        highestClearedMapIndex = st.highestClearedMapIndex
        println("[LOAD] Áp dụng save: map=${st.mapId} lv=${progression.tier+1} gold=${resources.gold} meat=${resources.meat}")
    }

    // Expose for overlay binding (call these instead of pressA/pressB old)
    fun pressA_new() { handleButtonA() }
    fun pressB_new() { handleButtonB() }

    // Direction reuse for menu navigation
    fun navigateMenu(dx: Int, dy: Int) {
        if (mapOverlay.visible) {
            mapOverlay.navigate(dy, maps.size)
            return
        }
        if (buyOverlay.visible) {
            buyOverlay.navigate(dy)
        }
    }

    // selectionToType removed (handled in BuyMenuOverlay)

    // (legacy drawPrompt/drawBuyMenu/drawGameOver removed)

    // (Removed debug zone drawing code)

    // ================= Projectiles =================
    private val projPaint = Paint().apply { color = Color.YELLOW } // fallback
    private var arrowBitmap: Bitmap? = null
    private fun ensureArrowBitmap() {
        if (arrowBitmap != null) return
        // Try load from Archer frames (first available)
        try {
            val frames = unitSprites.load(UnitType.ARCHER)
            arrowBitmap = frames.arrow
        } catch (_: Exception) { /* ignore */ }
    }
    private fun drawProjectiles(canvas: Canvas) {
        if (projectileSystem.projectiles.isEmpty()) return
        ensureArrowBitmap()
        val arrow = arrowBitmap
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        projectileSystem.projectiles.forEach { p ->
            val sx = (p.x - camX).toFloat()
            val sy = (p.y - camY).toFloat()
            if (arrow != null) {
                // Determine rotation based on velocity
                val angle = kotlin.math.atan2(p.vy, p.vx)
                val scale = 0.55f // thu nhỏ mũi tên
                val hw = (arrow.width * scale) / 2f
                val hh = (arrow.height * scale) / 2f
                val src = android.graphics.Rect(0,0,arrow.width,arrow.height)
                val dst = android.graphics.Rect((sx - hw).toInt(), (sy - hh).toInt(), (sx + hw).toInt(), (sy + hh).toInt())
                canvas.save()
                canvas.translate(sx, sy)
                canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
                canvas.translate(-sx, -sy)
                canvas.drawBitmap(arrow, src, dst, null)
                canvas.restore()
            } else {
                canvas.drawRect(sx-2, sy-2, sx+2, sy+2, projPaint)
            }
        }
        canvas.restore()
    }

    private fun drawSheep(canvas: Canvas) {
        if (currentMapId != "main") return
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        val paint = Paint()
        sheepSystem.sheep.forEach { s ->
            val bmp = sheepSystem.currentFrame(s)
            if (bmp != null) {
                val dx = s.x - camX - bmp.width/2f
                val dy = s.y - camY - bmp.height
                canvas.drawBitmap(bmp, dx, dy, paint)
            }
        }
        // meat drops
        val meatBmp = sheepSystem.meatBitmap()
        if (meatBmp != null) {
            val scale = 0.5f
            val w = meatBmp.width * scale
            val h = meatBmp.height * scale
            sheepSystem.meatDrops.forEach { d ->
                if (!d.collected) {
                    val left = d.x - camX - w/2f
                    val top = d.y - camY - h/2f
                    val dst = android.graphics.RectF(left, top, left + w, top + h)
                    canvas.drawBitmap(meatBmp, null, dst, paint)
                }
            }
        }
        canvas.restore()
    }

    private fun handleProjectileCollisions() {
        val enemies = spawnSystem.enemies
        if (projectileSystem.projectiles.isEmpty()) return
        projectileSystem.projectiles.forEach { p ->
            if (!p.alive) return@forEach
            // Check hits on regular enemies
            enemies.forEach { e ->
                if (!e.alive || e.state == SpawnSystem.Enemy.State.DEAD) return@forEach
                val ex1 = e.x
                val ey1 = e.y
                val ex2 = e.x + e.w
                val ey2 = e.y + e.h
                if (p.x >= ex1 && p.x <= ex2 && p.y >= ey1 && p.y <= ey2) {
                    val killed = spawnSystem.applyDamage(e, p.damage)
                    if (killed) combatSystem.onEnemyKilled(e.type)
                    p.alive = false
                }
            }
            // Check boss collision
            bossSystem.boss?.let { b ->
                if (!p.alive) return@let
                val bx1 = b.x
                val by1 = b.y
                val bx2 = b.x + b.w
                val by2 = b.y + b.h
                if (p.x >= bx1 && p.x <= bx2 && p.y >= by1 && p.y <= by2) {
                    val killed = bossSystem.applyDamage(p.damage)
                    if (killed) { uiState = UiState.WINNER; winnerOverlay.show() }
                    p.alive = false
                }
            }
        }
        if (enemies.isNotEmpty()) spawnSystem.removeDead()
    }

    // ================= Map Switching =================
    // Legacy map selection helpers removed (handled by MapSelectOverlay + attemptSwitchToMap(index))

    // Debug / helper: spawn some FlyBee around player
    fun debugSpawnBees(count: Int = 5) {
        val cx = playerX + playerBody.w/2f
        val cy = playerY + playerBody.h/2f
        repeat(count) {
            val offX = (-80..80).random()
            val offY = (-80..80).random()
            spawnSystem.spawnEnemy(cx + offX, cy + offY, SpawnSystem.EnemyType.FLYBEE)
        }
        println("Spawned $count FlyBee around player")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mapButtonRect.contains(event.x, event.y)) { mapBtnPressed = true; invalidate(); return true }
                if (pauseButtonRect.contains(event.x, event.y)) { pauseBtnPressed = true; invalidate(); return true }
                // Paused state handled by Compose; no touch targets here when paused.
            }
            MotionEvent.ACTION_UP -> {
                if (mapBtnPressed && mapButtonRect.contains(event.x, event.y)) { mapOverlay.toggle(); }
                if (pauseBtnPressed && pauseButtonRect.contains(event.x, event.y)) { togglePauseInternal() }
                mapBtnPressed = false; pauseBtnPressed = false; invalidate();
            }
            MotionEvent.ACTION_CANCEL -> { mapBtnPressed = false; pauseBtnPressed = false }
        }
        return super.onTouchEvent(event)
    }

    private fun attemptSwitchToMap(index: Int) {
        val target = maps.getOrNull(index) ?: return
        val playerLevel = progression.tier + 1
        if (playerLevel < target.levelRequired) {
            println("[MAP] Locked. Need level ${target.levelRequired}, current $playerLevel")
            return
        }
        // Gating theo wave clear: chỉ cho vào map (highestCleared + 1) hoặc quay lại map cũ
        if (index > highestClearedMapIndex + 1) {
            println("[MAP] Locked. Clear previous map waves first (cleared up to index=$highestClearedMapIndex)")
            return
        }
    currentMapIndex = index
        currentMapId = target.id
        println("[MAP] Switching to $currentMapId")
        map = MapLoader.loadFromAssets(context, target.file, target.tileset)
        mapRenderer = MapRenderer(map)
        renderSystem.setRenderer(mapRenderer)
        physics = PhysicsSystem(map)
        spawnSystem.setTileMap(map)
        armySystem.setTileMap(map)
    spawnSystem.clear()
    armySystem.clearDamagePopups()
        spawnSystem.setMapBounds(map.mapWidth * map.tileSize, map.mapHeight * map.tileSize)
        if (currentMapId == "main") {
            setSheepFarmBounds()
            sheepSystem.load()
            if (sheepSystem.sheep.isEmpty()) sheepSystem.spawnSheep(5)
            sheepSpawnTimer = sheepSpawnPeriod
            // No boss on main map
            bossSystem.clear()
        } else {
            // Ensure no sheep exist outside main map
            sheepSystem.clearAll()
        }
        if (target.enemyTypes.isNotEmpty()) {
            spawnSystem.enableWaves(SpawnSystem.WaveConfig(target.enemyTypes, waves = 2, countPerType = 5, cooldownAfter = 20f))
            spawnSystem.setDamageScale(target.damageScale)
            spawnSystem.setAutoSpawnEnabled(false)
        } else {
            spawnSystem.disableWaves()
            spawnSystem.setAutoSpawnEnabled(false)
            spawnSystem.setDamageScale(1f)
        }
        // Boss spawn/clear by map
        if (currentMapId == maps.last().id) {
            val bspawn = map.getSpawn("boss") ?: map.getSpawn("player") ?: (12 to 8)
            val (tbx, tby) = bspawn
            val (cbx, cby) = map.tileCenter(tbx, tby)
            bossSystem.spawnAt(cbx - 56f, cby - 56f)
        } else {
            bossSystem.clear()
        }
        val spawn = map.getSpawn("player") ?: (10 to 5)
        val (tx, ty) = spawn
        val (pcx, pcy) = map.tileCenter(tx, ty)
        playerX = pcx - 20f
        playerY = pcy - 20f
        playerBody.x = playerX
        playerBody.y = playerY
        armySystem.units.forEachIndexed { idxU, u ->
            val angle = (idxU.toFloat() / (armySystem.units.size.coerceAtLeast(1))) * (Math.PI * 2.0).toFloat()
            val radius = 80f
            u.x = playerX + 20f + kotlin.math.cos(angle) * radius
            u.y = playerY + 20f + kotlin.math.sin(angle) * radius
        }
        mapOverlay.hide()
        saveGame()
    }

    fun setPausedFromCompose(p: Boolean) { // gọi từ Compose -> không callback để tránh vòng lặp
        paused = p
    }

    private fun togglePauseInternal() {
        paused = !paused
        onPauseChange?.invoke(paused)
        if (paused) saveGame()
    }
    fun forcePause(value: Boolean) {
        if (paused != value) {
            paused = value
            onPauseChange?.invoke(paused)
        }
    }

    // Persist current run state to SharedPreferences (JSON) via SaveManager
    private fun saveGame() {
        try {
            // Capture army snapshot
            val armySnapshot = armySystem.units.map { u ->
                com.example.empire.data.SaveManager.SaveUnit(
                    type = u.type.name,
                    x = u.x,
                    y = u.y,
                    hp = u.hp
                )
            }
            val state = SaveManager.SaveState(
                mapIndex = currentMapIndex,
                mapId = currentMapId,
                playerX = playerX,
                playerY = playerY,
                gold = resources.gold,
                meat = resources.meat,
                tier = progression.tier,
                xp = progression.xp,
                totalXp = progression.totalXp,
                highestClearedMapIndex = highestClearedMapIndex,
                warriorsBought = unlocks.warriorsBought,
                lancerUnlocked = unlocks.lancerUnlocked,
                archerUnlocked = unlocks.archerUnlocked,
                monkUnlocked = unlocks.monkUnlocked,
                army = armySnapshot
            )
            saveManager.save(state)
            println("[SAVE] Game saved: map=${state.mapId} idx=${state.mapIndex} gold=${state.gold} meat=${state.meat} xp=${state.totalXp}")
        } catch (e: Exception) {
            println("[SAVE][ERR] ${e.message}")
        }
    }
}
