package com.example.empire.game.ecs.systems

import android.graphics.Canvas
import com.example.empire.core.gfx.PlayerAnimator
import com.example.empire.game.map.MapRenderer

/**
 * Render map + player (và sau này cả enemies, particles…).
 * Chỉ xử lý vẽ, không có logic.
 */
class RenderSystem(
    private var renderer: MapRenderer,
    private val playerAnimator: PlayerAnimator
) {
    fun setRenderer(newRenderer: MapRenderer) { this.renderer = newRenderer }
    /**
     * Vẽ theo camera & scale. Player đã đồng bộ x/y từ physics trước đó.
     *
     * @param canvas Canvas
     * @param camX, camY: toạ độ pixel camera (góc trái trên)
     * @param vw, vh: kích thước viewport theo world pixel (đÃ CHIA scale)
     * @param scaleFactor: tỉ lệ phóng
     * @param playerX, playerY: toạ độ world (pixel) của player (góc trái trên hitbox)
     * @param playerW, playerH: kích thước hitbox player (world pixel chưa scale)
     * @param spriteScale: tỉ lệ phóng riêng cho sprite player
     */
    fun draw(
        canvas: Canvas,
        camX: Int, camY: Int,
        vw: Int, vh: Int,
        scaleFactor: Float,
        playerX: Float, playerY: Float,
        playerW: Int, playerH: Int,
        spriteScale: Float
    ) {
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)

        // map
        renderer.draw(canvas, camX, camY, vw, vh)

        // player
        val sx = (playerX - camX).toInt()
        val sy = (playerY - camY).toInt()
        val dstW = (playerW * spriteScale).toInt()
        val dstH = (playerH * spriteScale).toInt()

        // căn chân và tâm
        val anchorX = sx - (dstW - playerW) / 2
        val anchorY = sy - (dstH - playerH)

        playerAnimator.draw(canvas, anchorX, anchorY, dstW, dstH)

        canvas.restore()
    }
}
