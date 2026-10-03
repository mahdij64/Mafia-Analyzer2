package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.model.GameStage
import com.example.domain.algorithm.FactorItem
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.ui.components.StageSelectorTabs
import com.example.ui.components.StatusChip
import com.example.ui.components.SuspicionProgressBar
import com.example.ui.components.SuspicionScoreBadge
import com.example.ui.theme.*

@Composable
fun DailyReportScreen(
    currentStageIndex: Int,
    scores: List<PlayerScoreAnalysis>,
    onStageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, HIGH (>=65), MEDIUM (45..64), LOW (<45)
    var expandedPlayerId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    val stage = GameStage.getStage(currentStageIndex)

    val filteredScores = remember(scores, searchQuery, selectedFilter) {
        scores
            .filter {
                if (searchQuery.isBlank()) true
                else it.displayName.contains(searchQuery.trim(), ignoreCase = true) ||
                     it.playerName.contains(searchQuery.trim(), ignoreCase = true)
            }
            .filter {
                when (selectedFilter) {
                    "HIGH" -> it.totalScore >= 66
                    "MEDIUM" -> it.totalScore in 46..65
                    "LOW" -> it.totalScore <= 45
                    else -> true
                }
            }
            .sortedByDescending { it.totalScore }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // Stage Selector
        item {
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                onStageSelected = onStageSelected
            )
        }

        // Header Title
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "گزارش سوءظن مرحله ${stage.title}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "رده‌بندی شفاف بر مبنای تارگت‌ها، نوسان، آراء و مشاهدات",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Button(
                            onClick = {
                                val sb = StringBuilder()
                                sb.appendLine("🕵️ گزارش روزانه تحلیل‌گر مافیا")
                                sb.appendLine("مرحله: ${stage.title}")
                                sb.appendLine("────────────────────")
                                sb.appendLine("📊 رده‌بندی سوءظن بازیکنان:")
                                scores.sortedByDescending { it.totalScore }.forEachIndexed { idx, s ->
                                    sb.appendLine("${idx + 1}. ${s.displayName}: ${s.totalScore}/100 (${s.statusLabel})")
                                    if (s.positiveFactors.isNotEmpty()) {
                                        sb.appendLine("   ⚠️ فاکتورها: " + s.positiveFactors.joinToString("، ") { it.title })
                                    }
                                }
                                sb.appendLine("────────────────────")
                                sb.appendLine("اپلیکیشن تحلیل‌گر مافیا")

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش مرحله ${stage.title}"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_share_daily_report")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اشتراک‌گذاری", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجوی نام بازیکن...", color = TextMutedDark, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMutedDark)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "پاک کردن", tint = TextMutedDark)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MafiaDarkBg,
                            unfocusedContainerColor = MafiaDarkBg,
                            focusedBorderColor = MafiaCrimson,
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterPill(
                            label = "همه (${scores.size})",
                            isSelected = selectedFilter == "ALL",
                            color = MafiaGold,
                            onClick = { selectedFilter = "ALL" },
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterPill(
                            label = "پرریسک (≥۶۶)",
                            isSelected = selectedFilter == "HIGH",
                            color = SuspicionRed,
                            onClick = { selectedFilter = "HIGH" },
                            modifier = Modifier.testTag("filter_high")
                        )
                        FilterPill(
                            label = "متوسط (۴۶-۶۵)",
                            isSelected = selectedFilter == "MEDIUM",
                            color = SuspicionYellow,
                            onClick = { selectedFilter = "MEDIUM" },
                            modifier = Modifier.testTag("filter_medium")
                        )
                        FilterPill(
                            label = "کم‌خطر (≤۴۵)",
                            isSelected = selectedFilter == "LOW",
                            color = SuspicionGreen,
                            onClick = { selectedFilter = "LOW" },
                            modifier = Modifier.testTag("filter_low")
                        )
                    }
                }
            }
        }

        // Statistics Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaGold)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📊 آمار کلی بازی",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (scores.isEmpty()) {
                        Text("هنوز داده‌ای ثبت نشده", fontSize = 12.sp, color = TextMutedDark)
                    } else {
                        // Most Targeted
                        val mostTargeted = scores.maxByOrNull { it.targetsReceivedCount }
                        if (mostTargeted != null && mostTargeted.targetsReceivedCount > 0) {
                            StatRow(
                                icon = "🎯",
                                label = "بیشترین تارگت دریافتی",
                                value = "${mostTargeted.displayName} (${mostTargeted.targetsReceivedCount} تارگت)",
                                color = MafiaCrimsonLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Least Targeted
                        val leastTargeted = scores.filter { !it.isEliminated }.minByOrNull { it.targetsReceivedCount }
                        if (leastTargeted != null) {
                            StatRow(
                                icon = "🛡️",
                                label = "کمترین تارگت دریافتی",
                                value = "${leastTargeted.displayName} (${leastTargeted.targetsReceivedCount} تارگت)",
                                color = SuspicionGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Most Active (made most targets)
                        val mostActive = scores.maxByOrNull { it.totalTargetsMade }
                        if (mostActive != null && mostActive.totalTargetsMade > 0) {
                            StatRow(
                                icon = "⚡",
                                label = "فعال‌ترین بازیکن",
                                value = "${mostActive.displayName} (${mostActive.totalTargetsMade} تارگت زده)",
                                color = Color(0xFFFFA726)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Highest Suspicion
                        val highestSuspicion = scores.maxByOrNull { it.totalScore }
                        if (highestSuspicion != null) {
                            StatRow(
                                icon = "🔴",
                                label = "مشکوک‌ترین بازیکن",
                                value = "${highestSuspicion.displayName} (امتیاز: ${highestSuspicion.totalScore})",
                                color = MafiaCrimson
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Lowest Suspicion
                        val lowestSuspicion = scores.filter { !it.isEliminated }.minByOrNull { it.totalScore }
                        if (lowestSuspicion != null) {
                            StatRow(
                                icon = "✅",
                                label = "کم‌خطرترین بازیکن",
                                value = "${lowestSuspicion.displayName} (امتیاز: ${lowestSuspicion.totalScore})",
                                color = SuspicionGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Most Votes Received
                        val mostVotes = scores.maxByOrNull { it.votesReceivedCount }
                        if (mostVotes != null && mostVotes.votesReceivedCount > 0) {
                            StatRow(
                                icon = "🗳️",
                                label = "بیشترین رأی دریافتی",
                                value = "${mostVotes.displayName} (${mostVotes.votesReceivedCount} رأی)",
                                color = Color(0xFFAB47BC)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        
                        // Most Notes
                        val mostNotes = scores.maxByOrNull { it.suspiciousNotesCount + it.talkNotesCount + it.opinionNotesCount }
                        if (mostNotes != null) {
                            val totalNotes = mostNotes.suspiciousNotesCount + mostNotes.talkNotesCount + mostNotes.opinionNotesCount
                            if (totalNotes > 0) {
                                StatRow(
                                    icon = "📝",
                                    label = "بیشترین یادداشت",
                                    value = "${mostNotes.displayName} ($totalNotes یادداشت)",
                                    color = Color(0xFF42A5F5)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredScores.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هیچ موردی مطابق فیلتر یافت نشد.",
                        fontSize = 13.sp,
                        color = TextMutedDark,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredScores, key = { it.playerId }) { analysis ->
                val isExpanded = expandedPlayerId == analysis.playerId
                val rank = scores.sortedByDescending { it.totalScore }.indexOfFirst { it.playerId == analysis.playerId } + 1
                DailyReportPlayerCard(
                    rank = rank,
                    analysis = analysis,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedPlayerId = if (isExpanded) null else analysis.playerId
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MafiaSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else Color.Transparent
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else TextSecondaryDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DailyReportPlayerCard(
    rank: Int,
    analysis: PlayerScoreAnalysis,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (analysis.totalScore >= 70) MafiaCrimson.copy(alpha = 0.5f) else MafiaBorder
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_card_${analysis.playerId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main clickable header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (rank <= 3 && analysis.totalScore >= 60) MafiaCrimsonDark else MafiaSurfaceVariant
                    ) {
                        Text(
                            text = "$rank",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
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
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusChip(label = analysis.statusLabel, score = analysis.totalScore)
                        }

                        Text(
                            text = "نظر شخصی: ${analysis.manualScore}/۱۰۰ | تارگت‌ها: ${analysis.totalTargetsMade}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuspicionScoreBadge(score = analysis.totalScore)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SuspicionProgressBar(score = analysis.totalScore)

            // Expanded Reasons Breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MafiaBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🔍 شفافیت فرمول و دلایل امتیاز (${analysis.totalScore}/۱۰۰):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MafiaGold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Positive factors (increase suspicion)
                    if (analysis.positiveFactors.isNotEmpty()) {
                        Text(
                            text = "⚠️ شواهد در جهت افزایش سوءظن:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MafiaCrimsonLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        analysis.positiveFactors.forEach { factor ->
                            FactorRow(factor = factor)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Negative factors (decrease suspicion)
                    if (analysis.negativeFactors.isNotEmpty()) {
                        Text(
                            text = "🛡️ شواهد در جهت کاهش سوءظن (شهروندی):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SuspicionGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        analysis.negativeFactors.forEach { factor ->
                            FactorRow(factor = factor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorRow(factor: FactorItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "• ${factor.title}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryDark
            )
            Text(
                text = factor.description,
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (factor.isSuspicious) MafiaCrimson.copy(alpha = 0.15f) else SuspicionGreen.copy(alpha = 0.15f)
        ) {
            Text(
                text = if (factor.points > 0) "+${factor.points}" else "${factor.points}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (factor.isSuspicious) MafiaCrimsonLight else SuspicionGreen,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.45f)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
