package com.example.empire.game.boss

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class BossSpriteLoader(private val context: Context) {
    data class DirSet(
        val up: List<Bitmap>,
        val down: List<Bitmap>,
        val left: List<Bitmap>,
        val right: List<Bitmap>
    )
    data class Frames(
        val idle: DirSet,
        val walk: DirSet,
        val swipe: DirSet,
        val stomp: DirSet
    )

    private var cached: Frames? = null

    fun load(): Frames {
        cached?.let { return it }
        val base = "sprites/boss/"
        val idle = DirSet(
            up = loadStrip(base + "idle/Up.png"),
            down = loadStrip(base + "idle/Down.png"),
            left = loadStrip(base + "idle/Left.png"),
            right = loadStrip(base + "idle/Right.png")
        )
        val walk = DirSet(
            up = loadStrip(base + "walk/Up.png"),
            down = loadStrip(base + "walk/Down.png"),
            left = loadStrip(base + "walk/Left.png"),
            right = loadStrip(base + "walk/Right.png")
        )
        val swipe = DirSet(
            up = loadStrip(base + "swipe/Up.png"),
            down = loadStrip(base + "swipe/Down.png"),
            left = loadStrip(base + "swipe/Left.png"),
            right = loadStrip(base + "swipe/Right.png")
        )
        val stomp = DirSet(
            up = loadStrip(base + "stomp/Up.png"),
            down = loadStrip(base + "stomp/Down.png"),
            left = loadStrip(base + "stomp/Left.png"),
            right = loadStrip(base + "stomp/Right.png")
        )
        return Frames(idle, walk, swipe, stomp).also { cached = it }
    }

    private fun loadStrip(path: String): List<Bitmap> {
        val bmp = decode(path)
        return sliceHorizontal(bmp)
    }

    private fun decode(path: String): Bitmap {
        return try { context.assets.open(path).use { BitmapFactory.decodeStream(it) } } catch (_: Exception) {
            // fallback 1x1 red
            val b = Bitmap.createBitmap(16,16, Bitmap.Config.ARGB_8888)
            b.eraseColor(0xFFFF0000.toInt())
            b
        }
    }

    private fun sliceHorizontal(sheet: Bitmap): List<Bitmap> {
        if (sheet.width <= 0 || sheet.height <= 0) return listOf(sheet)
        val size = sheet.height
        val count = (sheet.width / size).coerceAtLeast(1)
        val frames = ArrayList<Bitmap>(count)
        for (i in 0 until count) {
            val x = i * size
            if (x + size <= sheet.width) frames += Bitmap.createBitmap(sheet, x, 0, size, size)
        }
        return if (frames.isEmpty()) listOf(sheet) else frames
    }
}
