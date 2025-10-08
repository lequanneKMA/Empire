package com.example.empire.ui.settings

import androidx.compose.foundation.Image
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
    val context = androidx.compose.ui.platform.LocalContext.current
    // Khởi tạo âm thanh 1 lần (nhạc chưa có => truyền null)
    LaunchedEffect(Unit) { AudioManager.init(context, null) }

    var enabled by remember { mutableStateOf(AudioManager.enabled) }
    var volume by remember { mutableStateOf(AudioManager.volume) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CÀI ĐẶT ÂM THANH", fontFamily = PixelFont, fontSize = 30.sp, color = Color.White)
            Spacer(Modifier.height(28.dp))

            // Icon bật / tắt
            // Thử load icon từ assets/icons/OnSound & OffSound nếu có, fallback về drawable vectors
            val assetName = if (enabled) "icons/OnSound.png" else "icons/OffSound.png"
            var bitmap by remember(enabled) { mutableStateOf<android.graphics.Bitmap?>(null) }
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
                    contentDescription = if (enabled) "Sound On" else "Sound Off",
                    modifier = Modifier.size(96.dp).padding(8.dp)
                )
            } else {
                val iconRes = if (enabled) R.drawable.on_sound else R.drawable.off_sound
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = if (enabled) "Sound On" else "Sound Off",
                    modifier = Modifier.size(96.dp).padding(8.dp)
                )
            }
            McButton(if (enabled) "Tắt nhạc" else "Bật nhạc") {
                AudioManager.toggleEnabled(context)
                enabled = AudioManager.enabled
            }
            Spacer(Modifier.height(32.dp))
            Text("Âm lượng: ${(volume*100).toInt()}%", fontFamily = PixelFont, fontSize = 22.sp, color = Color.White)
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    AudioManager.setVolume(context, it)
                },
                valueRange = 0f..1f,
                modifier = Modifier.width(240.dp)
            )
            Spacer(Modifier.height(40.dp))
            McButton("Quay lại") { navController.popBackStack() }
        }
    }
}
