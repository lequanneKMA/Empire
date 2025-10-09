package com.example.empire.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.empire.R
import com.example.empire.ui.Screen                // <-- dùng Screen.route
import com.example.empire.ui.common.McButton
import androidx.compose.ui.platform.LocalContext
import com.example.empire.data.SaveManager
import com.example.empire.ui.gameplay.GameViewHolder

@Composable
fun StartScreen(nav: NavController) {
    val ctx = LocalContext.current
    val sm = remember { SaveManager(ctx) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_pixel),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val yBelowTitle = maxHeight * 0.44f  // chỉnh 0.34–0.44 tùy ảnh

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = yBelowTitle),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            McButton("Bắt đầu") {
                // New Game: clear in-memory GameView and persistent Save
                GameViewHolder.gameView = null
                sm.clear()
                nav.navigate(Screen.Gameplay.route)
            }
            Spacer(Modifier.height(13.dp))
            McButton("Load") { nav.navigate(Screen.SelectSave.route) }
            Spacer(Modifier.height(13.dp))
            McButton("Cài đặt") { nav.navigate(Screen.Settings.route) }
            Spacer(Modifier.height(13.dp))
            McButton("Thoát") { /* TODO: finish() */ }
        }
    }
}
