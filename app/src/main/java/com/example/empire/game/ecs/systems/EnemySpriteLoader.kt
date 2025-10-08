package com.example.empire.game.ecs.systems

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Directional enemy sprite loader.
 * Expected naming per enemy kind (e.g. Wolf):
 *  Down_Wolf_Walk.png, Up_Wolf_Walk.png, Side_Wolf_Walk.png
 *  (optional) Down_Wolf_Attack.png, Up_Wolf_Attack.png, Side_Wolf_Attack.png
 *  (optional) Down_Wolf_Death.png, Up_Wolf_Death.png, Side_Wolf_Death.png
 * Each file: horizontal strip, square frames (height = frame size).
 * Side sheets are assumed facing RIGHT; renderer will flip for left.
 */
class EnemySpriteLoader(private val context: Context) {
    enum class EnemyKind { WOLF, MONSTER, SLIME, FLYBEE }

    data class DirectionalSet(
        val walk: List<Bitmap>,
        val attack: List<Bitmap>?,
        val death: List<Bitmap>?
    )
    data class EnemyFrames(
        val up: DirectionalSet,
        val down: DirectionalSet,
        val side: DirectionalSet,
        val sideBaseFacesLeft: Boolean
    )

    private val cache = mutableMapOf<EnemyKind, EnemyFrames>()

    fun loadAll(kinds: Array<EnemyKind> = EnemyKind.values()) { kinds.forEach { load(it) } }

    fun load(kind: EnemyKind): EnemyFrames = cache.getOrPut(kind) { loadKind(kind) }

    private fun loadKind(kind: EnemyKind): EnemyFrames {
        val name = when(kind){
            EnemyKind.WOLF -> "Wolf"
            EnemyKind.MONSTER -> "Monster"
            EnemyKind.SLIME -> "Slime"
            EnemyKind.FLYBEE -> resolveFlyBeeName()
        }
        val base = "sprites/enemy/$name/"
        val down = loadDirSet(base, "Down_${name}")
        val up = loadDirSet(base, "Up_${name}")
        val side = loadDirSet(base, "Side_${name}")
        // Orientation assumptions (có thể chỉnh nếu asset khác):
        val sideLeft = when(kind){
            EnemyKind.WOLF -> true      // Wolf sheet faces LEFT
            EnemyKind.MONSTER -> true   // Adjusted: Monster sheet actually faces LEFT (was marked RIGHT) -> fix reversed animation
            EnemyKind.SLIME -> false    // Slime sheet faces RIGHT
            EnemyKind.FLYBEE -> false   // Flybee sheet faces RIGHT
        }
        return EnemyFrames(up = up, down = down, side = side, sideBaseFacesLeft = sideLeft)
    }

    // Try multiple capitalizations for FlyBee asset folder / filenames
    private fun resolveFlyBeeName(): String {
        val candidates = listOf("Flybee", "FlyBee", "FLYBEE", "flybee")
        for (c in candidates) {
            if (assetExists("sprites/enemy/$c/Down_${c}_Walk.png")) return c
        }
        return "Flybee" // fallback (will show red squares if missing)
    }

    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).close(); true
    } catch (_: Exception) { false }

    private fun loadDirSet(base: String, stem: String): DirectionalSet {
        val walk = sliceHorizontal(safeDecode(base + stem + "_Walk.png"))
        val attack = decodeOptional(base + stem + "_Attack.png")
        val death = decodeOptional(base + stem + "_Death.png")
        return DirectionalSet(
            walk = walk,
            attack = attack?.let { sliceHorizontal(it) },
            death = death?.let { sliceHorizontal(it) }
        )
    }

    private fun decodeOptional(path: String): Bitmap? = try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }

    private fun safeDecode(path: String): Bitmap {
        return try { context.assets.open(path).use { BitmapFactory.decodeStream(it) } ?: fallback() } catch (e: Exception) { fallback() }
    }

    private fun sliceHorizontal(sheet: Bitmap): List<Bitmap> {
        if (sheet.width <= 0 || sheet.height <= 0) return listOf(sheet)
        val size = sheet.height
        if (size <= 0) return listOf(sheet)
        val count = (sheet.width / size).coerceAtLeast(1)
        val frames = ArrayList<Bitmap>(count)
        for (i in 0 until count) {
            val x = i * size
            if (x + size <= sheet.width) frames += Bitmap.createBitmap(sheet, x, 0, size, size)
        }
        return if (frames.isEmpty()) listOf(sheet) else frames
    }

    private fun fallback(): Bitmap {
        val bmp = Bitmap.createBitmap(16,16, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFFFF0000.toInt())
        return bmp
    }
}
