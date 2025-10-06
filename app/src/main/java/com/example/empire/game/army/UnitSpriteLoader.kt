package com.example.empire.game.army

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Loads and slices simple horizontal sprite sheets for units.
 * MVP: only Idle + Run; Attack / Cast fallback to Idle first frame.
 * Folder convention (assets):
 *  sprites/units/<Type>/
 *      <Type>_Idle.png
 *      <Type>_Run.png
 * (Optional extra are ignored for now.)
 */
class UnitSpriteLoader(private val context: Context) {

    data class AnimFrames(val frames: List<Bitmap>)
    data class UnitFrames(
        val idle: AnimFrames,
        val run: AnimFrames
    )

    private val cache = mutableMapOf<UnitType, UnitFrames>()

    fun loadAll(types: Array<UnitType> = UnitType.values()) {
        types.forEach { load(it) }
    }

    fun load(type: UnitType): UnitFrames {
        return cache.getOrPut(type) {
            val base = when(type){
                UnitType.WARRIOR -> "sprites/units/Warrior/Warrior"
                UnitType.LANCER  -> "sprites/units/Lancer/Lancer"
                UnitType.ARCHER  -> "sprites/units/Archer/Archer"
                UnitType.MONK    -> "sprites/units/Monk/" + "" // Monk files: Idle.png, Run.png
            }
            val idlePath = if (type == UnitType.MONK) "sprites/units/Monk/Idle.png" else "${base}_Idle.png"
            val runPath  = if (type == UnitType.MONK) "sprites/units/Monk/Run.png"  else "${base}_Run.png"
            val idleBmp = safeDecode(idlePath)
            val runBmp  = safeDecode(runPath)
            UnitFrames(
                idle = AnimFrames(sliceHorizontal(idleBmp)),
                run = AnimFrames(sliceHorizontal(runBmp))
            )
        }
    }

    private fun safeDecode(path: String): Bitmap {
        return try {
            context.assets.open(path).use {
                BitmapFactory.decodeStream(it) ?: makeFallback()
            }
        } catch (e: Exception) { makeFallback() }
    }

    private fun sliceHorizontal(sheet: Bitmap): List<Bitmap> {
        if (sheet.width <= 0 || sheet.height <= 0) return listOf(sheet)
        val frameSize = sheet.height // assume square frames
        if (frameSize <= 0) return listOf(sheet)
        val count = (sheet.width / frameSize).coerceAtLeast(1)
        val frames = ArrayList<Bitmap>(count)
        for (i in 0 until count) {
            val x = i * frameSize
            if (x + frameSize <= sheet.width) {
                frames += Bitmap.createBitmap(sheet, x, 0, frameSize, frameSize)
            }
        }
        return if (frames.isEmpty()) listOf(sheet) else frames
    }

    private fun makeFallback(): Bitmap {
        val bmp = Bitmap.createBitmap(16,16, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFFFF00FF.toInt()) // magenta debug
        return bmp
    }
}
