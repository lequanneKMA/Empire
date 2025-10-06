package com.example.empire.game.ecs.systems

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Bitmap
import android.graphics.Matrix

/** Enemy renderer using sprite sheets via EnemySpriteLoader. */
class EnemyRenderSystem(
    private val spawnSystem: SpawnSystem,
    private val spriteLoader: EnemySpriteLoader
) {
    private val hpBar = Paint().apply { color = Color.RED }
    private val hpBack = Paint().apply { color = Color.argb(120,80,0,0) }
    private val damageTint = Paint().apply { color = Color.argb(90,255,0,0) }
    private val matrix = Matrix()

    // Track per-enemy animation frame progress
    private val frameTime = mutableMapOf<Int, Float>()
    private val currentFrame = mutableMapOf<Int, Int>()

    fun update(dt: Float) {
        spawnSystem.enemies.forEach { e ->
            if (!e.alive || e.hp <= 0) return@forEach
            val key = System.identityHashCode(e)
            val t = (frameTime[key] ?: 0f) + dt
            val speedMul = if (e.state == SpawnSystem.Enemy.State.WALK) 1f else 0.6f
            if (t > FRAME_DURATION / speedMul) {
                frameTime[key] = t - FRAME_DURATION / speedMul
                currentFrame[key] = (currentFrame[key] ?: 0) + 1
            } else frameTime[key] = t
        }
    }

    fun draw(canvas: Canvas, camX: Int, camY: Int, scale: Float) {
        val enemies = spawnSystem.enemies
        if (enemies.isEmpty()) return
        canvas.save()
        canvas.scale(scale, scale)
        enemies.forEach { e ->
            if (!e.alive || e.hp <= 0) return@forEach
            val frames = framesForType(e.type)
            val dirSet = when(e.facing){
                SpawnSystem.Enemy.Facing.UP -> frames.up
                SpawnSystem.Enemy.Facing.DOWN -> frames.down
                SpawnSystem.Enemy.Facing.LEFT, SpawnSystem.Enemy.Facing.RIGHT -> frames.side
            }
            val animList = when (e.state) {
                SpawnSystem.Enemy.State.ATTACK -> dirSet.attack ?: dirSet.walk
                SpawnSystem.Enemy.State.DEAD -> dirSet.death ?: dirSet.walk
                else -> dirSet.walk
            }
            val idx = System.identityHashCode(e)
            val frameIdx = (currentFrame[idx] ?: 0) % animList.size
            var bmp = animList[frameIdx]
            val x = (e.x - camX)
            val y = (e.y - camY)
            val w = e.w
            val h = e.h
            val sideBaseFacesLeft = frames.sideBaseFacesLeft
            val needFlip = when(e.facing){
                SpawnSystem.Enemy.Facing.LEFT -> !sideBaseFacesLeft
                SpawnSystem.Enemy.Facing.RIGHT -> sideBaseFacesLeft
                else -> false
            }
            if (needFlip) {
                matrix.reset()
                val sx = w / bmp.width
                val sy = h / bmp.height
                matrix.postScale(-sx, sy)
                matrix.postTranslate(x + w, y)
                canvas.drawBitmap(bmp, matrix, null)
            } else {
                val src = SRC_RECT.apply { set(0,0,bmp.width,bmp.height) }
                val dst = DST_RECT.apply { set(x.toInt(), y.toInt(), (x+w).toInt(), (y+h).toInt()) }
                canvas.drawBitmap(bmp, src, dst, null)
            }
            if (e.hp < e.maxHp * 0.35f) {
                canvas.drawRect(x, y, x + w, y + h, damageTint)
            }
            drawHpBar(canvas, e, x, y)
        }
        canvas.restore()
    }

    private fun drawHpBar(canvas: Canvas, e: SpawnSystem.Enemy, x: Float, y: Float) {
        val hbW = e.w
        val hbH = 4f
        val top = y - 6f
        canvas.drawRect(x, top, x + hbW, top + hbH, hpBack)
        val perc = (e.hp.toFloat() / e.maxHp).coerceIn(0f,1f)
        canvas.drawRect(x, top, x + hbW * perc, top + hbH, hpBar)
    }

    private fun framesForType(type: SpawnSystem.EnemyType): EnemySpriteLoader.EnemyFrames = when(type){
        SpawnSystem.EnemyType.WOLF -> spriteLoader.load(EnemySpriteLoader.EnemyKind.WOLF)
        SpawnSystem.EnemyType.MONSTER -> spriteLoader.load(EnemySpriteLoader.EnemyKind.MONSTER)
        SpawnSystem.EnemyType.SLIME -> spriteLoader.load(EnemySpriteLoader.EnemyKind.SLIME)
        SpawnSystem.EnemyType.FLYBEE -> spriteLoader.load(EnemySpriteLoader.EnemyKind.FLYBEE)
    }

    companion object {
        private const val FRAME_DURATION = 0.14f
        private val SRC_RECT = Rect()
        private val DST_RECT = Rect()
    }
}

