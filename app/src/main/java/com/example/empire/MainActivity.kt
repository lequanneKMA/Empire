package com.example.empire

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.example.empire.ui.AppNav
import com.example.empire.audio.AudioManager
import com.example.empire.audio.SfxManager

class MainActivity : ComponentActivity() {

    private fun goImmersiveSticky() {
        val w = window
        WindowCompat.setDecorFitsSystemWindows(w, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            w.attributes = w.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val c = WindowInsetsControllerCompat(w, w.decorView)
            c.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goImmersiveSticky()
        // Khởi tạo nhạc nền từ assets (đặt file tại: app/src/main/assets/audio/soundbackground.mp3)
    AudioManager.initFromAssets(this, "audio/soundbackground.mp3")
    SfxManager.init(this)
        setContent { AppNav(rememberNavController()) }
    }

    // Re-apply khi app quay lại foreground
    override fun onResume() {
        super.onResume()
        goImmersiveSticky()
    }

    // Một số máy chỉ ẩn được khi có focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersiveSticky()
    }
}

