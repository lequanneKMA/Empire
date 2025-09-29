package com.example.empire.game

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.empire.game.map.MapLoader
import com.example.empire.game.map.MapRenderer




class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var running = false
    private var gameThread: Thread? = null

    // Map
    private val map = MapLoader.loadFromAssets(context, "maps/map_main.json", "maps/tileset.png")
    private val renderer = MapRenderer(map)

    // Camera
    private var camX = 0
    private var camY = 0

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        gameThread?.join()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun run() {
        while (running) {
            val canvas: Canvas = holder.lockCanvas() ?: continue
            synchronized(holder) {
                drawGame(canvas)
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGame(canvas: Canvas) {
        // Xoá màn hình
        canvas.drawRGB(0, 0, 0)

        // Vẽ maps
        renderer.draw(canvas, camX, camY, width, height)

        // Sau này thêm player, HUD, NPC ở đây
    }
}
