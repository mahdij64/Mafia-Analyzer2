package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PlayerEntity
import com.example.data.model.GameStage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MafiaViewModel

enum class MainDestination(val title: String, val icon: ImageVector) {
    TARGETS("میز بازی", Icons.Default.CrisisAlert),
    ANALYSIS("گزارش‌ها و تحلیل", Icons.Default.QueryStats),
    SETTINGS("تنظیمات", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MafiaViewModel) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var currentDestination by remember { mutableStateOf(MainDestination.TARGETS) }
        var analysisInitialSubTab by remember { mutableStateOf(0) }

        val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
        val allGames by viewModel.allGames.collectAsStateWithLifecycle()
        val currentStageIndex by viewModel.currentStageIndex.collectAsStateWithLifecycle()
        val players by viewModel.players.collectAsStateWithLifecycle()
        val targets by viewModel.targets.collectAsStateWithLifecycle()
        val notes by viewModel.notes.collectAsStateWithLifecycle()
        val votes by viewModel.votes.collectAsStateWithLifecycle()
        val manualSuspicions by viewModel.manualSuspicions.collectAsStateWithLifecycle()
        val algorithmWeights by viewModel.algorithmWeights.collectAsStateWithLifecycle()
        val scores by viewModel.calculatedScores.collectAsStateWithLifecycle()
        val speechGuide by viewModel.speechGuide.collectAsStateWithLifecycle()
        val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
        val maxUnlockedStageIndex by viewModel.maxUnlockedStageIndex.collectAsStateWithLifecycle()
        val isStealthMafiaMode by viewModel.isStealthMafiaMode.collectAsStateWithLifecycle()
        val secretNotes by viewModel.secretNotes.collectAsStateWithLifecycle()
        val stealthAnalysis by viewModel.stealthAnalysis.collectAsStateWithLifecycle()
        val haptic = LocalHapticFeedback.current
        val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarMsg) {
            snackbarMsg?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearSnackbar()
            }
        }

        val stage = GameStage.getStage(currentStageIndex)
        val isTableScreen = currentDestination == MainDestination.TARGETS
        // Bottom navigation is always shown now (no edge-swipe hide).
        val shouldShowMenus = true

        // Speaker-targeting flow: when set, full-screen SpeakerTargetScreen
        // takes over the UI until the user saves-and-advances through all
        // active players or goes back.
        var speakerFlowQueue by remember { mutableStateOf<List<PlayerEntity>>(emptyList()) }
        var speakerFlowNoteText by remember { mutableStateOf("") }
        var speakerFlowNoteTags by remember { mutableStateOf<Set<String>>(emptySet()) }
        // Persist target-selection across speaker changes so re-opening a
        // previous speaker shows the same red circles as before.
        val speakerSelections = remember { mutableMapOf<Long, Set<Long>>() }
        val speakerNoteTexts = remember { mutableMapOf<Long, String>() }
        val speakerNoteTagsMap = remember { mutableMapOf<Long, Set<String>>() }

        // Full-screen override when a speaker is being targeted.
        val activeSpeaker = speakerFlowQueue.firstOrNull()
        if (activeSpeaker != null) {
            SpeakerTargetScreen(
                speaker = activeSpeaker,
                allPlayers = players,
                initialSelectedTargetIds = speakerSelections[activeSpeaker.id] ?: emptySet(),
                initialNoteText = speakerNoteTexts[activeSpeaker.id] ?: "",
                initialNoteTags = speakerNoteTagsMap[activeSpeaker.id] ?: emptySet(),
                alreadyTargetedIds = emptySet(),
                initialSelectedTargetIds = emptySet(),
                initialNoteText = "",
                initialNoteTags = emptySet(),
                isLastPlayer = speakerFlowQueue.size == 1,
                onBack = { speakerFlowQueue = emptyList() },
                onSaveAndNext = { targetIds, noteText, noteTags ->
                    viewModel.saveTargets(activeSpeaker.id, targetIds, true)
                    val noteParts = buildList {
                        if (noteText.isNotBlank()) add(noteText)
                        addAll(noteTags)
                    }
                    if (noteParts.isNotEmpty()) {
                        viewModel.addNote(
                            playerId = activeSpeaker.id,
                            category = "speaker_targeting",
                            text = noteParts.joinToString(" • ")
                        )
                    }
                    // Persist this speaker's selections so the red circles
                    // come back if the user re-opens this speaker.
                    speakerSelections[activeSpeaker.id] = targetIds.toSet()
                    speakerNoteTexts[activeSpeaker.id] = noteText
                    speakerNoteTagsMap[activeSpeaker.id] = noteTags.toSet()
                    // Pop the current speaker, continue with the rest.
                    speakerFlowQueue = speakerFlowQueue.drop(1)
                },
                onSelectionChange = { ids ->
                    speakerSelections[activeSpeaker.id] = ids
                }
            )
            return@CompositionLocalProvider
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AnimatedVisibility(
                    visible = shouldShowMenus,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            viewModel.toggleStealthMafiaMode()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                }
                            ) {
                                Text(
                                    text = "🕵️ تحلیل‌گر مافیا",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimaryDark
                                )
                                if (isStealthMafiaMode) {
                                    Text(
                                        text = " •",
                                        fontSize = 14.sp,
                                        color = TextMutedDark.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MafiaCrimson.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MafiaCrimson.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = stage.title,
                                        fontSize = 11.sp,
                                        color = MafiaCrimsonLight,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = { currentDestination = MainDestination.SETTINGS },
                                modifier = Modifier.testTag("topbar_game_name_btn")
                            ) {
                                Text(
                                    text = activeGame?.name ?: "انتخاب بازی",
                                    color = MafiaGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MafiaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MafiaDarkBg,
                            titleContentColor = TextPrimaryDark
                        )
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowMenus,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    NavigationBar(
                        containerColor = MafiaCardBg,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        MainDestination.values().forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MafiaCrimson,
                                    selectedTextColor = MafiaCrimson,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark,
                                    indicatorColor = MafiaCrimsonDark.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MafiaDarkBg
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    MainDestination.TARGETS -> Box(modifier = Modifier.fillMaxSize()) {
                        // Round-table view with numbered seats. Tap any
                        // player to open the SpeakerTargetScreen for them.
                        PlayerListTableScreen(
                            players = players,
                            activeSpeakersCount = players.count { !it.isEliminated },
                            activeGame = activeGame,
                            onStartTargeting = {
                                val activeSpeakers = players.filter { !it.isEliminated }
                                if (activeSpeakers.isNotEmpty()) {
                                    speakerFlowQueue = activeSpeakers
                                }
                            },
                            onPlayerTap = { tapped ->
                                speakerFlowQueue = listOf(tapped)
                            },
                            onSwapPlayers = { id1, id2 -> viewModel.swapPlayers(id1, id2) },
                            onMovePlayer = { p, up -> viewModel.movePlayer(p, up) }
                        )
                    }

                    MainDestination.ANALYSIS -> AnalysisContainerScreen(
                        currentStageIndex = currentStageIndex,
                        activeGame = activeGame,
                        players = players,
                        scores = scores,
                        targets = targets,
                        notes = notes,
                        speechGuide = speechGuide,
                        aiResult = aiResult,
                        initialSubTab = analysisInitialSubTab,
                        onStageSelected = { viewModel.setStage(it) },
                        onUpdateMafiaTeammates = { viewModel.updateMafiaTeammates(it) },
                        onUpdateGameScenarioAndOwner = { c, m, ind, oid, orole -> viewModel.updateGameScenarioAndOwner(c, m, ind, oid, orole) },
                        onTogglePlayerEliminated = { viewModel.togglePlayerEliminated(it) },
                        onRunAiAnalysis = { viewModel.runAiAnalysis() },
                        onClearAiAnalysis = { viewModel.clearAiAnalysis() },
                        onExportReport = { viewModel.generateShareableReport() }
                    )

                    MainDestination.SETTINGS -> Column(modifier = Modifier.fillMaxSize()) {
                        var settingsTab by remember { mutableStateOf(0) }
                        PrimaryTabRow(
                            selectedTabIndex = settingsTab,
                            containerColor = MafiaCardBg,
                            contentColor = MafiaCrimson
                        ) {
                            Tab(
                                selected = settingsTab == 0,
                                onClick = { settingsTab = 0 },
                                text = { Text("مدیریت بازی‌ها", fontWeight = if (settingsTab == 0) FontWeight.Bold else FontWeight.Normal) }
                            )
                            Tab(
                                selected = settingsTab == 1,
                                onClick = { settingsTab = 1 },
                                text = { Text("چیدمان صندلی", fontWeight = if (settingsTab == 1) FontWeight.Bold else FontWeight.Normal) }
                            )
                            Tab(
                                selected = settingsTab == 2,
                                onClick = { settingsTab = 2 },
                                text = { Text("ضرایب الگوریتم", fontWeight = if (settingsTab == 2) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }

                        if (settingsTab == 0) {
                            GameManagementScreen(
                                activeGameId = activeGame?.id,
                                allGames = allGames,
                                players = players,
                                onSelectGame = { viewModel.selectGame(it) },
                                onCreateGame = { name, pList, c, m, ind, oidx, orole, mTeammates ->
                                    viewModel.createNewGame(name, pList, c, m, ind, oidx, orole, mTeammates)
                                },
                                onRenameGame = { id, name -> viewModel.renameGame(id, name) },
                                onDuplicateGame = { viewModel.duplicateGame(it) },
                                onDeleteGame = { viewModel.deleteGame(it) },
                                onClearAllData = { viewModel.clearAllDataAndReset() },
                                onAddPlayer = { viewModel.addPlayer(it) },
                                onTogglePlayerEliminated = { viewModel.togglePlayerEliminated(it) },
                                onDeletePlayer = { viewModel.deletePlayer(it) },
                                onRenamePlayer = { pId, newName -> viewModel.renamePlayer(pId, newName) },
                                onMovePlayer = { p, dir -> viewModel.movePlayer(p, dir) }
                            )
                        } else if (settingsTab == 1) {
                            SeatLayoutScreen(
                                players = players,
                                onSwap = { id1: Long, id2: Long -> viewModel.swapPlayers(id1, id2) },
                                onMove = { p, dir -> viewModel.movePlayer(p, dir) }
                            )
                        } else {
                            SettingsScreen(
                                weights = algorithmWeights,
                                onSaveWeights = { viewModel.updateWeights(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
