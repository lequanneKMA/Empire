package com.example.empire.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.empire.R

/** Quản lý nhạc nền đơn giản: enable/disable + volume, lưu vào SharedPreferences. */
object AudioManager {
    private const val PREF = "audio_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_VOLUME = "volume"

    private var media: MediaPlayer? = null
    private var initialized = false
    val isInitialized: Boolean get() = initialized
    var enabled: Boolean = true; private set
    var volume: Float = 1f; private set

    fun init(context: Context, musicRes: Int? = null) {
        if (initialized) return
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        enabled = sp.getBoolean(KEY_ENABLED, true)
        volume = sp.getFloat(KEY_VOLUME, 1f).coerceIn(0f,1f)
        if (musicRes != null) {
            try {
                media = MediaPlayer.create(context, musicRes)?.apply {
                    isLooping = true
                    setVolume(volume, volume)
                }
                if (enabled) media?.start()
            } catch (e: Exception) {
                Log.w("AudioManager", "Init media failed: ${e.message}")
            }
        }
        initialized = true
    }

    /** Khởi tạo bằng file trong assets (ví dụ "audio/soundbackground.mp3"). */
    fun initFromAssets(context: Context, assetPath: String) {
        if (initialized) return
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        enabled = sp.getBoolean(KEY_ENABLED, true)
        volume = sp.getFloat(KEY_VOLUME, 1f).coerceIn(0f,1f)
        try {
            val afd = context.assets.openFd(assetPath)
            media = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                prepare()
                setVolume(volume, volume)
            }
            if (enabled) media?.start()
        } catch (e: Exception) {
            Log.w("AudioManager", "Init asset media failed ($assetPath): ${e.message}")
        }
        initialized = true
    }

    fun toggleEnabled(context: Context) {
        setEnabled(context, !enabled)
    }

    fun setEnabled(context: Context, value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (value) {
            media?.seekTo(0)
            media?.start()
        } else {
            media?.pause()
        }
        persist(context)
    }

    fun setVolume(context: Context, v: Float) {
        val nv = v.coerceIn(0f,1f)
        if (nv == volume) return
        volume = nv
        media?.setVolume(volume, volume)
        persist(context)
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putFloat(KEY_VOLUME, volume)
            .apply()
    }
}