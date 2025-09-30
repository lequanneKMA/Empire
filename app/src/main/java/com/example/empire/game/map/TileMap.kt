package com.example.empire.game.map

import android.graphics.Bitmap

data class Tile(
    val id: Int,
    val x: Int,
    val y: Int
)

data class Layer(
    val name: String,
    val tiles: List<Tile>,
    val collider: Boolean
)

data class Tileset(
    val bitmap: Bitmap,
    val columns: Int,
    val tileSize: Int,
    val spacing: Int = 0,
    val margin: Int = 0
)

data class TileMap(
    val tileSize: Int,       // pixel per tile (square)
    val mapWidth: Int,       // tiles
    val mapHeight: Int,      // tiles
    val tileset: Tileset,
    val layers: List<Layer>
) {
    // === NEW: lÆ°á»›i va cháº¡m (true = ráº¯n)
    val collision: Array<BooleanArray> = Array(mapHeight) { BooleanArray(mapWidth) }

    // === NEW: Ä‘á»•i pixel -> Ã´ tile
    fun worldToTile(px: Float, py: Float): Pair<Int, Int> {
        val ts = tileSize.toFloat()
        val tx = kotlin.math.floor(px / ts).toInt()
        val ty = kotlin.math.floor(py / ts).toInt()
        return tx to ty
    }

    // === NEW: kiá»ƒm tra 1 Ã´ cÃ³ ráº¯n khÃ´ng
    fun isSolidAt(tx: Int, ty: Int): Boolean {
        if (tx !in 0 until mapWidth || ty !in 0 until mapHeight) return true // ngoÃ i map = ráº¯n
        return collision[ty][tx]
    }

    // === NEW: dá»±ng collision tá»« cÃ¡c layer cÃ³ collider=true
    fun buildCollisionFromLayers() {
        // reset collision
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                collision[y][x] = false
            }
        }

        // gán collider cho các layer có collider = true
        for (layer in layers) {
            if (layer.collider) {
                for (tile in layer.tiles) {
                    collision[tile.y][tile.x] = true
                }
            }
        }

        // override: Stair (hoặc Bridge) clear collider
        // special: nếu có Stair thì clear luôn ô stair + ô trên nó
        for (layer in layers) {
            if (layer.name.contains("Stair", ignoreCase = true)) {
                for (t in layer.tiles) {
                    collision[t.y][t.x] = false
                    if (t.y > 0) {
                        collision[t.y - 1][t.x] = false
                    }
                }
            }
        }
    }
    }