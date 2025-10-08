package com.example.empire.game.ai

import com.example.empire.game.army.UnitEntity
import com.example.empire.game.map.TileMap
import kotlin.math.abs

/**
 * Handles higher-level movement + path usage for army units.
 * Responsibilities:
 *  - Decide when to (re)path
 *  - Follow current path with basic smoothing / LOS skipping
 *  - Stuck detection
 */
class ArmyAIController(
    private val map: TileMap,
    private val mover: (UnitEntity, Float, Float) -> Boolean
) {

    // Repath when target tile changed or every N frames while moving.
    private val repathInterval = 45 // frames
    private val stuckVelocityThreshold = 2f
    private val stuckCheckInterval = 30

    fun update(unit: UnitEntity, targetX: Float, targetY: Float, frame: Int, dt: Float, moveSpeed: Float) {
        val tileSize = map.tileSize
        val ux = unit.x
        val uy = unit.y

        // Basic stuck detection: if almost not moving across interval trigger repath
        if (frame % stuckCheckInterval == 0) {
            val dx = ux - unit.lastPosX
            val dy = uy - unit.lastPosY
            val dist = kotlin.math.sqrt(dx*dx + dy*dy)
            if (dist < stuckVelocityThreshold) {
                unit.path?.clear() // force repath
                unit.pathIndex = 0
            }
            unit.lastPosX = ux
            unit.lastPosY = uy
        }

        val targetTileX = (targetX / tileSize).toInt()
        val targetTileY = (targetY / tileSize).toInt()
        val unitTileX = (ux / tileSize).toInt()
        val unitTileY = (uy / tileSize).toInt()

        val needRepath = unit.path == null || unit.pathIndex >= (unit.path?.size ?: 0) ||
                frame % repathInterval == 0 || // periodic refresh
                !unit.hasLineOfSight(map, targetTileX, targetTileY) // lost LOS

        if (needRepath) {
            val newPath = Pathfinder.find(map, unitTileX, unitTileY, targetTileX, targetTileY)
            if (newPath != null) {
                unit.path = newPath.toMutableList()
                unit.pathIndex = 0
            }
        }

        follow(unit, targetX, targetY, dt, moveSpeed)
    }

    private fun follow(unit: UnitEntity, targetX: Float, targetY: Float, dt: Float, moveSpeed: Float) {
        val path = unit.path
        if (path == null || path.isEmpty()) {
            // Direct steering toward target
            val dx = targetX - unit.x
            val dy = targetY - unit.y
            val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > 1f) {
                val step = moveSpeed * dt
                val mx = (dx / dist) * step
                val my = (dy / dist) * step
                mover(unit, mx, my)
            }
            return
        }
        if (unit.pathIndex >= path.size) {
            unit.moveToward(targetX, targetY)
            return
        }
        // Path smoothing: Skip ahead while LOS to further nodes
        var lookAhead = unit.pathIndex
        val maxSkip = 3
        while (lookAhead < path.size && lookAhead - unit.pathIndex <= maxSkip) {
            val node = path[lookAhead]
            if (!unit.hasLineOfSight(map, node.first, node.second)) break
            lookAhead++
        }
        if (lookAhead - 1 > unit.pathIndex) unit.pathIndex = lookAhead - 1

        val (tx, ty) = path[unit.pathIndex]
        val centerX = tx * map.tileSize + map.tileSize * 0.5f
        val centerY = ty * map.tileSize + map.tileSize * 0.5f
        val dx = centerX - unit.x
        val dy = centerY - unit.y
        val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist < 4f) {
            unit.pathIndex++
            return
        }
        val step = moveSpeed * dt
        val mx = (dx / dist) * step
        val my = (dy / dist) * step
        mover(unit, mx, my)
    }

    // ================= Line of Sight (tile-based Bresenham) =================
    private fun bresenhamLOS(map: TileMap, x0:Int, y0:Int, x1:Int, y1:Int): Boolean {
        var cx = x0
        var cy = y0
        var dx = kotlin.math.abs(x1 - x0)
        var dy = -kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            if (map.isSolidAt(cx, cy)) return false
            if (cx == x1 && cy == y1) return true
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; cx += sx }
            if (e2 <= dx) { err += dx; cy += sy }
        }
    }

    private fun UnitEntity.hasLineOfSight(map: TileMap, tx:Int, ty:Int): Boolean {
        val ts = map.tileSize
        val sx = (x / ts).toInt()
        val sy = (y / ts).toInt() // using bottom anchor tile; precision not critical for LOS
        return bresenhamLOS(map, sx, sy, tx, ty)
    }
}
