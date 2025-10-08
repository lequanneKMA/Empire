package com.example.empire.game.army

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

/**
 * Vẽ quân (MVP: chỉ idle/run anim loop) theo camera & scale.
 */
class ArmyRenderSystem(
    private val army: ArmySystem,
    private val spriteLoader: UnitSpriteLoader
) {
    private val paint = Paint()
    private val dbgPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.CYAN
    }
    private var time = 0f

    fun update(dt: Float) { time += dt }
    fun updatePerUnit(dt: Float) {
        army.units.forEach { u ->
            if (u.anim == AnimState.ATTACK) u.attackAnimTimer += dt else u.attackAnimTimer = 0f
        }
    }

    private val renderScale = 0.55f // ~ hơn 1/2 kích thước gốc

    fun draw(canvas: Canvas, camX: Int, camY: Int, scale: Float) {
        canvas.save()
        canvas.scale(scale, scale)
    val moveFrameTime = 0.12f
    val attackFrameTime = 0.10f
    // Iterate over a snapshot to avoid ConcurrentModificationException if units list changes mid-frame
    val snapshot = army.units.toList()
    snapshot.forEach { u ->
            val frames = spriteLoader.load(u.type)
            val facingLeft = u.facing == Facing.LEFT
            val runFrames = if (facingLeft && frames.runLeft != null) frames.runLeft.frames else frames.runRight.frames
            val attackPools = if (facingLeft) frames.attackLeft else frames.attackRight
            val attackFrames = if (attackPools.isNotEmpty()) {
                val anim = attackPools.getOrNull(u.attackVariantIndex % attackPools.size) ?: attackPools.first()
                anim.frames
            } else runFrames
            val chosenList = when(u.anim){
                AnimState.ATTACK -> attackFrames
                AnimState.CAST -> (frames.heal?.frames ?: frames.idle.frames)
                AnimState.MOVE -> runFrames
                AnimState.IDLE -> frames.idle.frames
            }
            val frameTime = if (u.anim == AnimState.ATTACK) attackFrameTime else moveFrameTime
            val animClock = if (u.anim == AnimState.ATTACK) u.attackAnimTimer else time
            val idx = ((animClock / frameTime).toInt()) % chosenList.size
            val bmp = chosenList.getOrNull(idx)
            // Flip horizontal nếu facing LEFT mà chỉ có sheet Right (dựa vào heurstic: nếu folder chứa 'Right_' trong tên attack frames)
            // Hiện tại loader không cung cấp meta tên file, nên tạm thời không flip tự động ở đây.
            if (bmp != null) {
                val scaledW = (bmp.width * renderScale)
                val scaledH = (bmp.height * renderScale)
                val drawX = (u.x - camX - scaledW/2f)
                val drawY = (u.y - camY - scaledH)
                val src = android.graphics.Rect(0,0,bmp.width,bmp.height)
                val dst = android.graphics.Rect(
                    drawX.toInt(),
                    drawY.toInt(),
                    (drawX + scaledW).toInt(),
                    (drawY + scaledH).toInt()
                )
                canvas.drawBitmap(bmp, src, dst, paint)

                // Heal effect overlay (Monk cast) if available
                if (u.anim == AnimState.CAST && frames.healEffect != null) {
                    val effectFrames = frames.healEffect.frames
                    if (effectFrames.isNotEmpty()) {
                        val eIdx = ((time / 0.10f).toInt()) % effectFrames.size
                        val eb = effectFrames[eIdx]
                        val eScaledW = (eb.width * renderScale)
                        val eScaledH = (eb.height * renderScale)
                        val eDrawX = (u.x - camX - eScaledW/2f)
                        val eDrawY = (u.y - camY - eScaledH)
                        val eSrc = android.graphics.Rect(0,0,eb.width,eb.height)
                        val eDst = android.graphics.Rect(
                            eDrawX.toInt(),
                            eDrawY.toInt(),
                            (eDrawX + eScaledW).toInt(),
                            (eDrawY + eScaledH).toInt()
                        )
                        canvas.drawBitmap(eb, eSrc, eDst, paint)
                    }
                }
            } else {
                // fallback debug rect 32x32
                val sx = (u.x - camX - 16).toInt()
                val sy = (u.y - camY - 32).toInt()
                canvas.drawRect(sx.toFloat(), sy.toFloat(), (sx+32).toFloat(), (sy+32).toFloat(), dbgPaint)
            }
        }
        canvas.restore()
    }
}
