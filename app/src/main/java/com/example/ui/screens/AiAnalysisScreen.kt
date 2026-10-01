package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiAnalysisResult
import com.example.data.local.AiSettingsEntity
import com.example.data.local.GameEntity
import com.example.data.model.GameStage
import com.example.ui.components.PersianConfirmDialog
import com.example.ui.theme.*

@Composable
fun AiAnalysisScreen(
    activeGame: GameEntity?,
    currentStageIndex: Int,
    aiResult: AiAnalysisResult?,
    aiSettings: AiSettingsEntity? = null,
    onRunAnalysis: () -> Unit,
    onClearAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    val stage = GameStage.getStage(currentStageIndex)

    if (showPrivacyDialog) {
        PersianConfirmDialog(
            title = "حفظ حریم خصوصی و شروع تحلیل AI",
            message = "داده‌های ثبت‌شده (شامل اسامی، تارگت‌ها، یادداشت‌ها و آرای بازی) برای تحلیل الگوها به سرویس هوش مصنوعی ارسال خواهد شد.\n\nاین قابلیت اختیاری است و بدون اینترنت نیز تحلیل عمیق محلی در دسترس است. آیا مایل به شروع تحلیل هستید؟",
            confirmText = "ارسال و تحلیل",
            cancelText = "انصراف",
            onConfirm = {
                showPrivacyDialog = false
                onRunAnalysis()
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // AI Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤖 تحلیل هوشمند هوش مصنوعی",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB388FF)
                            )
                            Text(
                                text = "بررسی چندوجهی تارگت‌ها، تناقض‌ها، تقابل‌ها و خطوط ائتلاف تا ${stage.title}",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = if (aiSettings?.isRouterConfigured == true) {
                                    "🔌 سرویس فعال: مدل ${aiSettings.model}"
                                } else {
                                    "📴 سرویس خارجی تنظیم نشده — از تحلیلگر محلی/کلید داخلی استفاده می‌شود (تنظیمات ← هوش مصنوعی)"
                                },
                                fontSize = 11.sp,
                                color = if (aiSettings?.isRouterConfigured == true) SuspicionGreen else TextMutedDark
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFB388FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPrivacyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_start_ai_analysis")
                        ) {
                            Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (aiResult == null) "شروع تحلیل با هوش مصنوعی" else "تحلیل مجدد وضعیت",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (aiResult != null && aiResult !is AiAnalysisResult.Loading) {
                            OutlinedButton(
                                onClick = onClearAnalysis,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "پاک کردن", tint = TextMutedDark)
                            }
                        }
                    }
                }
            }
        }

        // AI Results
        when (aiResult) {
            is AiAnalysisResult.Loading -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFB388FF))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "در حال پردازش داده‌های تارگت‌ها و استخراج الگوها...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "بررسی تغییرات روزانه، تقابل‌های دوطرفه و تناقض‌های کلامی",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            is AiAnalysisResult.Success -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_result_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "نتایج گزارش تحلیلی:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MafiaGold
                                    )
                                    if (aiResult.providerLabel.isNotBlank()) {
                                        Text(
                                            text = "ارائه‌دهنده: ${aiResult.providerLabel}",
                                            fontSize = 11.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("تحلیل هوش مصنوعی مافیا", aiResult.markdownContent)
                                        clipboard.setPrimaryClip(clip)
                                        copiedToClipboard = true
                                    },
                                    modifier = Modifier.testTag("btn_copy_ai_report")
                                ) {
                                    Icon(
                                        imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "کپی متن گزارش",
                                        tint = if (copiedToClipboard) SuspicionGreen else TextSecondaryDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = aiResult.markdownContent,
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }
            }

            is AiAnalysisResult.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCrimsonDark.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaCrimson)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MafiaCrimsonLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "وضعیت اتصال:",
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaCrimsonLight,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = aiResult.message,
                                color = TextPrimaryDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (aiResult.fallbackAnalysis != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                            shape = RoundedCornerShape(14.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📊 تحلیل آفلاین هوشمند محلی:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MafiaGold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = aiResult.fallbackAnalysis,
                                    fontSize = 14.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }
                }
            }

            null -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 ویژگی‌های تحلیل هوش مصنوعی:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• دسته‌بندی شواهد در جهت اتهام و تبرئه به تفکیک هر بازیکن\n" +
                                        "• کشف خطوط رأی‌گیری مشکوک و تغییر مسیر تارگت‌ها\n" +
                                        "• طرح سوالات کلیدی استراتژیک برای روزهای بعد\n" +
                                        "• بدون ادعای قطعیت؛ صرفاً تحلیل شواهد عینی ثبت‌شده",
                                fontSize = 13.sp,
                                color = TextSecondaryDark,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
