package com.example.empire.game.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject

object MapLoader {

    /**
     * Đọc JSON cũ (tileSize/mapWidth/mapHeight/layers[...] với tiles có id,x,y),
     * và load 1 tileset PNG. spacing/margin mặc định = 0 (đổi nếu spritesheet có khoảng cách).
     */
    fun loadFromAssets(
        context: Context,
        mapFile: String,
        tilesetFile: String,
        spacing: Int = 0,
        margin: Int = 0
    ): TileMap {
        // --- đọc JSON ---
        val jsonText = context.assets.open(mapFile).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)

        val tileSize  = root.getInt("tileSize")
        val mapWidth  = root.getInt("mapWidth")
        val mapHeight = root.getInt("mapHeight")

    // --- optional spawn points ---
    val spawnPointsObj = root.optJSONObject("spawnPoints")

        // --- load tileset bitmap (ARGB_8888, giữ alpha) ---
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = false
            inScaled = false
        }
        val bmp = BitmapFactory.decodeStream(context.assets.open(tilesetFile), null, opts)
            ?: error("Không load được tileset: $tilesetFile")

        // Tính số cột từ bitmap width + spacing/margin
        val effW = bmp.width - margin * 2
        val stride = tileSize + spacing
        val columns = ((effW + spacing) / stride).coerceAtLeast(1)

        // --- parse layers ---
        val layersJson = root.getJSONArray("layers")
        val layers = ArrayList<Layer>(layersJson.length())
        for (i in 0 until layersJson.length()) {
            val layerObj = layersJson.getJSONObject(i)
            val name = layerObj.getString("name")
            val collider = layerObj.optBoolean("collider", false)
            val tilesArr = layerObj.getJSONArray("tiles")
            val tiles = parseTiles(tilesArr)
            layers += Layer(name = name, tiles = tiles, collider = collider)
        }

        val tileset = Tileset(
            bitmap = bmp,
            columns = columns,
            tileSize = tileSize,
            spacing = spacing,
            margin = margin
        )

        val map = TileMap(
            tileSize = tileSize,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            tileset = tileset,
            layers = layers
        )
        map.buildCollisionFromLayers()     // <-- thêm

        // Ghi spawn points vào map
        if (spawnPointsObj != null) {
            val keys = spawnPointsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = spawnPointsObj.optJSONArray(key) ?: continue
                if (arr.length() >= 2) {
                    val sx = arr.optInt(0, 0)
                    val sy = arr.optInt(1, 0)
                    map.spawnPoints[key] = sx to sy
                }
            }
        }
        return map
    }

    // JSON cũ: id có thể là "string" hoặc int
    private fun parseTiles(arr: JSONArray): List<Tile> {
        val out = ArrayList<Tile>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val rawId = if (o.has("id")) o.get("id") else 0
            val id = when (rawId) {
                is Int -> rawId
                is String -> rawId.toIntOrNull() ?: 0
                else -> 0
            }
            // JSON cũ: id >= 0 đều là tile hợp lệ (ô rỗng không được ghi vào mảng)
            val x = o.getInt("x")
            val y = o.getInt("y")
            out += Tile(id = id, x = x, y = y)
        }
        return out
    }
}
