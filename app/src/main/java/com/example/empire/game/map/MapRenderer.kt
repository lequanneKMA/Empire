package com.example.empire.game.map

import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

class MapRenderer(private val map: TileMap) {
    private val src = Rect()
    private val dst = Rect()

    fun draw(canvas: Canvas, camX: Int, camY: Int, screenW: Int, screenH: Int) {
        val ts  = map.tileSize
        val set = map.tileset
        val cols = set.columns
        val spacing = set.spacing
        val margin = set.margin
        val bmp = set.bitmap ?: return

        // Camera culling
        val startCol = max(0, camX / ts)
        val startRow = max(0, camY / ts)
        val endCol   = min(map.mapWidth,  (camX + screenW) / ts + 2)
        val endRow   = min(map.mapHeight, (camY + screenH) / ts + 2)

        // JSON cũ: Background ở CUỐI → phải vẽ TRƯỚC
        for (layer in map.layers.asReversed()) {
            for (tile in layer.tiles) {
                if (tile.x < startCol || tile.x >= endCol ||
                    tile.y < startRow || tile.y >= endRow) continue

                val id = tile.id
                if (id <= 0) continue

                // Toạ độ trong tileset
                val srcX = margin + (id % cols) * (ts + spacing)
                val srcY = margin + (id / cols) * (ts + spacing)
                src.set(srcX, srcY, srcX + ts, srcY + ts)

                // Toạ độ ngoài screen
                val dstX = tile.x * ts - camX
                val dstY = tile.y * ts - camY
                dst.set(dstX, dstY, dstX + ts, dstY + ts)

                canvas.drawBitmap(bmp, src, dst, null)
            }
        }
    }
}
