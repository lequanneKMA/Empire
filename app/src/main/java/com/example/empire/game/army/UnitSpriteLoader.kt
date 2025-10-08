package com.example.empire.game.army

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

/**
 * Unified sprite loader cho tất cả unit với tên file hiện tại nằm trong:
 *  assets/sprites/units/<UnitType>/...
 * Warrior: Idle.png Run.png Attack1.png Attack2.png Defend.png
 * Lancer : Idle.png Run.png <Dir>_Attack.png <Dir>_Defend.png (Dir: Up,Down,Right,Up_Right,Down_Right,...) + Down_Right.png (optional run dir)
 * Archer : Idle.png Run.png Shoot.png Arrow.png
 * Monk   : Idle.png Run.png Heal.png Heal_Effect.png
 */
class UnitSpriteLoader(private val context: Context) {
    data class Anim(val frames: List<Bitmap>)
    data class UnitFrames(
        val idle: Anim,
        val runRight: Anim,
        val runLeft: Anim?,
        val attackRight: List<Anim>,
        val attackLeft: List<Anim>,
        val defendRight: List<Anim>,
        val defendLeft: List<Anim>,
        val heal: Anim?,
        val healEffect: Anim?,
        val arrow: Bitmap?
    )

    private val cache = mutableMapOf<UnitType, UnitFrames>()

    // Preload tất cả unit (giữ API cũ để GameView không lỗi)
    fun loadAll(types: Array<UnitType> = UnitType.values()) {
        types.forEach { load(it) }
    }

    fun load(type: UnitType): UnitFrames = cache.getOrPut(type) { loadUnit(type) }

    private fun loadUnit(type: UnitType): UnitFrames {
        val folder = "sprites/units/${type.name.lowercase().replaceFirstChar { it.uppercase() }}/"
        fun bmpOpt(name: String) = safeDecodeOrNull(folder + name)
        fun firstBitmap(names: List<String>): Bitmap? {
            for (n in names) { val b = bmpOpt(n); if (b != null) return b }
            return null
        }
        fun animFrom(names: List<String>): Anim? = firstBitmap(names)?.let { Anim(slice(it)) }

        // Idle (bắt buộc)
        val idle = Anim(slice(safeDecode(folder + "Idle.png")))

        // Generic run fallback list theo thứ tự ưu tiên (Right, Run, Left)
    val runRight = animFrom(listOf("Right_Run.png","Run.png")) ?: idle
    val runLeft = animFrom(listOf("Left_Run.png"))

    val attackRight = mutableListOf<Anim>()
    val attackLeft = mutableListOf<Anim>()
    val defendRight = mutableListOf<Anim>()
    val defendLeft = mutableListOf<Anim>()
        var heal: Anim? = null
        var healEff: Anim? = null
        var arrow: Bitmap? = null

        when(type) {
            UnitType.WARRIOR -> {
                // Directional + variant numbers
                // Right variants
                listOf("Right_Attack1.png","Right_Attack2.png","Right_Attack.png").forEach { bmpOpt(it)?.let { b -> attackRight += Anim(slice(b)) } }
                // Left variants
                listOf("Left_Attack1.png","Left_Attack2.png","Left_Attack.png").forEach { bmpOpt(it)?.let { b -> attackLeft += Anim(slice(b)) } }
                listOf("Right_Defend.png").forEach { bmpOpt(it)?.let { b -> defendRight += Anim(slice(b)) } }
                listOf("Left_Defend.png").forEach { bmpOpt(it)?.let { b -> defendLeft += Anim(slice(b)) } }
            }
            UnitType.LANCER -> {
                listOf("Right_Attack.png").forEach { bmpOpt(it)?.let { b -> attackRight += Anim(slice(b)) } }
                listOf("Left_Attack.png").forEach { bmpOpt(it)?.let { b -> attackLeft += Anim(slice(b)) } }
                listOf("Right_Defend.png").forEach { bmpOpt(it)?.let { b -> defendRight += Anim(slice(b)) } }
                listOf("Left_Defend.png").forEach { bmpOpt(it)?.let { b -> defendLeft += Anim(slice(b)) } }
            }
            UnitType.ARCHER -> {
                listOf("Right_Shoot.png","Shoot.png").forEach { bmpOpt(it)?.let { b -> attackRight += Anim(slice(b)) } }
                arrow = bmpOpt("Arrow.png")
            }
            UnitType.MONK -> {
                bmpOpt("Heal.png")?.let { b -> val a = Anim(slice(b)); heal = a; attackRight += a }
                bmpOpt("Heal_Effect.png")?.let { b -> healEff = Anim(slice(b)) }
            }
        }

        if (attackRight.isEmpty() && attackLeft.isEmpty()) attackRight += idle
        return UnitFrames(
            idle = idle,
            runRight = runRight,
            runLeft = runLeft,
            attackRight = attackRight,
            attackLeft = attackLeft,
            defendRight = defendRight,
            defendLeft = defendLeft,
            heal = heal,
            healEffect = healEff,
            arrow = arrow
        )
    }

    private fun slice(sheet: Bitmap): List<Bitmap> {
        if (sheet.width <= 0 || sheet.height <= 0) return listOf(sheet)
        val frameSize = sheet.height
        if (frameSize <= 0) return listOf(sheet)
        val count = (sheet.width / frameSize).coerceAtLeast(1)
        val out = ArrayList<Bitmap>(count)
        for (i in 0 until count) {
            val x = i * frameSize
            if (x + frameSize <= sheet.width) out += Bitmap.createBitmap(sheet, x, 0, frameSize, frameSize)
        }
        return if (out.isEmpty()) listOf(sheet) else out
    }

    private fun safeDecode(path: String): Bitmap = try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) ?: fallback() }
    } catch (e: Exception) { fallback() }

    private fun safeDecodeOrNull(path: String): Bitmap? = try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }

    private fun fallback(): Bitmap {
        val b = Bitmap.createBitmap(16,16, Bitmap.Config.ARGB_8888)
        b.eraseColor(0xFFFF00FF.toInt())
        return b
    }

    // Optional: horizontal mirror helper (currently unused but ready for left variants)
    private fun mirror(src: Bitmap): Bitmap {
        val m = Matrix().apply { preScale(-1f,1f) }
        return Bitmap.createBitmap(src,0,0,src.width,src.height,m,true)
    }
}
