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

    fun draw(canvas: Canvas, camX: Int, camY: Int, scale: Float) {
        canvas.save()
        canvas.scale(scale, scale)
        val frameTime = 0.12f
        army.units.forEach { u ->
            val frames = spriteLoader.load(u.type)
            val anim = when(u.anim){
                AnimState.MOVE -> frames.run
                else -> frames.idle
            }
            val list = anim.frames
            val idx = ((time / frameTime).toInt()) % list.size
            val bmp = list.getOrNull(idx)
            if (bmp != null) {
                val drawX = (u.x - camX - bmp.width/2).toInt()
                val drawY = (u.y - camY - bmp.height).toInt() // anchor bottom center
                canvas.drawBitmap(bmp, drawX.toFloat(), drawY.toFloat(), paint)
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
