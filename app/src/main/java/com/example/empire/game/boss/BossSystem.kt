package com.example.empire.game.boss

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.empire.game.ecs.systems.SpawnSystem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class BossSystem(
    private val sprites: BossSpriteLoader,
    private val spawnSystem: SpawnSystem
) {
    enum class State { IDLE, WALK, ATTACK, DEAD }
    enum class Facing { UP, DOWN, LEFT, RIGHT }
    enum class AttackKind { SWIPE, STOMP }

    data class Boss(
        var x: Float,
        var y: Float,
        var w: Float = 112f,
        var h: Float = 112f,
        var maxHp: Int = 100,
        var hp: Int = 100,
        var state: State = State.IDLE,
        var facing: Facing = Facing.DOWN,
        var attackKind: AttackKind = AttackKind.SWIPE,
        var attackCooldown: Float = 1.2f,
        var attackTimer: Float = 0f,
        var attackDidDamage: Boolean = false,
        var deathTimer: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f
    )

    var boss: Boss? = null
        private set

    // wiring to game events
    var onPlayerHit: (Int) -> Unit = {}
    var onArmyImpact: (Float, Float, Float, Int) -> Unit = { _,_,_,_ -> }
    var onDeath: () -> Unit = {}

    private var playerX = 0f
    private var playerY = 0f

    fun spawnAt(x: Float, y: Float) { boss = Boss(x, y) }
    fun setPlayerTarget(x: Float, y: Float) { playerX = x; playerY = y }
    fun clear() { boss = null }

    fun update(dt: Float) {
        val b = boss ?: return
        if (b.hp <= 0 && b.state != State.DEAD) {
            b.state = State.DEAD
            b.deathTimer = 1.4f
            return
        }
        if (b.state == State.DEAD) {
            b.deathTimer -= dt
            if (b.deathTimer <= 0f) { onDeath(); boss = null }
            return
        }
        // movement towards target
        val cx = b.x + b.w/2f
        val cy = b.y + b.h/2f
        val dx = playerX - cx
        val dy = playerY - cy
        val d2 = dx*dx + dy*dy
        val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val speed = 28f
        val inRange = d <= if (b.attackKind == AttackKind.STOMP) 100f else 64f
        if (b.state == State.ATTACK) {
            b.attackTimer -= dt
            val hitPoint = if (b.attackKind == AttackKind.STOMP) 0.75f else 0.35f
            val dur = if (b.attackKind == AttackKind.STOMP) 1.1f else 0.8f
            if (!b.attackDidDamage && b.attackTimer <= (dur - hitPoint)) {
                b.attackDidDamage = true
                val dmg = if (b.attackKind == AttackKind.STOMP) 12 else 9
                val range = if (b.attackKind == AttackKind.STOMP) 50f else 32f
                // Impact both player and nearest unit area; outer layer can decide target handling
                onArmyImpact(cx, cy, range, dmg)
                onPlayerHit(dmg)
            }
            if (b.attackTimer <= 0f) {
                b.state = State.IDLE
                b.attackCooldown = if (b.attackKind == AttackKind.STOMP) 2.2f else 1.6f
                b.attackDidDamage = false
            }
        } else {
            if (!inRange) {
                if (d > 1f) { b.vx = (dx/d) * speed; b.vy = (dy/d) * speed }
                b.x += b.vx * dt; b.y += b.vy * dt
                b.state = State.WALK
            } else {
                if (b.attackCooldown <= 0f) {
                    b.attackKind = if ((0..1).random()==0) AttackKind.SWIPE else AttackKind.STOMP
                    b.attackTimer = if (b.attackKind == AttackKind.STOMP) 1.1f else 0.8f
                    b.attackDidDamage = false
                    b.state = State.ATTACK
                } else b.state = State.IDLE
            }
        }
        // facing
        if (kotlin.math.abs(b.vx) >= kotlin.math.abs(b.vy)) b.facing = if (b.vx >= 0) Facing.RIGHT else Facing.LEFT
        else b.facing = if (b.vy >= 0) Facing.DOWN else Facing.UP
        if (b.attackCooldown > 0f) b.attackCooldown -= dt
    }

    fun applyDamage(dmg: Int): Boolean {
        val b = boss ?: return false
        if (b.state == State.DEAD) return false
        b.hp -= dmg
        if (b.hp <= 0) { b.hp = 0; b.state = State.DEAD; b.deathTimer = 1.4f; return true }
        return false
    }

    fun draw(canvas: Canvas, camX: Int, camY: Int, scale: Float) {
        val b = boss ?: return
        val frames = sprites.load()
        val dirFrames = when(b.state){
            State.ATTACK -> if (b.attackKind == AttackKind.STOMP) frames.stomp else frames.swipe
            State.WALK -> frames.walk
            else -> frames.idle
        }
        val list = when(b.facing){
            Facing.UP -> dirFrames.up
            Facing.DOWN -> dirFrames.down
            Facing.LEFT -> dirFrames.left
            Facing.RIGHT -> dirFrames.right
        }
        val idx = ((System.nanoTime()/100_000_000L)%list.size).toInt()
        val bmp = list[idx]
        val src = SRC.apply { set(0,0,bmp.width,bmp.height) }
        val x = (b.x - camX)
        val y = (b.y - camY)
        val dst = DST.apply { set(x.toInt(), y.toInt(), (x+b.w).toInt(), (y+b.h).toInt()) }
        canvas.save(); canvas.scale(scale, scale)
        canvas.drawBitmap(bmp, src, dst, null)
        canvas.restore()
        // Optional: draw HP bar
        val p = hpPaint
        val back = hpBack
        val top = (y-8).coerceAtLeast(0f)
        val w = b.w
        val perc = (b.hp.toFloat()/b.maxHp).coerceIn(0f,1f)
        canvas.save(); canvas.scale(scale, scale)
        canvas.drawRect(x, top, x+w, top+5, back)
        canvas.drawRect(x, top, x+w*perc, top+5, p)
        canvas.restore()
    }

    companion object {
        private val SRC = Rect()
        private val DST = Rect()
        private val hpPaint = Paint().apply { color = 0xFFFF3333.toInt() }
        private val hpBack = Paint().apply { color = 0x88AA0000.toInt() }
    }
}
