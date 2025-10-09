package com.example.empire.ui.load

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.empire.data.SaveManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.empire.ui.gameplay.GameViewHolder

@Composable
fun SelectSaveScreen(nav: NavController) {
    val ctx = LocalContext.current
    val sm = remember { SaveManager(ctx) }
    val confirmSaveSlot = remember { mutableStateOf<Int?>(null) }
    val confirmDeleteSlot = remember { mutableStateOf<Int?>(null) }
    var tick by remember { mutableStateOf(0) } // trigger recomposition on operations

    fun fmtTime(ts: Long?): String {
        if (ts == null) return "--:--"
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    val slots = remember(tick) {
        sm.listSlotSummaries(5)
    }

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
                itemsIndexed(slots) { index, summary ->
                    // Hàng: nút chọn save + nút xóa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // nút chọn save
                        PixelButtonPro(
                            text = buildString {
                                append("Slot ${index + 1} • ")
                                if (summary.exists) {
                                    append("Lv.")
                                    append(summary.level ?: 1)
                                    append(" • ")
                                    append(fmtTime(summary.savedAt))
                                } else append("(trống)")
                            },
                            minWidthDp = 300.dp,
                            heightDp = 60.dp,
                            fontSizeSp = 30
                        ) {
                            val loaded = sm.loadFromSlot(index)
                            if (loaded != null) {
                                println("[SAVE SLOT] Đã nạp slot ${index + 1}: map=${loaded.mapId} lv=${loaded.tier + 1}")
                                // Áp dụng state vào GameView đang tồn tại (nếu có) để load ngay giữa game
                                GameViewHolder.gameView?.applyLoadedState(loaded)
                                GameViewHolder.gameView?.setPausedFromCompose(false)
                                // Đảm bảo unpause khi quay lại gameplay
                                nav.previousBackStackEntry?.savedStateHandle?.set("returnToPause", false)
                                nav.navigate(Screen.Gameplay.route)
                            } else {
                                println("[SAVE SLOT] Slot ${index + 1} trống (không có gì để nạp)")
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // nút Lưu (mở xác nhận)
                        PixelButtonPro(
                            text = "Lưu",
                            minWidthDp = 110.dp,
                            heightDp = 60.dp,
                            fontSizeSp = 28
                        ) {
                            confirmSaveSlot.value = index
                            println("bạn có muốn lưu game vào slot ${index + 1}")
                        }

                        Spacer(Modifier.width(10.dp))

                        // nút xóa (nhỏ, đỏ)
                        PixelDangerButton(
                            text = "Xóa",
                            minWidthDp = 100.dp,
                            heightDp = 60.dp,
                            fontSizeSp = 30
                        ) {
                            confirmDeleteSlot.value = index
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
            // If opened from pause overlay during gameplay, go back to pause instead of Start
            nav.previousBackStackEntry?.savedStateHandle?.set("returnToPause", true)
            nav.popBackStack()
        }

        // Overlay xác nhận lưu
        val pending = confirmSaveSlot.value
        if (pending != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                        .align(Alignment.Center)
                ) {}

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bạn có muốn lưu game vào slot ${pending + 1}?",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xCC000000),
                                offset = Offset(2f, 2f),
                                blurRadius = 2f
                            )
                        ),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PixelButtonPro(
                            text = "Đồng ý",
                            minWidthDp = 140.dp,
                            heightDp = 56.dp,
                            fontSizeSp = 25
                        ) {
                            val ok = sm.saveToSlot(pending)
                            if (ok) println("[SAVE SLOT] Đồng ý lưu vào slot ${pending + 1}") else println("[SAVE SLOT] Lưu thất bại slot ${pending + 1}")
                            confirmSaveSlot.value = null
                            tick++
                        }
                        PixelButtonPro(
                            text = "Từ chối",
                            minWidthDp = 140.dp,
                            heightDp = 56.dp,
                            fontSizeSp = 25
                        ) {
                            println("[SAVE SLOT] Từ chối lưu vào slot ${pending + 1}")
                            confirmSaveSlot.value = null
                        }
                    }
                }
            }
        }

        // Overlay xác nhận xóa
        val del = confirmDeleteSlot.value
        if (del != null) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000))
                        .align(Alignment.Center)
                ) {}

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xóa dữ liệu slot ${del + 1}?",
                        fontFamily = PixelFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xCC000000),
                                offset = Offset(2f, 2f),
                                blurRadius = 2f
                            )
                        ),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PixelDangerButton(
                            text = "Xóa",
                            minWidthDp = 140.dp,
                            heightDp = 56.dp,
                            fontSizeSp = 25
                        ) {
                            sm.clearSlot(del)
                            println("[SAVE SLOT] Đã xóa slot ${del + 1}")
                            confirmDeleteSlot.value = null
                            tick++
                        }
                        PixelButtonPro(
                            text = "Hủy",
                            minWidthDp = 140.dp,
                            heightDp = 56.dp,
                            fontSizeSp = 25
                        ) {
                            println("[SAVE SLOT] Không xóa slot ${del + 1}")
                            confirmDeleteSlot.value = null
                        }
                    }
                }
            }
        }
    }
}
