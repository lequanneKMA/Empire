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

    fun update(dt: Float) {
        time += dt
        // Advance per-unit attack timers so attack animations can play at their own pace
        army.units.forEach { u ->
            if (u.anim == AnimState.ATTACK) u.attackAnimTimer += dt else u.attackAnimTimer = 0f
        }
    }
    fun updatePerUnit(dt: Float) {
        army.units.forEach { u ->
            if (u.anim == AnimState.ATTACK) u.attackAnimTimer += dt else u.attackAnimTimer = 0f
        }
    }

    private val renderScale = 0.55f // ~ hơn 1/2 kích thước gốc

    // Base movement frame time (seconds per frame)
    private val moveFrameTime = 0.12f
    // Attack frame pacing tùy loại
    private val warriorAttackFrameTime = 0.45f  // chậm hơn nữa (4 frame ~1.8s) chỉ ảnh hưởng hoạt ảnh, không đổi DPS
    private val lancerAttackFrameTime = 0.20f   // 3 frame => ~0.60s anim (nhẹ hơn cooldown)
    private val archerAttackFrameTime = 0.18f
    private val monkCastFrameTime = 0.18f
    private fun attackFrameTime(u: UnitEntity, attackFrames: Int): Float = when(u.type) {
        UnitType.WARRIOR -> warriorAttackFrameTime
        UnitType.LANCER -> lancerAttackFrameTime
        UnitType.ARCHER -> archerAttackFrameTime
        UnitType.MONK -> monkCastFrameTime
    }

    fun draw(canvas: Canvas, camX: Int, camY: Int, scale: Float) {
        canvas.save()
        canvas.scale(scale, scale)
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
            // Lancer defend show: nếu có sheet defend và đang IDLE (không MOVE/ATTACK) trong khi cooldown > 40% còn lại.
            val defendPools = if (facingLeft) frames.defendLeft else frames.defendRight
            val showDefend = (u.type == UnitType.LANCER && u.anim == AnimState.IDLE && defendPools.isNotEmpty() && u.cooldown > u.stats.cooldown * 0.4f)
            val defendFrames = if (showDefend) defendPools.first().frames else null
            val chosenList = when {
                u.anim == AnimState.ATTACK -> attackFrames
                u.anim == AnimState.CAST -> (frames.heal?.frames ?: frames.idle.frames)
                showDefend -> defendFrames ?: frames.idle.frames
                u.anim == AnimState.MOVE -> runFrames
                else -> frames.idle.frames
            }
            val frameTime = if (u.anim == AnimState.ATTACK) attackFrameTime(u, chosenList.size) else moveFrameTime
            val animClock = if (u.anim == AnimState.ATTACK) u.attackAnimTimer else time
            val idx = ((animClock / frameTime).toInt()) % chosenList.size
            val bmp = chosenList.getOrNull(idx)
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

                // Tiny HP bar above unit
                val hbW = scaledW * 0.3f
                val hbH = 2f
                val left = drawX + (scaledW - hbW) / 2f
                val top = drawY - 6f
                val hpPerc = (u.hp.toFloat() / u.stats.maxHp).coerceIn(0f, 1f)
                val back = Paint().apply { color = Color.argb(150, 40, 40, 40) }
                val bar = Paint().apply { color = Color.rgb(220, 40, 40) }
                canvas.drawRect(left, top, left + hbW, top + hbH, back)
                canvas.drawRect(left, top, left + hbW * hpPerc, top + hbH, bar)

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
                val sx = (u.x - camX - 16).toInt()
                val sy = (u.y - camY - 32).toInt()
                canvas.drawRect(sx.toFloat(), sy.toFloat(), (sx+32).toFloat(), (sy+32).toFloat(), dbgPaint)
            }
        }
        // Draw floating damage popups (screen-space in world coords then scaled already)
        if (army.getDamagePopups().isNotEmpty()) {
            val pPaint = Paint(paint)
            pPaint.color = Color.WHITE
            pPaint.textSize = 12f
            pPaint.isFakeBoldText = true
            army.getDamagePopups().forEach { p ->
                val sx = p.x - camX
                val sy = p.y - camY
                canvas.drawText("-${p.value}", sx, sy, pPaint)
            }
        }
        canvas.restore()
    }
}
