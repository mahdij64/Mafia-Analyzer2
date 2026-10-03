package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.GameStage
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.ui.components.StageSelectorTabs
import com.example.ui.components.StatusChip
import com.example.ui.components.SuspicionScoreBadge
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    activeGame: GameEntity?,
    currentStageIndex: Int,
    players: List<PlayerEntity>,
    targetsCount: Int,
    notesCount: Int,
    votesCount: Int,
    scores: List<PlayerScoreAnalysis>,
    onStageSelected: (Int) -> Unit,
    onNavigateToTargets: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToVotes: () -> Unit,
    onNavigateToDailyReport: () -> Unit,
    onNavigateToFinalReport: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToGames: () -> Unit,
    onPlayerClick: (PlayerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = GameStage.getStage(currentStageIndex)
    val sortedScores = scores.sortedByDescending { it.totalScore }
    val topSuspects = sortedScores.take(4)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Game Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_game_header")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeGame?.name ?: "بازی مافیا",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "مرحله فعال: ${stage.title} (${stage.description})",
                                fontSize = 13.sp,
                                color = MafiaCrimsonLight,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = onNavigateToGames,
                            modifier = Modifier.testTag("btn_switch_game")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "مدیریت بازی‌ها",
                                tint = MafiaGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StageSelectorTabs(
                        currentStageIndex = currentStageIndex,
                        onStageSelected = onStageSelected
                    )
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "بازیکنان",
                    value = "${players.size}",
                    subtitle = "${players.count { !it.isEliminated }} در بازی",
                    icon = Icons.Default.Groups,
                    color = MafiaGold,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "تارگت‌ها",
                    value = "$targetsCount",
                    subtitle = "در این مرحله",
                    icon = Icons.Default.CrisisAlert,
                    color = MafiaCrimson,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "مشاهدات",
                    value = "$notesCount",
                    subtitle = "یادداشت‌ها",
                    icon = Icons.Default.EditNote,
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "آراء",
                    value = "$votesCount",
                    subtitle = "رأی خروج",
                    icon = Icons.Default.HowToVote,
                    color = SuspicionGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Access Action Grid
        item {
            Text(
                text = "دسترسی سریع و ثبت اطلاعات",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimaryDark,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionBigCard(
                        title = "ثبت تارگت‌ها",
                        subtitle = "مشخص کردن اتهامات هر بازیکن",
                        icon = Icons.Default.CrisisAlert,
                        accentColor = MafiaCrimson,
                        onClick = onNavigateToTargets,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_targets"
                    )
                    ActionBigCard(
                        title = "یادداشت‌ها و مشاهدات",
                        subtitle = "صحبت‌ها، رفتار مشکوک و نظر شخصی",
                        icon = Icons.Default.EditNote,
                        accentColor = Color(0xFF64B5F6),
                        onClick = onNavigateToNotes,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_notes"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionBigCard(
                        title = "ثبت آراء خروج",
                        subtitle = "آرای دست‌بلند کردن یا رأی‌گیری",
                        icon = Icons.Default.HowToVote,
                        accentColor = SuspicionGreen,
                        onClick = onNavigateToVotes,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_votes"
                    )
                    ActionBigCard(
                        title = "نقشه روابط و تقابل‌ها",
                        subtitle = "بررسی تقابل‌های مستقیم و ائتلاف‌ها",
                        icon = Icons.Default.Hub,
                        accentColor = MafiaGold,
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_map"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionBigCard(
                        title = "گزارش روزانه",
                        subtitle = "رده‌بندی سوءظن مرحله جاری",
                        icon = Icons.Default.Assessment,
                        accentColor = Color(0xFFFFB300),
                        onClick = onNavigateToDailyReport,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_daily_report"
                    )
                    ActionBigCard(
                        title = "تحلیل با هوش مصنوعی",
                        subtitle = "بررسی عمیق الگوها و تناقض‌ها",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = Color(0xFFB388FF),
                        onClick = onNavigateToAi,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_ai"
                    )
                }
            }
        }

        // Top Suspects Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کانون سوءظن در مرحله ${stage.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryDark
                )
                TextButton(onClick = onNavigateToDailyReport) {
                    Text(text = "مشاهده همه", color = MafiaCrimsonLight, fontSize = 13.sp)
                }
            }
        }

        if (topSuspects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنوز بازیکنی ثبت نشده یا داده‌ای وارد نشده است",
                            color = TextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(topSuspects, key = { it.playerId }) { scoreAnalysis ->
                PlayerSuspectCard(
                    analysis = scoreAnalysis,
                    onClick = {
                        val player = players.firstOrNull { it.id == scoreAnalysis.playerId }
                        if (player != null) onPlayerClick(player)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimaryDark
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionBigCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlayerSuspectCard(
    analysis: PlayerScoreAnalysis,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("suspect_card_${analysis.playerId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = analysis.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when (analysis.ownerRoleColor) {
                            "PURPLE" -> Color(0xFF9C27B0)
                            "GREEN" -> Color(0xFF4CAF50)
                            "BLUE" -> Color(0xFF2196F3)
                            else -> TextPrimaryDark
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(
                        label = analysis.statusLabel,
                        score = analysis.totalScore
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val reasonPreview = if (analysis.positiveFactors.isNotEmpty()) {
                    analysis.positiveFactors.first().title
                } else if (analysis.negativeFactors.isNotEmpty()) {
                    analysis.negativeFactors.first().title
                } else {
                    "بدون عامل خاص"
                }

                Text(
                    text = "دلیل عمده: $reasonPreview",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            SuspicionScoreBadge(score = analysis.totalScore)
        }
    }
}
