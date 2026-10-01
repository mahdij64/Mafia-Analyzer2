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
        var showMenusOnTable by remember { mutableStateOf(false) }
        val shouldShowMenus = !isTableScreen || showMenusOnTable

        Scaffold(
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
                    .padding(
                        if (isTableScreen && !shouldShowMenus) PaddingValues(0.dp) else paddingValues
                    )
                    .pointerInput(isTableScreen) {
                        if (isTableScreen) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val pos = event.changes.firstOrNull()?.position
                                    if (pos != null) {
                                        val nearEdge = pos.y < 80f || pos.y > (size.height - 80f)
                                        if (nearEdge && !showMenusOnTable) {
                                            showMenusOnTable = true
                                        } else if (!nearEdge && showMenusOnTable) {
                                            showMenusOnTable = false
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                when (currentDestination) {
                    MainDestination.TARGETS -> Box(modifier = Modifier.fillMaxSize()) {
                        CircularTableTargetScreen(
                            currentStageIndex = currentStageIndex,
                            unlockedStageIndex = maxUnlockedStageIndex,
                            activeGame = activeGame,
                            players = players,
                            allTargets = targets,
                            allNotes = notes,
                            scores = scores,
                            isStealthMafiaMode = isStealthMafiaMode,
                            secretNotes = secretNotes,
                            stealthConflicts = stealthAnalysis.first,
                            stealthTactics = stealthAnalysis.second,
                            onToggleStealthMode = { viewModel.toggleStealthMafiaMode() },
                            onStageSelected = { viewModel.setStage(it) },
                            onSaveTargets = { src, tgts, isManual -> viewModel.saveTargets(src, tgts, isManual) },
                            onFinishStageAndAdvance = { stg -> viewModel.finishStageAndAdvance(stg) },
                            onAddQuickNote = { pid, tag, freeText -> viewModel.addQuickTagNote(pid, tag, freeText) },
                            onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
                            onAddSecretNote = { pid, content, sugg -> viewModel.addSecretNote(pid, content, sugg) },
                            onDeleteSecretNote = { noteId -> viewModel.deleteSecretNote(noteId) },
                            onSwapPlayers = { p1, p2 -> viewModel.swapPlayers(p1, p2) },
                            onReorderPlayers = { list -> viewModel.reorderPlayers(list) },
                            onRenamePlayer = { pid, newName -> viewModel.renamePlayer(pid, newName) },
                            onTogglePlayerEliminated = { viewModel.togglePlayerEliminated(it) }
                        )

                        // Discreet toggle button for touchscreen / mouse to easily toggle menus on table screen
                        FloatingActionButton(
                            onClick = { showMenusOnTable = !showMenusOnTable },
                            containerColor = if (showMenusOnTable) MafiaGold else MafiaCardBg.copy(alpha = 0.85f),
                            contentColor = if (showMenusOnTable) Color.Black else MafiaGold,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 8.dp, top = 8.dp)
                                .size(34.dp)
                                .testTag("toggle_table_menus_btn")
                        ) {
                            Icon(
                                imageVector = if (showMenusOnTable) Icons.Default.FullscreenExit else Icons.Default.Menu,
                                contentDescription = if (showMenusOnTable) "مخفی‌سازی منوها" else "نمایش منوها",
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                                text = { Text("ضرایب الگوریتم و تنظیمات", fontWeight = if (settingsTab == 1) FontWeight.Bold else FontWeight.Normal) }
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
