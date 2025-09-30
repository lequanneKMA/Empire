package com.example.empire.game.ecs.systems

import com.example.empire.game.map.TileMap
import kotlin.math.floor
class PhysicsSystem(private val map: TileMap) {

    data class Body(
        var x: Float,
        var y: Float,
        var w: Float,
        var h: Float,
        var vx: Float = 0f,
        var vy: Float = 0f
    )

    fun step(body: Body, dt: Float) {
        val nx = body.x + body.vx * dt
        val ny = body.y + body.vy * dt

        if (!collides(nx, body.y, body.w, body.h)) {
            body.x = nx
        }
        if (!collides(body.x, ny, body.w, body.h)) {
            body.y = ny
        }
    }

    private fun collides(x: Float, y: Float, w: Float, h: Float): Boolean {
        val left = (x / map.tileSize).toInt()
        val right = ((x + w - 1) / map.tileSize).toInt()
        val top = (y / map.tileSize).toInt()
        val bottom = ((y + h - 1) / map.tileSize).toInt()

        for (ty in top..bottom) {
            for (tx in left..right) {
                if (map.isSolidAt(tx, ty)) return true
            }
        }
        return false
    }
}
