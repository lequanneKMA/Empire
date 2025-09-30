package com.example.empire.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.empire.game.map.MapLoader
import com.example.empire.game.map.MapRenderer
import com.example.empire.game.map.TileMap
import com.example.empire.core.gfx.PlayerAnimator
import com.example.empire.core.gfx.AnimKind
import com.example.empire.core.gfx.Facing
import com.example.empire.game.ecs.systems.PhysicsSystem
import com.example.empire.game.ecs.systems.RenderSystem
import com.example.empire.game.ecs.systems.SpawnSystem
import com.example.empire.game.ecs.systems.CombatSystem
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.abs

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    // ---------------- runtime ----------------
    @Volatile private var running = false
    private var gameThread: Thread? = null

    // ---------------- world / map ----------------
    private val map = MapLoader.loadFromAssets(context, "maps/map_main.json", "maps/tileset.png")
    private val mapRenderer = MapRenderer(map)

    // ---------------- player ----------------
    private val playerW = map.tileSize
    private val playerH = map.tileSize
    private var playerX = 1 * map.tileSize + (map.tileSize - 40) / 2f
    private var playerY = 1 * map.tileSize + (map.tileSize - 40) / 2f
    private val moveSpeed = 180f // px/s

    // physics
    private val physics = PhysicsSystem(map)
    private val playerBody = PhysicsSystem.Body(
        x = playerX, y = playerY,
        w = 40f, h = 40f
    )


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
        // spawn vài con thử cho vui – m tự chỉnh toạ độ/type
        spawnEnemy(playerX + 128f, playerY, SpawnSystem.EnemyType.WARRIOR)
        spawnEnemy(playerX - 160f, playerY, SpawnSystem.EnemyType.ARCHER)
        spawnEnemy(playerX + 64f,  playerY + 64f, SpawnSystem.EnemyType.PAWN)
    }
    private val combatSystem = CombatSystem(spawnSystem)

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

    init {
        holder.addCallback(this)
        isFocusable = true
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
        // normalize input dir only when there's actual movement
        var vx = dirX; var vy = dirY
        if (abs(vx) > 0.0001f || abs(vy) > 0.0001f) { // Avoid division by zero
            val l = sqrt(vx*vx + vy*vy)
            if (l > 0.0001f) { // Extra safety check
                vx /= l; vy /= l
            }
        }

        // Chuyển input sang velocity cho physics
        playerBody.vx = vx * moveSpeed
        playerBody.vy = vy * moveSpeed

        // Attack
        if (atkEdge) {
            combatSystem.startAttack()
            animator.kind = AnimKind.ATTACK1
            atkEdge = false
        }

        // Debug: Print current tile position before physics
        val preTx = (playerX / map.tileSize).toInt()
        val preTy = (playerY / map.tileSize).toInt()
        println("Pre-physics: Player at tile ($preTx, $preTy)")
        println("Collision map around player:")
        for (y in preTy-2..preTy+2) {
            var line = ""
            for (x in preTx-2..preTx+2) {
                line += if (map.isSolidAt(x, y)) "X" else "."
            }
            println(line)
        }

        // Apply physics
        physics.step(playerBody, dt)

        // Update player position from physics
        playerX = playerBody.x
        playerY = playerBody.y

        // animator state (idle/run)
        if (combatSystem.attackTimer <= 0f) {
            animator.kind = if (dirX != 0f || dirY != 0f) AnimKind.RUN else AnimKind.IDLE
        }
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
        spawnSystem.update(dt)
        combatSystem.update(dt, playerX, playerY, playerBody.w, playerBody.h)

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
    }

    // =========================================================
    // Public API (Overlay gọi)
    // =========================================================
    fun setDirection(dir: Direction) {
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

    fun pressAttack()  { atkEdge = true; atkHeld = true }
    fun releaseAttack(){ atkHeld = false }

    fun pressA() {}
    fun releaseA() {}
    fun pressB() {}
    fun releaseB() {}
    fun pressC() {}
    fun releaseC() {}
}
