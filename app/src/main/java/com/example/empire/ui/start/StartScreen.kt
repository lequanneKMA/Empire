package com.example.empire.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.empire.R
import com.example.empire.ui.Screen                // <-- dùng Screen.route
import com.example.empire.ui.common.McButton

@Composable
fun StartScreen(nav: NavController) {
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
            McButton("Bắt đầu") { nav.navigate(Screen.Gameplay.route) }
            Spacer(Modifier.height(13.dp))
            McButton("Tiếp tục") { nav.navigate(Screen.SelectSave.route) }
            Spacer(Modifier.height(13.dp))
            McButton("Cài đặt") { nav.navigate(Screen.Settings.route) }
            Spacer(Modifier.height(13.dp))
            McButton("Thoát") { /* TODO: finish() */ }
        }
    }
}
