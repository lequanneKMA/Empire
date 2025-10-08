package com.example.empire.game.ai

import com.example.empire.game.map.TileMap
import java.util.PriorityQueue

/**
 * Simple reusable A* pathfinder (4-direction) with small optimizations.
 * Returns list of (tx,ty) tile coordinates or null if no path.
 */
object Pathfinder {
    private data class Node(val x:Int,val y:Int,val g:Float,val h:Float,val f:Float,val parent: Node?)

    fun find(map: TileMap, sx:Int, sy:Int, gx:Int, gy:Int, maxNodes:Int = 1200): List<Pair<Int,Int>>? {
        if (sx == gx && sy == gy) return emptyList()
        if (gx !in 0 until map.mapWidth || gy !in 0 until map.mapHeight) return null
        if (map.isSolidAt(gx, gy)) return null
        val open = PriorityQueue<Node>(compareBy<Node>{ it.f })
        val closed = HashSet<Long>()
        fun key(x:Int,y:Int) = (x.toLong() shl 32) xor y.toLong()
        fun h(x:Int,y:Int) = (kotlin.math.abs(gx - x) + kotlin.math.abs(gy - y)).toFloat()
        val h0 = h(sx,sy)
        open += Node(sx, sy, 0f, h0, h0, null)
        val dirs = intArrayOf(1,0, -1,0, 0,1, 0,-1)
        var explored = 0
        while (open.isNotEmpty() && explored < maxNodes) {
            explored++
            val cur = open.poll()
            if (cur.x == gx && cur.y == gy) return reconstruct(cur)
            if (!closed.add(key(cur.x, cur.y))) continue
            for (i in 0 until 4) {
                val nx = cur.x + dirs[i*2]
                val ny = cur.y + dirs[i*2+1]
                if (nx !in 0 until map.mapWidth || ny !in 0 until map.mapHeight) continue
                if (map.isSolidAt(nx, ny)) continue
                val g = cur.g + 1f
                val hh = h(nx, ny)
                val f = g + hh
                open += Node(nx, ny, g, hh, f, cur)
            }
        }
        return null
    }

    private fun reconstruct(goal: Node): List<Pair<Int,Int>> {
        val out = ArrayList<Pair<Int,Int>>()
        var c: Node? = goal
        while (c != null) { out.add(0, c.x to c.y); c = c.parent }
        return out
    }
}
