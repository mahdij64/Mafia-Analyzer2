package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiAnalysisResult
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.TargetEntity
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.domain.speech.SpeechGuide
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisContainerScreen(
    currentStageIndex: Int,
    activeGame: GameEntity?,
    players: List<PlayerEntity>,
    scores: List<PlayerScoreAnalysis>,
    targets: List<TargetEntity>,
    notes: List<PlayerNoteEntity>,
    speechGuide: SpeechGuide?,
    aiResult: AiAnalysisResult?,
    initialSubTab: Int = 0,
    onStageSelected: (Int) -> Unit,
    onUpdateMafiaTeammates: (Set<Long>) -> Unit = {},
    onUpdateGameScenarioAndOwner: (citizenCount: Int, mafiaCount: Int, independentCount: Int, ownerPlayerId: Long?, ownerRole: String) -> Unit = { _, _, _, _, _ -> },
    onTogglePlayerEliminated: (PlayerEntity) -> Unit = {},
    onRunAiAnalysis: () -> Unit,
    onClearAiAnalysis: () -> Unit,
    onExportReport: () -> String,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }

    val subTabs = listOf(
        Pair("چی بگم؟ 🗣️", Icons.Default.RecordVoiceOver),
        Pair("گزارش روزانه", Icons.Default.Assessment),
        Pair("نقشه روابط", Icons.Default.Hub),
        Pair("روند و فینال", Icons.AutoMirrored.Filled.TrendingUp),
        Pair("چت AI 🤖", Icons.Default.SmartToy)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
    ) {
        // Sub-tabs row
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MafiaCardBg,
            contentColor = MafiaCrimson,
            divider = { HorizontalDivider(color = MafiaBorder, thickness = 1.dp) },
            modifier = Modifier.fillMaxWidth().testTag("analysis_sub_tabs")
        ) {
            subTabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedSubTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MafiaGold else TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MafiaGold else TextMutedDark,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    modifier = Modifier.testTag("analysis_tab_$index")
                )
            }
        }

        // Selected Sub Screen
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedSubTab) {
                0 -> SpeechCoachScreen(
                    currentStageIndex = currentStageIndex,
                    activeGame = activeGame,
                    players = players,
                    scores = scores,
                    targets = targets,
                    notes = notes,
                    speechGuide = speechGuide,
                    onStageSelected = onStageSelected,
                    onUpdateMafiaTeammates = onUpdateMafiaTeammates,
                    onUpdateGameScenarioAndOwner = onUpdateGameScenarioAndOwner,
                    onTogglePlayerEliminated = onTogglePlayerEliminated
                )
                1 -> DailyReportScreen(
                    currentStageIndex = currentStageIndex,
                    scores = scores,
                    onStageSelected = onStageSelected
                )
                2 -> RelationshipMapScreen(
                    currentStageIndex = currentStageIndex,
                    players = players,
                    allTargets = targets,
                    onStageSelected = onStageSelected
                )
                3 -> FinalReportScreen(
                    activeGame = activeGame,
                    players = players,
                    scores = scores,
                    targets = targets,
                    notes = notes,
                    currentStageIndex = currentStageIndex,
                    onExportReport = onExportReport
                )
                4 -> AiChatScreen(
                    gameContextProvider = {
                        buildGameContext(activeGame, players, scores, targets, notes, currentStageIndex)
                    },
                    activeGameName = activeGame?.name
                )
            }
        }
    }
}

/**
 * Build a comprehensive game context string for AI analysis.
 */
private fun buildGameContext(
    activeGame: GameEntity?,
    players: List<PlayerEntity>,
    scores: List<PlayerScoreAnalysis>,
    targets: List<TargetEntity>,
    notes: List<PlayerNoteEntity>,
    currentStageIndex: Int
): String {
    if (activeGame == null || players.isEmpty()) return ""

    val sb = StringBuilder()
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("📋 اطلاعات بازی")
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("نام بازی: ${activeGame.name}")
    sb.appendLine("سناریو: ${activeGame.citizenCount} شهروند / ${activeGame.mafiaCount} مافیا / ${activeGame.independentCount} مستقل")
    sb.appendLine("مرحله فعلی: روز ${currentStageIndex + 1}")
    sb.appendLine("تعداد بازیکنان: ${players.size}")
    sb.append("")

    // Players
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("👥 بازیکنان")
    sb.appendLine("═══════════════════════════════════════")
    players.forEachIndexed { index, p ->
        val status = if (p.isEliminated) "💀 حذف‌شده" else "🟢 زنده"
        val ownerMark = if (p.id == activeGame.ownerPlayerId) " (👑 شما)" else ""
        val score = scores.firstOrNull { it.playerId == p.id }
        val scoreText = score?.let { " | امتیاز سوءظن: ${it.totalScore}%" } ?: ""
        sb.appendLine("${index + 1}. صندلی ${index + 1}: $status$ownerMark$scoreText")
    }
    sb.append("")

    // Targets per stage
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("🎯 تاریخچه تارگت‌ها")
    sb.appendLine("═══════════════════════════════════════")
    for (stg in 0..currentStageIndex) {
        val stgTargets = targets.filter { it.stageIndex == stg }
        if (stgTargets.isNotEmpty()) {
            sb.appendLine("روز ${stg + 1}:")
            stgTargets.groupBy { it.sourcePlayerId }.forEach { (srcId, tList) ->
                val srcSeat = players.indexOfFirst { it.id == srcId } + 1
                val tgtSeats = tList.map { t ->
                    val tgtSeat = players.indexOfFirst { it.id == t.targetPlayerId } + 1
                    "${tgtSeat}"
                }.joinToString("، ")
                sb.appendLine("  صندلی $srcSeat → $tgtSeats")
            }
        }
    }
    sb.append("")

    // Notes
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("📝 یادداشت‌ها")
    sb.appendLine("═══════════════════════════════════════")
    val stageNotes = notes.filter { it.stageIndex == currentStageIndex }
    if (stageNotes.isNotEmpty()) {
        sb.appendLine("یادداشت‌های روز ${currentStageIndex + 1}:")
        stageNotes.groupBy { it.playerId }.forEach { (pid, noteList) ->
            val seat = players.indexOfFirst { it.id == pid } + 1
            val text = noteList.joinToString(" | ") { it.text.take(50) }
            sb.appendLine("  صندلی $seat: $text")
        }
    } else {
        sb.appendLine("بدون یادداشت در این روز.")
    }
    sb.append("")

    // Suspicion scores
    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("📊 رتبه‌بندی سوءظن (بالاترین تا کمترین)")
    sb.appendLine("═══════════════════════════════════════")
    val sortedScores = scores.sortedByDescending { it.totalScore }
    sortedScores.forEachIndexed { index, s ->
        val seat = players.indexOfFirst { it.id == s.playerId } + 1
        val isAlive = players.firstOrNull { it.id == s.playerId }?.isEliminated == false
        val status = if (isAlive) "🟢" else "💀"
        sb.appendLine("${index + 1}. صندلی $seat: ${s.totalScore}% $status")
    }
    sb.append("")

    sb.appendLine("═══════════════════════════════════════")
    sb.appendLine("لطفاً بر اساس این داده‌ها تحلیل کن.")
    return sb.toString()
}
