package com.example.empire.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.empire.R

/** Sound effects manager dùng SoundPool cho hiệu ứng ngắn. */
object SfxManager {
    private var soundPool: SoundPool? = null
    private var loaded = false
    private var playerAttackId = 0
    private var slimeAttackId = 0
    private var wolfAttackId = 0
    private var monsterAttackId = 0
    private var flybeeAttackId = 0
    private var lastStreamSlime = 0
    private var lastStreamWolf = 0
    private var lastStreamMonster = 0
    private var lastStreamFlybee = 0

    fun init(context: Context) {
        if (loaded) return
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        // Map resource ids
        playerAttackId = soundPool!!.load(context, R.raw.sword_attack, 1)
        slimeAttackId = safeLoad(context, "slime_attack")
        wolfAttackId = safeLoad(context, "wolf_attack")
        monsterAttackId = safeLoad(context, "monster_attack")
        flybeeAttackId = safeLoad(context, "flybee_attack")
        loaded = true
    }

    private fun safeLoad(ctx: Context, name: String): Int {
        val resId = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        return if (resId != 0) soundPool!!.load(ctx, resId, 1) else 0
    }

    private fun canPlay(): Boolean = loaded && AudioManager.enabled

    fun playPlayerAttack() {
        if (!canPlay()) return
        soundPool?.play(playerAttackId, AudioManager.volume, AudioManager.volume, 1, 0, 1f)
    }
    fun playEnemyAttack(type: String) { // type = enum name (SLIME/WOLF/...)
        if (!canPlay()) return
        val id = when(type.uppercase()) {
            "SLIME" -> slimeAttackId
            "WOLF" -> wolfAttackId.takeIf { it != 0 } ?: slimeAttackId
            "MONSTER" -> monsterAttackId.takeIf { it != 0 } ?: slimeAttackId
            "FLYBEE" -> flybeeAttackId.takeIf { it != 0 } ?: slimeAttackId
            else -> slimeAttackId
        }
        if (id != 0) {
            val rate = if (type.equals("FLYBEE", true)) 1.4f else 1f // rút ngắn cảm giác âm flybee
            val streamId = soundPool?.play(id, AudioManager.volume, AudioManager.volume, 1, 0, rate) ?: 0
            when(type.uppercase()) {
                "SLIME" -> lastStreamSlime = streamId
                "WOLF" -> lastStreamWolf = streamId
                "MONSTER" -> lastStreamMonster = streamId
                "FLYBEE" -> lastStreamFlybee = streamId
            }
        }
    }

    fun stopEnemyAttackForType(type: String) {
        val sp = soundPool ?: return
        val streamId = when(type.uppercase()) {
            "SLIME" -> lastStreamSlime
            "WOLF" -> lastStreamWolf
            "MONSTER" -> lastStreamMonster
            "FLYBEE" -> lastStreamFlybee
            else -> 0
        }
        if (streamId != 0) sp.stop(streamId)
    }
    fun release() { soundPool?.release(); soundPool = null; loaded = false }
}