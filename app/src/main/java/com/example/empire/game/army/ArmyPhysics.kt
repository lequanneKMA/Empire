package com.example.empire.game.army

import com.example.empire.game.map.TileMap

/** Simple physics step for army units mirroring player/enemy collision (axis separated). */
class ArmyPhysics(private var map: TileMap?) {
    data class ABody(var x: Float, var y: Float, var w: Float, var h: Float, var vx: Float = 0f, var vy: Float = 0f)

    fun setMap(m: TileMap?) { map = m }

    fun step(body: ABody, dt: Float) {
        val m = map ?: run { body.x += body.vx * dt; body.y += body.vy * dt; return }
        val nx = body.x + body.vx * dt
        val ny = body.y + body.vy * dt
        if (!collides(m, nx, body.y, body.w, body.h)) body.x = nx
        if (!collides(m, body.x, ny, body.w, body.h)) body.y = ny
    }

    private fun collides(map: TileMap, x: Float, y: Float, w: Float, h: Float): Boolean {
        val ts = map.tileSize
        val left = (x / ts).toInt()
        val right = ((x + w - 1) / ts).toInt()
        val top = (y / ts).toInt()
        val bottom = ((y + h - 1) / ts).toInt()
        for (ty in top..bottom) for (tx in left..right) if (map.isSolidAt(tx, ty)) return true
        return false
    }
}