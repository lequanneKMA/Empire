package com.example.empire.core.gfx

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log

enum class Facing { Down, Up, Left, Right }

/** slug = tên thư mục & prefix file (đều lowercase, trùng nhau) */
enum class AnimKind(val slug: String) {
    IDLE("idle"),
    RUN("run"),
    ATTACK1("attack1"),
    ATTACK2("attack2");
}

class PlayerAnimator(
    private val ctx: Context,
    private val basePath: String = "sprites/player",
    // KÍCH THƯỚC VẼ RA MÀN (tile size) – KHÔNG PHẢI kích thước trong sheet
    private val frameW: Int = 256,
    private val frameH: Int = 256,
    private val fps: Int = 12,
    private val framesPerSheet: Int = 8,   // mỗi sheet 1 hàng ngang 8 frame
) {
    private val animations = HashMap<Pair<AnimKind, Facing>, List<Bitmap>>()

    var facing: Facing = Facing.Down

    private var _kind: AnimKind = AnimKind.IDLE
    var kind: AnimKind
        get() = _kind
        set(value) {
            if (_kind != value) {
                _kind = value
                frame = 0
                t = 0f
            }
        }

    private var t = 0f
    private var frame = 0

    fun load() {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val kinds   = arrayOf(AnimKind.IDLE, AnimKind.RUN, AnimKind.ATTACK1, AnimKind.ATTACK2)
        val facings = arrayOf(Facing.Down, Facing.Up, Facing.Left, Facing.Right)

        for (k in kinds) for (f in facings) {
            // folder & prefix dùng chung 'slug'
            val path = "$basePath/${k.slug}/${k.slug}_${f.name.lowercase()}.png"
            try {
                ctx.assets.open(path).use { input ->
                    val sheet = BitmapFactory.decodeStream(input, null, opts)
                    if (sheet != null) {
                        animations[k to f] = sliceRow(sheet, framesPerSheet)
                    } else {
                        Log.e("PlayerAnimator", "Decode fail: $path")
                    }
                }
            } catch (e: Exception) {
                // Thiếu file -> draw() sẽ fallback
                Log.w("PlayerAnimator", "Missing sprite: $path")
            }
        }
    }

    fun update(dt: Float) {
        val frames = animations[kind to facing] ?: animations[AnimKind.IDLE to facing] ?: return
        if (frames.isEmpty()) return
        t += dt
        val step = 1f / fps
        while (t >= step) {
            t -= step
            frame = (frame + 1) % frames.size
        }
    }

    // Vẽ tại (x, y) và scale về kích thước tile (frameW x frameH)
    fun draw(canvas: Canvas, x: Int, y: Int, dstW: Int, dstH: Int) {
        val frames = animations[kind to facing] ?: animations[AnimKind.IDLE to facing] ?: return
        if (frames.isEmpty()) return
        val bmp = frames[frame]
        val src = Rect(0, 0, bmp.width, bmp.height)
        val dst = Rect(x, y, x + dstW, y + dstH)
        canvas.drawBitmap(bmp, src, dst, null)
    }


    /** Cắt 1 hàng spritesheet: width mỗi frame = sheet.width / framesPerRow */
    private fun sliceRow(sheet: Bitmap, framesPerRow: Int): List<Bitmap> {
        val fw = sheet.width / framesPerRow
        val fh = sheet.height
        val out = ArrayList<Bitmap>(framesPerRow)
        for (i in 0 until framesPerRow) {
            out.add(Bitmap.createBitmap(sheet, i * fw, 0, fw, fh))
        }
        return out
    }
}
