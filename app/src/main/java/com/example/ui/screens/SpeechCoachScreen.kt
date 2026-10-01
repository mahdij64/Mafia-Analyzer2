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
    var showScenarioOwnerDialog by remember { mutableStateOf(false) }
    var showEliminatedDialog by remember { mutableStateOf(false) }

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
                                text = "👑 هویت شما:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val roleBadgeColor = when (ownerRole) {
                                "MAFIA" -> MafiaCrimsonDark
                                "INDEPENDENT" -> Color(0xFFE65100)
                                else -> Color(0xFF0D47A1)
                            }
                            val roleLabel = when (ownerRole) {
                                "MAFIA" -> "مافیا 🗡️"
                                "INDEPENDENT" -> "مستقل 🎭"
                                else -> "شهروند 🛡️"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = roleBadgeColor,
                                border = BorderStroke(0.5.dp, MafiaGold)
                            ) {
                                Text(
                                    text = "${ownerPlayer?.name ?: "تعیین نشده"} ($roleLabel)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Edit Role Button
                        OutlinedButton(
                            onClick = { showScenarioOwnerDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغییر هویت / سناریو", fontSize = 10.sp, color = MafiaGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick status summary
                    val livingCount = players.count { !it.isEliminated }
                    val deadCount = players.count { it.isEliminated }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🟢 $livingCount بازیکن زنده | 💀 $deadCount بازیکن حذف‌شده",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )

                        TextButton(
                            onClick = { showEliminatedDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "مدیریت کشته‌ها 💀",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaCrimsonLight
                            )
                        }
                    }
                }
            }
        }

        // MAFIA TEAMMATES SELECTION SECTION (CRITICAL REQUIREMENT)
        if (ownerRole == "MAFIA") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261217)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MafiaCrimson),
                    modifier = Modifier.fillMaxWidth().testTag("mafia_teammates_section")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🗡️ همکاران و یاران مافیای شما:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "چون شما مافیا هستید، یاران خود را از لیست زیر تیک بزنید تا سیستم در تنظیم نطق، پوشش‌ها و تارگت‌های فیک دقیق‌تر شما را راهنمایی کند:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val availableOtherPlayers = players.filter { it.id != ownerPlayer?.id && !it.isEliminated }
                        if (availableOtherPlayers.isEmpty()) {
                            Text("بازیکن دیگری در بازی وجود ندارد.", fontSize = 11.sp, color = TextMutedDark)
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableOtherPlayers, key = { it.id }) { p ->
                                    val isTeammate = currentTeammateIds.contains(p.id)
                                    val seat = players.indexOfFirst { it.id == p.id } + 1
                                    FilterChip(
                                        selected = isTeammate,
                                        onClick = {
                                            val newSet = currentTeammateIds.toMutableSet()
                                            if (isTeammate) {
                                                newSet.remove(p.id)
                                            } else {
                                                newSet.add(p.id)
                                            }
                                            onUpdateMafiaTeammates(newSet)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        label = {
                                            Text(
                                                text = if (isTeammate) "🗡️ $seat. ${p.name}" else "$seat. ${p.name}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isTeammate) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MafiaCrimson,
                                            selectedLabelColor = Color.White,
                                            containerColor = MafiaSurfaceVariant,
                                            labelColor = TextPrimaryDark
                                        )
                                    )
                                }
                            }
                        }

                        if (guide.mafiaTeammatesInfo != null && guide.mafiaTeammatesInfo.teammatesNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MafiaCardBg.copy(alpha = 0.6f),
                                border = BorderStroke(0.5.dp, MafiaCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 تاکتیک پوشش یاران: ${guide.mafiaTeammatesInfo.tacticalCoveringAdvice}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFCDD2),
                                    modifier = Modifier.padding(8.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

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
                        text = "⚡ این نطق بر اساس تارگت‌های ثبت‌شده، یادداشت‌های دست‌نویس و نقش شما (${guide.ownerRole}) بازنویسی شده است.",
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
                                            text = "${tip.playerName}" + if (tip.isTeammate) " (🗡️ هم‌تیمی شما)" else "",
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
                        text = "⚡ توصیه‌های کلیدی متناسب با نقش شما ($roleLabel):",
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

    // Dialog for Scenario & Owner Identity Setup
    if (showScenarioOwnerDialog) {
        var editCitizenCount by remember { mutableIntStateOf(activeGame?.citizenCount ?: 4) }
        var editMafiaCount by remember { mutableIntStateOf(activeGame?.mafiaCount ?: 3) }
        var editIndependentCount by remember { mutableIntStateOf(activeGame?.independentCount ?: 1) }
        var editOwnerPlayerId by remember { mutableStateOf<Long?>(ownerPlayer?.id) }
        var editOwnerRole by remember { mutableStateOf(ownerRole) }

        AlertDialog(
            onDismissRequest = { showScenarioOwnerDialog = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "👑 تنظیم سناریو و هویت شما",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MafiaGold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    Text(
                        text = "ترکیب سناریوی بازی و هویت خود را مشخص کنید تا سیستم دقیق‌ترین نطق و تحلیل را آماده کند:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Scenario counters
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MafiaSurfaceVariant,
                        border = BorderStroke(1.dp, MafiaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("⚙️ تعداد نقش‌ها در سناریو:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MafiaGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Citizen
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🛡️ شهروند", fontSize = 10.sp, color = TextSecondaryDark)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (editCitizenCount > 0) editCitizenCount-- }, modifier = Modifier.size(24.dp)) {
                                            Text("-", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$editCitizenCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        IconButton(onClick = { editCitizenCount++ }, modifier = Modifier.size(24.dp)) {
                                            Text("+", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // Mafia
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🗡️ مافیا", fontSize = 10.sp, color = TextSecondaryDark)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (editMafiaCount > 0) editMafiaCount-- }, modifier = Modifier.size(24.dp)) {
                                            Text("-", fontSize = 16.sp, color = MafiaCrimson, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$editMafiaCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        IconButton(onClick = { editMafiaCount++ }, modifier = Modifier.size(24.dp)) {
                                            Text("+", fontSize = 16.sp, color = MafiaCrimson, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // Independent
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎭 مستقل", fontSize = 10.sp, color = TextSecondaryDark)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (editIndependentCount > 0) editIndependentCount-- }, modifier = Modifier.size(24.dp)) {
                                            Text("-", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$editIndependentCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        IconButton(onClick = { editIndependentCount++ }, modifier = Modifier.size(24.dp)) {
                                            Text("+", fontSize = 16.sp, color = MafiaGold, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Owner Player Selection
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF231626),
                        border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("👑 شما کدام بازیکن هستید؟", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MafiaGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(players, key = { it.id }) { p ->
                                    val isMe = editOwnerPlayerId == p.id
                                    val seat = players.indexOfFirst { it.id == p.id } + 1
                                    Surface(
                                        onClick = { editOwnerPlayerId = p.id },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isMe) MafiaGold else MafiaSurfaceVariant,
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "$seat. ${p.name}" + if (isMe) " (👑 من)" else "",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.Black else TextPrimaryDark
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("نقش شما در بازی چیست؟", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MafiaGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val roleOptions = listOf(
                                    Triple("CITIZEN", "🛡️ شهروند", Color(0xFF1976D2)),
                                    Triple("MAFIA", "🗡️ مافیا", MafiaCrimson),
                                    Triple("INDEPENDENT", "🎭 مستقل", Color(0xFFFFA000))
                                )
                                roleOptions.forEach { (rKey, rLabel, rColor) ->
                                    val isSel = editOwnerRole == rKey
                                    Surface(
                                        onClick = { editOwnerRole = rKey },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) rColor else MafiaSurfaceVariant,
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = rLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateGameScenarioAndOwner(
                            editCitizenCount,
                            editMafiaCount,
                            editIndependentCount,
                            editOwnerPlayerId,
                            editOwnerRole
                        )
                        showScenarioOwnerDialog = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaGold)
                ) {
                    Text("ذخیره تنظیمات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScenarioOwnerDialog = false }) {
                    Text("انصراف", color = TextSecondaryDark)
                }
            }
        )
    }

    // Dialog for Managing Eliminated / Killed Players
    if (showEliminatedDialog) {
        AlertDialog(
            onDismissRequest = { showEliminatedDialog = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "💀 مدیریت بازیکنان حذف‌شده (کشته‌های بازی)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MafiaCrimsonLight
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    Text(
                        text = "بازیکنانی که در شب یا روز حذف شده‌اند را علامت بزنید تا الگوریتم و نطق بازی به‌روز شوند:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(players, key = { it.id }) { p ->
                            val seat = players.indexOfFirst { it.id == p.id } + 1
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (p.isEliminated) Color(0xFF26181B) else MafiaSurfaceVariant,
                                border = BorderStroke(1.dp, if (p.isEliminated) MafiaCrimsonDark else MafiaBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = if (p.isEliminated) "💀" else "🟢", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "صندلی $seat: ${p.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (p.isEliminated) TextMutedDark else TextPrimaryDark
                                            )
                                            Text(
                                                text = if (p.isEliminated) "حذف‌شده از بازی" else "در حال بازی",
                                                fontSize = 10.sp,
                                                color = if (p.isEliminated) MafiaCrimsonLight else MafiaGreen
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onTogglePlayerEliminated(p)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (p.isEliminated) MafiaGreen else MafiaCrimson
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (p.isEliminated) "بازگشت 🔄" else "کشته شد 💀",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEliminatedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaGold)
                ) {
                    Text("بستن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
