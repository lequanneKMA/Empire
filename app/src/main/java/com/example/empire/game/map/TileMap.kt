package com.example.empire.game.map

import android.graphics.Bitmap
import kotlin.math.floor

// ---------- data models ----------
data class Tile(
    val id: Int,
    val x: Int,
    val y: Int
)

data class Layer(
    val name: String,
    val tiles: List<Tile>,
    /** true = layer này có va chạm (đá, vách, nhà…) */
    val collider: Boolean
)

data class Tileset(
    val bitmap: Bitmap,
    val columns: Int,
    val tileSize: Int,
    val spacing: Int = 0,
    val margin: Int = 0
)

/**
 * TileMap: giữ dữ liệu map + lưới collision
 *
 * Quy ước:
 * - Các layer nền (Water/Grass/Sand) thường collider = false.
 * - Các layer cản (Rock/Cliff/Building/Tree) collider = true.
 * - Các layer “vượt” (Bridge/Stair/…): collider = false nhưng
 *   **ghi đè** thành không rắn nếu nằm trên một ô rắn.
 */
data class TileMap(
    val tileSize: Int,
    val mapWidth: Int,
    val mapHeight: Int,
    val tileset: Tileset,
    val layers: List<Layer>
) {

    /** collision[y][x] = true nghĩa là ô rắn, không đi qua. */
    val collision: Array<BooleanArray> = Array(mapHeight) { BooleanArray(mapWidth) }
    // Lưu tên layer (trim) đã set collision để biết loại va chạm (Rock, Building...).
    private val collisionSource: Array<Array<String?>> = Array(mapHeight) { Array<String?>(mapWidth) { null } }

    /**
     * Danh sách spawn point (tọa độ tile) được nạp từ JSON.
     * Key ví dụ: "player", "caveEntrance", ...
     */
    val spawnPoints: MutableMap<String, Pair<Int, Int>> = mutableMapOf()

    /**
     * Danh sách layer có tính “pass-through” (đè lên để cho đi qua).
     * Thêm tên layer của map m vào đây nếu cần.
     */
    private val passThroughLayerNames = setOf(
        "Bridge",
        "Stair"
    )

    init {
        buildCollisionFromLayers()
    }

    // ---------- world <-> tile ----------
    fun worldToTile(px: Float, py: Float): Pair<Int, Int> {
        val ts = tileSize.toFloat()
        val tx = floor(px / ts).toInt()
        val ty = floor(py / ts).toInt()
        return tx to ty
    }

    fun isSolidAt(tx: Int, ty: Int): Boolean {
        if (tx !in 0 until mapWidth || ty !in 0 until mapHeight) return true // ngoài map = rắn
        return collision[ty][tx]
    }

    fun setSolid(tx: Int, ty: Int, solid: Boolean) {
        if (tx in 0 until mapWidth && ty in 0 until mapHeight) {
            collision[ty][tx] = solid
        }
    }

    fun rebuildCollision() = buildCollisionFromLayers()

    // ---------- spawn utilities ----------
    /** Lấy spawn point theo tên (trả về cặp (tx, ty) hoặc null). */
    fun getSpawn(name: String): Pair<Int, Int>? = spawnPoints[name]

    /** Chuyển tile (tx,ty) sang tâm pixel (x,y). */
    fun tileCenter(tx: Int, ty: Int): Pair<Float, Float> {
        val cx = tx * tileSize + tileSize / 2f
        val cy = ty * tileSize + tileSize / 2f
        return cx to cy
    }

    /**
     * Tìm 1 ô không rắn gần (tx,ty) trong bán kính (Manhattan) cho phép – dùng khi spawn gốc bị kẹt.
     */
    fun findNearestFree(tx: Int, ty: Int, maxRadius: Int = 6): Pair<Int, Int>? {
        if (!isSolidAt(tx, ty)) return tx to ty
        for (r in 1..maxRadius) {
            // duyệt hình vuông viền r
            for (x in tx - r..tx + r) {
                val y1 = ty - r
                val y2 = ty + r
                if (!isSolidAt(x, y1)) return x to y1
                if (!isSolidAt(x, y2)) return x to y2
            }
            for (y in ty - (r - 1)..ty + (r - 1)) {
                val x1 = tx - r
                val x2 = tx + r
                if (!isSolidAt(x1, y)) return x1 to y
                if (!isSolidAt(x2, y)) return x2 to y
            }
        }
        return null
    }

    // ---------- build collision with override by upper layers ----------
    public fun buildCollisionFromLayers() {
        // reset
        for (y in 0 until mapHeight) {
            for (x in 0 until mapWidth) {
                collision[y][x] = false
                collisionSource[y][x] = null
            }
        }

        /**
         * Duyệt theo thứ tự layer từ DƯỚI lên TRÊN (như vẽ).
         * - Nếu layer.collider = true và có tile -> set true.
         * - Nếu layer nằm trong passThroughLayerNames và có tile -> set false (ghi đè).
         *
         * Lưu ý: JSON của m thường đã được load theo đúng thứ tự vẽ.
         * Nếu MapLoader đảo thứ tự để vẽ, đảm bảo ở đây dùng đúng thứ tự vẽ thật.
         */
        // Các loại có thể bị grass dọn sạch (soft colliders)
        val softOverrideable = setOf("Rock")
        val groundOverride = setOf("Grass") // ground lớp trên có thể xóa soft colliders bên dưới

        for (layer in layers) {
            val lname = layer.name.trim()
            val isPassThrough = lname in passThroughLayerNames

            // Nhồi lưới: cách nhanh – mỗi tile trong layer cập nhật một ô.
            for (tile in layer.tiles) {
                val tx = tile.x
                val ty = tile.y
                if (tx !in 0 until mapWidth || ty !in 0 until mapHeight) continue

                if (isPassThrough) {
                    // cầu/thang: luôn mở lối
                    collision[ty][tx] = false
                    collisionSource[ty][tx] = null
                } else if (layer.collider) {
                    // layer cản: đánh dấu rắn
                    collision[ty][tx] = true
                    collisionSource[ty][tx] = lname
                } else if (lname in groundOverride) {
                    // Grass: nếu nó nằm trên soft collider (ví dụ Rock) thì clear
                    val src = collisionSource[ty][tx]
                    if (src != null && src in softOverrideable) {
                        collision[ty][tx] = false
                        collisionSource[ty][tx] = null
                    }
                }
                // layer nền (collider=false và không thuộc pass-through): bỏ qua
            }
        }

        // Post-pass: Grass luôn dọn Rock phía dưới bất kể thứ tự layer.
        for (layer in layers) {
            val lname = layer.name.trim()
            if (lname == "Grass") {
                for (tile in layer.tiles) {
                    val tx = tile.x; val ty = tile.y
                    if (tx !in 0 until mapWidth || ty !in 0 until mapHeight) continue
                    val src = collisionSource[ty][tx]
                    if (src == "Rock") {
                        collision[ty][tx] = false
                        collisionSource[ty][tx] = null
                    }
                }
            }
        }

        // Post-pass 2: Bridge/Stair luôn mở đường (ghi đè nước/background) bất kể thứ tự.
        for (layer in layers) {
            val lname = layer.name.trim()
            if (lname in passThroughLayerNames) {
                for (tile in layer.tiles) {
                    val tx = tile.x; val ty = tile.y
                    if (tx !in 0 until mapWidth || ty !in 0 until mapHeight) continue
                    collision[ty][tx] = false
                    collisionSource[ty][tx] = null
                }
            }
        }
    }
}
