package com.example.empire.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.empire.R
import com.example.empire.audio.AudioManager
import com.example.empire.ui.common.McButton
import com.example.empire.ui.common.PixelFont
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(navController: NavController) {
    // Handle system back: quay lại Pause overlay nếu đến từ Gameplay
    BackHandler {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set("returnToPause", true)
        navController.popBackStack()
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Khởi tạo âm thanh 1 lần (nhạc chưa có => truyền null)
    LaunchedEffect(Unit) { AudioManager.init(context, null) }

    // Background music state
    var musicEnabled by remember { mutableStateOf(AudioManager.enabled) }
    var musicVolume by remember { mutableStateOf(AudioManager.volume) }
    // SFX state
    com.example.empire.audio.SfxManager.init(context)
    var sfxEnabled by remember { mutableStateOf(com.example.empire.audio.SfxManager.enabled) }
    var sfxVolume by remember { mutableStateOf(com.example.empire.audio.SfxManager.volume) }

    Box(Modifier.fillMaxSize()) {
        // Always-visible back button pinned at top-left
        McButton(
            text = "Quay lại",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            width = 110.dp,
            height = 34.dp,
            textSizeSp = 18
        ) {
            // báo cho GameplayScreen biết là cần hiện Pause overlay khi quay lại
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("returnToPause", true)
            navController.popBackStack()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CÀI ĐẶT ÂM THANH", fontFamily = PixelFont, fontSize = 30.sp, color = Color.White)
            Spacer(Modifier.height(28.dp))

            // Icon bật / tắt
            // Thử load icon từ assets/icons/OnSound & OffSound nếu có, fallback về drawable vectors
            val assetName = if (musicEnabled) "icons/OnSound.png" else "icons/OffSound.png"
            var bitmap by remember(musicEnabled) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(assetName) {
                try {
                    context.assets.open(assetName).use { inp ->
                        bitmap = android.graphics.BitmapFactory.decodeStream(inp)
                    }
                } catch (_: Exception) { bitmap = null }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = if (musicEnabled) "Sound On" else "Sound Off",
                    modifier = Modifier.size(96.dp).padding(8.dp)
                )
            } else {
                val iconRes = if (musicEnabled) R.drawable.on_sound else R.drawable.off_sound
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = if (musicEnabled) "Sound On" else "Sound Off",
                    modifier = Modifier.size(96.dp).padding(8.dp)
                )
            }
            McButton(if (musicEnabled) "Tắt nhạc" else "Bật nhạc") {
                AudioManager.toggleEnabled(context)
                musicEnabled = AudioManager.enabled
            }
            Spacer(Modifier.height(32.dp))
            Text("Âm lượng nhạc: ${(musicVolume*100).toInt()}%", fontFamily = PixelFont, fontSize = 22.sp, color = Color.White)
            Slider(
                value = musicVolume,
                onValueChange = {
                    musicVolume = it
                    AudioManager.setVolume(context, it)
                },
                valueRange = 0f..1f,
                modifier = Modifier.width(240.dp)
            )
            Spacer(Modifier.height(40.dp))

            // SFX Section
            Text("HIỆU ỨNG (SFX)", fontFamily = PixelFont, fontSize = 26.sp, color = Color.White)
            Spacer(Modifier.height(16.dp))
            McButton(if (sfxEnabled) "Tắt SFX" else "Bật SFX") {
                com.example.empire.audio.SfxManager.toggleEnabled(context)
                sfxEnabled = com.example.empire.audio.SfxManager.enabled
            }
            Spacer(Modifier.height(16.dp))
            Text("Âm lượng SFX: ${(sfxVolume*100).toInt()}%", fontFamily = PixelFont, fontSize = 22.sp, color = Color.White)
            Slider(
                value = sfxVolume,
                onValueChange = {
                    sfxVolume = it
                    com.example.empire.audio.SfxManager.setVolume(context, it)
                },
                valueRange = 0f..1f,
                modifier = Modifier.width(240.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
