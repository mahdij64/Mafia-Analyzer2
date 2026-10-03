package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.domain.speech.SpeechCoachEngine
import com.example.domain.speech.SpeechGuide
import com.example.ui.components.StageSelectorTabs
import com.example.ui.theme.*

@Composable
fun SpeechCoachScreen(
    currentStageIndex: Int,
    activeGame: GameEntity?,
    players: List<PlayerEntity>,
    scores: List<PlayerScoreAnalysis>,
    targets: List<TargetEntity>,
    notes: List<PlayerNoteEntity>,
    speechGuide: SpeechGuide?,
    onStageSelected: (Int) -> Unit,
    onUpdateMafiaTeammates: (Set<Long>) -> Unit,
    onUpdateGameScenarioAndOwner: (citizenCount: Int, mafiaCount: Int, independentCount: Int, ownerPlayerId: Long?, ownerRole: String) -> Unit,
    onTogglePlayerEliminated: (PlayerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showCopiedToast by remember { mutableStateOf(false) }

    val ownerPlayer = remember(players, activeGame) {
        players.firstOrNull { it.isOwner || (activeGame?.ownerPlayerId != null && it.id == activeGame.ownerPlayerId) }
    }
    val ownerRole = activeGame?.ownerRole ?: ownerPlayer?.knownRole ?: "CITIZEN"

    // Mafia teammates (all players with knownRole == "MAFIA" except owner)
    val currentTeammateIds = remember(players, ownerPlayer) {
        players.filter { it.knownRole == "MAFIA" && it.id != ownerPlayer?.id }.map { it.id }.toSet()
    }

    val guide = remember(currentStageIndex, activeGame, players, scores, targets, notes) {
        speechGuide ?: SpeechCoachEngine.generateGuide(
            currentStageIndex = currentStageIndex,
            activeGame = activeGame,
            players = players,
            scores = scores,
            allTargets = targets,
            allNotes = notes
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Stage Selector
        item {
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                onStageSelected = onStageSelected
            )
        }

        // Owner & Role Banner Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth().testTag("speech_identity_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "شما : ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            val ownerName = ownerPlayer?.name ?: "تعیین نشده"
                            val ownerColor = when (ownerRole) {
                                "MAFIA" -> Color(0xFF9C27B0)
                                "INDEPENDENT" -> Color(0xFF2196F3)
                                else -> Color(0xFF4CAF50)
                            }
                            Text(
                                text = ownerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ownerColor
                            )
                        }

                    }
                }
            }
        }

        // (Mafia teammates selection moved to Settings tab)

        // FULL COLLOQUIAL SPEECH SCRIPT (خروجی نهایی: متن کامل نطق به زبان عامیانه)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1626)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MafiaGold),
                modifier = Modifier.fillMaxWidth().testTag("speech_script_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🗣️ متن نطق شما (زبان عامیانه و کوچه بازاری):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaGold
                            )
                        }

                        // Copy Speech Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("نطق مافیا", guide.fullColloquialSpeechText)
                                clipboard.setPrimaryClip(clip)
                                showCopiedToast = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کپی نطق", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showCopiedToast) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✅ متن نطق کپی شد!",
                            fontSize = 11.sp,
                            color = MafiaGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MafiaDarkBg.copy(alpha = 0.9f),
                        border = BorderStroke(0.5.dp, MafiaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = guide.fullColloquialSpeechText,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ این نطق بر اساس تارگت‌های ثبت‌شده، یادداشت‌های دست‌نویس و نقش شما (${ownerRoleLabel(ownerRole)}) بازنویسی شده است.",
                        fontSize = 10.sp,
                        color = TextMutedDark
                    )
                }
            }
        }

        // STEP 1: اول چی بگم؟ (Opening Hook)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MafiaCrimsonLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎯 اول چی بگم؟ (شروع صحبت):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaCrimsonLight
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MafiaSurfaceVariant,
                        border = BorderStroke(0.5.dp, MafiaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = guide.openingHook,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // STEP 2: تحلیل یادداشت‌های ثبت‌شده شما (بخش بسیار مهم)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📝 برگ‌های برنده از یادداشت‌های شما:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    guide.userNotesInsights.forEach { insight ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MafiaSurfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MafiaBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = insight,
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: افراد زیر ذره‌بین (مشکوک‌ها) و تارگت‌های اصلی
        if (guide.topSuspectsAdvice.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MafiaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MafiaCrimson, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎯 افراد زیر ذره‌بین و شیوه صحبت درباره آن‌ها:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaCrimson
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        guide.topSuspectsAdvice.forEach { tip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (tip.isTeammate) Color(0xFF261217) else MafiaSurfaceVariant,
                                border = BorderStroke(1.dp, if (tip.isTeammate) MafiaCrimsonDark else MafiaBorder),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${tip.displayName}" + if (tip.isTeammate) " (🗡️ هم‌تیمی شما)" else "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tip.isTeammate) MafiaCrimsonLight else TextPrimaryDark
                                        )
                                        Text(
                                            text = "سوءظن: ${tip.suspicionScore}٪",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tip.suspicionScore >= 66) MafiaCrimsonLight else MafiaGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = tip.colloquialArgument,
                                        fontSize = 11.sp,
                                        lineHeight = 17.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // STEP 4: تحلیل الگوی تارگت‌ها و چرخش‌ها
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔄 الگوی تارگت‌ها و نوسان نظرات:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = guide.targetPatternAnalysis,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = TextPrimaryDark
                    )
                }
            }
        }

        // STEP 5: استراتژی‌های طلایی ویژه نقش شما
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val roleLabel = when (ownerRole) {
                        "MAFIA" -> "مافیا 🗡️"
                        "INDEPENDENT" -> "مستقل 🎭"
                        else -> "شهروند 🛡️"
                    }
                    Text(
                        text = "⚡ توصیه‌های کلیدی متناسب با نقش شما:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    guide.roleSpecificStrategy.forEach { tip ->
                        Text(
                            text = tip,
                            fontSize = 11.sp,
                            lineHeight = 18.sp,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun ownerRoleLabel(role: String): String = when (role) {
    "MAFIA" -> "مافیا 🗡️"
    "INDEPENDENT" -> "مستقل 🎭"
    else -> "شهروند 🛡️"
}
