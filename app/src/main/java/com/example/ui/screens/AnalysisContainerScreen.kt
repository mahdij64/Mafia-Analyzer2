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
import com.example.data.local.AiSettingsEntity
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
    aiSettings: AiSettingsEntity? = null,
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
        Pair("چی بگم؟ (راهنمای نطق) 🗣️", Icons.Default.RecordVoiceOver),
        Pair("گزارش روزانه", Icons.Default.Assessment),
        Pair("نقشه روابط", Icons.Default.Hub),
        Pair("روند و فینال", Icons.AutoMirrored.Filled.TrendingUp),
        Pair("هوش مصنوعی", Icons.Default.AutoAwesome)
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
                    onExportReport = onExportReport
                )
                4 -> AiAnalysisScreen(
                    activeGame = activeGame,
                    currentStageIndex = currentStageIndex,
                    aiResult = aiResult,
                    aiSettings = aiSettings,
                    onRunAnalysis = onRunAiAnalysis,
                    onClearAnalysis = onClearAiAnalysis
                )
            }
        }
    }
}
