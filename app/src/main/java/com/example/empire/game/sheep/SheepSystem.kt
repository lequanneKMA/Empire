package com.example.empire.game.sheep

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.random.Random

/**
 * Quản lý bầy cừu trong trang trại (map main, góc dưới bên phải).
 * Cừu chỉ di chuyển ngẫu nhiên trong vùng giới hạn.
 */
class SheepSystem(private val assets: android.content.res.AssetManager) {
    data class Sheep(
        var x: Float,
        var y: Float,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var animTime: Float = 0f,
        var alive: Boolean = true,
        var hp: Int = 3,
        var bounce: Boolean = false
    )

    data class MeatDrop(var x: Float, var y: Float, var collected: Boolean = false)

    private val _sheep = mutableListOf<Sheep>()
    val sheep: List<Sheep> get() = _sheep
    private val _meatDrops = mutableListOf<MeatDrop>()
    val meatDrops: List<MeatDrop> get() = _meatDrops

    // Trang trại bounding box (pixel) – set từ GameView
    var farmLeft = 0f
    var farmTop = 0f
    var farmRight = 0f
    var farmBottom = 0f

    // Sprites
    private var idleSheet: Bitmap? = null
    private var bounceSheet: Bitmap? = null
    private var meatBitmap: Bitmap? = null

    data class Anim(val frames: List<Bitmap>)
    private var idleAnim: Anim? = null
    private var bounceAnim: Anim? = null
    // meat không còn là anim

    private fun slice(sheet: Bitmap): List<Bitmap> {
        val h = sheet.height
        if (h <= 0) return listOf(sheet)
        val frameSize = h
        val count = (sheet.width / frameSize).coerceAtLeast(1)
        return (0 until count).map { i ->
            val x = i * frameSize
            Bitmap.createBitmap(sheet, x, 0, frameSize, frameSize)
        }
    }

    fun load() {
        if (idleSheet == null) {
            idleSheet = loadBmp("sprites/sheep/idle.png")
            bounceSheet = loadBmp("sprites/sheep/bouncing.png")
            meatBitmap = loadBmp("sprites/spawn/Meat.png")
            idleAnim = idleSheet?.let { Anim(slice(it)) }
            bounceAnim = bounceSheet?.let { Anim(slice(it)) }
        }
    }

    private fun loadBmp(path: String): Bitmap? = try { assets.open(path).use { BitmapFactory.decodeStream(it) } } catch (_: Exception){ null }

    fun spawnSheep(count: Int) {
        repeat(count) {
            val sx = Random.nextFloat() * (farmRight - farmLeft - 16f) + farmLeft + 8f
            val sy = Random.nextFloat() * (farmBottom - farmTop - 16f) + farmTop + 8f
            _sheep += Sheep(sx, sy)
        }
    }

    fun update(dt: Float) {
        _sheep.forEach { s ->
            if (!s.alive) return@forEach
            s.animTime += dt
            // Random dir change
            if (Random.nextFloat() < 0.01f) {
                val ang = Random.nextFloat() * (Math.PI * 2).toFloat()
                val spd = 18f
                s.vx = kotlin.math.cos(ang) * spd
                s.vy = kotlin.math.sin(ang) * spd
            }
            s.x += s.vx * dt
            s.y += s.vy * dt
            // Clamp to farm bounds
            if (s.x < farmLeft) { s.x = farmLeft; s.vx = -s.vx }
            if (s.y < farmTop) { s.y = farmTop; s.vy = -s.vy }
            if (s.x > farmRight) { s.x = farmRight; s.vx = -s.vx }
            if (s.y > farmBottom) { s.y = farmBottom; s.vy = -s.vy }
            // Simple bounce anim toggle when moving
            s.bounce = (kotlin.math.abs(s.vx) + kotlin.math.abs(s.vy)) > 2f
        }
    }

    fun damageSheep(px: Float, py: Float, radius: Float, dmg: Int, onMeatDrop: (Float, Float) -> Unit) {
        _sheep.forEach { s ->
            if (!s.alive) return@forEach
            val dx = s.x - px
            val dy = s.y - py
            if (dx*dx + dy*dy <= radius*radius) {
                s.hp -= dmg
                if (s.hp <= 0) {
                    s.hp = 0
                    s.alive = false
                    onMeatDrop(s.x, s.y)
                    _meatDrops += MeatDrop(s.x, s.y) // luôn 1 miếng
                }
            }
        }
    }

    fun currentFrame(s: Sheep): Bitmap? {
        if (!s.alive) return null // không vẽ cừu chết (thịt vẽ riêng)
        val anim = if (s.bounce) bounceAnim else idleAnim
        val frames = anim?.frames ?: return null
        val t = if (s.alive) s.animTime else s.animTime * 0.6f
        val idx = ((t / 0.25f).toInt()) % frames.size
        return frames[idx]
    }

    fun meatBitmap(): Bitmap? = meatBitmap
    fun cleanupCollected() { _meatDrops.removeAll { it.collected } }

    fun tryCollect(px: Float, py: Float, radius: Float, onCollect: (Int) -> Unit) {
        var count = 0
        _meatDrops.forEach { d -> if (!d.collected) { val dx = d.x - px; val dy = d.y - py; if (dx*dx + dy*dy <= radius*radius) { d.collected = true; count++ } } }
        if (count > 0) onCollect(count)
        cleanupCollected()
    }

    // Helpers for external management
    fun clearAll() {
        _sheep.clear()
        _meatDrops.clear()
    }
    fun aliveCount(): Int = _sheep.count { it.alive }
}
