package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.model.GameStage
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.ui.components.StatusChip
import com.example.ui.components.SuspicionScoreBadge
import com.example.ui.components.SuspicionTrendChart
import com.example.ui.theme.*

@Composable
fun FinalReportScreen(
    activeGame: GameEntity?,
    players: List<PlayerEntity>,
    scores: List<PlayerScoreAnalysis>,
    targets: List<TargetEntity>,
    notes: List<PlayerNoteEntity>,
    currentStageIndex: Int = 0,
    onExportReport: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPlayerId by remember(scores) {
        mutableStateOf(scores.maxByOrNull { it.totalScore }?.playerId ?: players.firstOrNull()?.id ?: 0L)
    }

    val selectedScore = scores.firstOrNull { it.playerId == selectedPlayerId }
    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }
    val playerMap = players.associateBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // Final Report Header
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
                        Column {
                            Text(
                                text = "🏆 گزارش تحلیلی نهایی بازی",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaGold
                            )
                            Text(
                                text = activeGame?.name ?: "بازی مافیا",
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Button(
                            onClick = {
                                val reportText = onExportReport()
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش مافیا")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_export_report")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اشتراک‌گذاری", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "انتخاب بازیکن برای بررسی جامع روند و آمار:",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(scores.sortedByDescending { it.totalScore }, key = { it.playerId }) { s ->
                            val isSelected = s.playerId == selectedPlayerId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlayerId = s.playerId },
                                label = {
                                    Text(
                                        text = "${s.displayName} (${s.totalScore})",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MafiaCrimsonDark,
                                    selectedLabelColor = TextPrimaryDark,
                                    containerColor = MafiaSurfaceVariant,
                                    labelColor = TextSecondaryDark
                                ),
                                modifier = Modifier.testTag("final_player_chip_${s.playerId}")
                            )
                        }
                    }
                }
            }
        }

        if (selectedScore != null && selectedPlayer != null) {
            // Visual Trend Chart Item
            item {
                SuspicionTrendChart(
                    stageScores = selectedScore.stageTrendScores,
                    playerName = selectedScore.displayName,
                    modifier = Modifier.testTag("trend_chart_${selectedScore.playerId}")
                )
            }

            // Statistics Grid Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 آمار و شاخص‌های ثبت‌شده برای «${selectedScore.displayName}»:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatCard(
                                title = "تارگت‌های زده‌شده",
                                value = "${selectedScore.totalTargetsMade}",
                                subtitle = "به ${selectedScore.uniqueTargetsCount} بازیکن",
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCard(
                                title = "تارگت‌های دریافتی",
                                value = "${selectedScore.targetsReceivedCount}",
                                subtitle = "از سایرین",
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCard(
                                title = "نوسان و چرخش",
                                value = "${selectedScore.targetFlipsCount}",
                                subtitle = "تغییر موضع",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatCard(
                                title = "آرای خروج داده‌شده",
                                value = "${selectedScore.votesCastCount}",
                                subtitle = "در رأی‌گیری",
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCard(
                                title = "آرای خروج دریافتی",
                                value = "${selectedScore.votesReceivedCount}",
                                subtitle = "علیه او",
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatCard(
                                title = "رفتار مشکوک",
                                value = "${selectedScore.suspiciousNotesCount}",
                                subtitle = "مورد ثبت‌شده",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Target Breakdown Across Days
            val myTargets = targets.filter { it.sourcePlayerId == selectedPlayer.id }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 تاریخچه تارگت‌ها به تفکیک روز:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        (0..maxOf(currentStageIndex, 4)).forEach { stg ->
                            val stgTargets = myTargets.filter { it.stageIndex == stg }
                            val stgTitle = GameStage.getStage(stg).title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$stgTitle:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaGold,
                                    modifier = Modifier.width(65.dp)
                                )
                                if (stgTargets.isEmpty()) {
                                    Text(text = "تارگتی ثبت نشده", fontSize = 13.sp, color = TextMutedDark)
                                } else {
                                    val names = stgTargets.mapNotNull { playerMap[it.targetPlayerId]?.name }.joinToString("، ")
                                    Text(text = "→ $names", fontSize = 13.sp, color = TextPrimaryDark)
                                }
                            }
                        }
                    }
                }
            }

            // Key Notes & Observations
            val playerNotes = notes.filter { it.playerId == selectedPlayer.id }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📝 یادداشت‌ها و مشاهدات کلیدی (${playerNotes.size} مورد):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (playerNotes.isEmpty()) {
                            Text(
                                text = "یادداشتی ثبت نشده است.",
                                fontSize = 13.sp,
                                color = TextMutedDark
                            )
                        } else {
                            playerNotes.forEach { note ->
                                val catTitle = when (note.category) {
                                    "TALK" -> "🗣️ صحبت‌ها"
                                    "SUSPICIOUS" -> "⚠️ رفتار مشکوک"
                                    "OPINION" -> "💡 نظر شخصی"
                                    "speaker_targeting" -> "🎯 تارگت‌زنی"
                                    else -> "📝 یادداشت آزاد"
                                }
                                val stgTitle = GameStage.getStage(note.stageIndex).title
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "[$stgTitle - $catTitle]:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MafiaGold
                                    )
                                    Text(
                                        text = note.text,
                                        fontSize = 13.sp,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MafiaSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            Text(text = title, fontSize = 10.sp, color = TextSecondaryDark, maxLines = 1)
            Text(text = subtitle, fontSize = 9.sp, color = TextMutedDark, maxLines = 1)
        }
    }
}
