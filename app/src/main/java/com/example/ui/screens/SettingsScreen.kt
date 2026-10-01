package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlgorithmWeightEntity
import com.example.ui.components.PersianConfirmDialog
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    weights: AlgorithmWeightEntity,
    onSaveWeights: (AlgorithmWeightEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualW by remember(weights) { mutableStateOf(weights.manualWeight) }
    var suspiciousW by remember(weights) { mutableStateOf(weights.suspiciousNoteWeight) }
    var spreadW by remember(weights) { mutableStateOf(weights.targetSpreadWeight) }
    var flipsW by remember(weights) { mutableStateOf(weights.targetFlipWeight) }
    var votesW by remember(weights) { mutableStateOf(weights.votesReceivedWeight) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Algorithm Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ تنظیم ضرایب الگوریتم شفاف سوءظن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تعیین میزان تأثیر فاکتورهای مختلف در محاسبه خودکار امتیاز سوءظن هر بازیکن",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    WeightSlider(
                        title = "وزن نظر شهودی کاربر (امتیاز دستی)",
                        value = manualW,
                        onValueChange = { manualW = it }
                    )

                    WeightSlider(
                        title = "وزن رفتارهای مشکوک ثبت‌شده",
                        value = suspiciousW,
                        onValueChange = { suspiciousW = it }
                    )

                    WeightSlider(
                        title = "وزن پراکندگی و تعداد بالای تارگت‌ها",
                        value = spreadW,
                        onValueChange = { spreadW = it }
                    )

                    WeightSlider(
                        title = "وزن تغییر و نوسان تارگت‌ها بین روزها",
                        value = flipsW,
                        onValueChange = { flipsW = it }
                    )

                    WeightSlider(
                        title = "وزن آرای خروج دریافتی",
                        value = votesW,
                        onValueChange = { votesW = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveWeights(
                                    weights.copy(
                                        manualWeight = manualW,
                                        suspiciousNoteWeight = suspiciousW,
                                        targetSpreadWeight = spreadW,
                                        targetFlipWeight = flipsW,
                                        votesReceivedWeight = votesW
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_save_weights")
                        ) {
                            Text("ذخیره تغییرات ضرایب", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                manualW = 0.35f
                                suspiciousW = 0.25f
                                spreadW = 0.15f
                                flipsW = 0.15f
                                votesW = 0.10f
                                onSaveWeights(
                                    weights.copy(
                                        manualWeight = 0.35f,
                                        suspiciousNoteWeight = 0.25f,
                                        targetSpreadWeight = 0.15f,
                                        targetFlipWeight = 0.15f,
                                        votesReceivedWeight = 0.10f
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("پیش‌فرض", color = TextSecondaryDark)
                        }
                    }
                }
            }
        }

        // About & Privacy Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 حریم خصوصی و امنیت داده‌ها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = SuspicionGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• این برنامه صرفاً دفترچه یادداشت تحلیلی شخصی است و نقش واقعی هیچ بازیکنی را نمی‌پرسد.\n" +
                                "• تمام داده‌های تارگت‌ها، یادداشت‌ها و امتیازات شما به صورت کاملاً آفلاین روی حافظه دستگاه ذخیره می‌شوند.\n" +
                                "• هیچ مجوزی برای دوربین، میکروفون، مخاطبین یا موقعیت مکانی نیاز نیست.\n" +
                                "• قابلیت تحلیل هوش مصنوعی کاملاً اختیاری است و فقط با فشردن دکمه صریح توسط شما فراخوانی می‌شود.",
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 13.sp, color = TextPrimaryDark)
            Text(text = "${(value * 100).roundToInt()}%", fontSize = 13.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.05f..0.60f,
            colors = SliderDefaults.colors(
                thumbColor = MafiaGold,
                activeTrackColor = MafiaCrimson,
                inactiveTrackColor = MafiaSurfaceVariant
            )
        )
    }
}
