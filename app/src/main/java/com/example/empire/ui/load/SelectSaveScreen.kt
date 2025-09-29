package com.example.empire.ui.load

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.empire.R
import com.example.empire.ui.Screen                      // <- dùng Screen.route
import com.example.empire.ui.common.PixelButtonPro
import com.example.empire.ui.common.PixelDangerButton
import com.example.empire.ui.common.PixelFont

@Composable
fun SelectSaveScreen(nav: NavController) {
    val saves = listOf(
        "Slot 1 • Lv.5 • 12:34",
        "Slot 2 • Lv.2 • 03:12",
        "Slot 3 • (trống)",
        "Slot 4 • demo",
        "Slot 5 • demo"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.sky_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tiêu đề
            Text(
                text = "CHỌN SAVE",
                fontFamily = PixelFont,
                fontWeight = FontWeight.Normal,
                fontSize = 35.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xAA000000),
                        offset = Offset(2f, 2f),
                        blurRadius = 2f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            // Danh sách slot (cuộn)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // chiếm phần còn lại để cuộn
            ) {
                itemsIndexed(saves) { index, label ->
                    // Hàng: nút chọn save + nút xóa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // nút chọn save
                        PixelButtonPro(
                            text = label,
                            minWidthDp = 300.dp,
                            heightDp = 60.dp,
                            fontSizeSp = 30
                        ) {
                            // TODO: SaveManager.load(index)
                            nav.navigate(Screen.Gameplay.route)   // <-- SỬA Ở ĐÂY
                        }

                        Spacer(Modifier.width(10.dp))

                        // nút xóa (nhỏ, đỏ)
                        PixelDangerButton(
                            text = "Xóa",
                            minWidthDp = 100.dp,
                            heightDp = 60.dp,
                            fontSizeSp = 30
                        ) {
                            // TODO: xác nhận rồi xóa slot index
                            // showConfirmDelete(index)
                        }
                    }
                }
            }
        }

        // Nút quay lại ở góc phải dưới
        PixelButtonPro(
            text = "Quay lại",
            minWidthDp = 100.dp,
            heightDp = 52.dp,
            fontSizeSp = 25,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            nav.popBackStack()
        }
    }
}
