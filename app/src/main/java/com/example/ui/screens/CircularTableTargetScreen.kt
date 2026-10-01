package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.PlayerNoteEntity
import com.example.data.local.SecretNoteEntity
import com.example.data.local.TargetEntity
import com.example.data.model.GameStage
import com.example.domain.algorithm.PlayerScoreAnalysis
import com.example.domain.model.CitizenConflict
import com.example.domain.model.MisdirectionTactic
import com.example.ui.components.StageSelectorTabs
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val QUICK_NOTE_TAGS = listOf(
    "تناقض",
    "سکوت مشکوک",
    "دفاع عصبی",
    "اتهام فله‌ای",
    "تغییر موضع",
    "خیلی محکم حرف زد",
    "ضعیف و مردد",
    "مشکوک به مافیا"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CircularTableTargetScreen(
    currentStageIndex: Int,
    unlockedStageIndex: Int = 0,
    activeGame: GameEntity? = null,
    players: List<PlayerEntity>,
    allTargets: List<TargetEntity>,
    allNotes: List<PlayerNoteEntity>,
    scores: List<PlayerScoreAnalysis>,
    isStealthMafiaMode: Boolean = false,
    secretNotes: List<SecretNoteEntity> = emptyList(),
    stealthConflicts: List<CitizenConflict> = emptyList(),
    stealthTactics: List<MisdirectionTactic> = emptyList(),
    onToggleStealthMode: () -> Unit = {},
    onStageSelected: (Int) -> Unit,
    onSaveTargets: (sourcePlayerId: Long, targetPlayerIds: List<Long>, isManual: Boolean) -> Unit,
    onFinishStageAndAdvance: (stageIndex: Int) -> Unit = {},
    onAddQuickNote: (playerId: Long, tag: String, freeText: String) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onAddSecretNote: (playerId: Long, content: String, suggestion: String) -> Unit = { _, _, _ -> },
    onDeleteSecretNote: (noteId: Long) -> Unit = {},
    onSwapPlayers: (player1Id: Long, player2Id: Long) -> Unit = { _, _ -> },
    onReorderPlayers: (List<PlayerEntity>) -> Unit = {},
    onRenamePlayer: (playerId: Long, newName: String) -> Unit = { _, _ -> },
    onTogglePlayerEliminated: (PlayerEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Active speaker player ID (prioritize living player)
    var selectedPlayerId by remember(players) {
        val firstLiving = players.firstOrNull { !it.isEliminated } ?: players.firstOrNull()
        mutableStateOf(firstLiving?.id ?: 0L)
    }

    // Auto-advance toggle (حالت دور میز)
    var autoAdvanceEnabled by remember { mutableStateOf(true) }

    // Direction: True = Clockwise (ساعت‌گرد), False = Counter-Clockwise (پادساعت‌گرد)
    var isClockwise by remember { mutableStateOf(true) }

    // Mode: False = Game Recording Mode (Instant Tap), True = Arrange Seats Mode (Instant Drag)
    var isArrangeMode by remember { mutableStateOf(false) }

    // Option to completely hide eliminated players from the table seats
    var hideEliminatedOnTable by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showQuickSwapSheet by remember { mutableStateOf(false) }
    var showMissedPlayersSheet by remember { mutableStateOf(false) }
    var showMostSuspiciousSheet by remember { mutableStateOf(false) }
    var showStealthAnalysisSheet by remember { mutableStateOf(false) }
    var noteTargetPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var showCloseStageDialog by remember { mutableStateOf(false) }
    var playerToRename by remember { mutableStateOf<PlayerEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showEliminationDialog by remember { mutableStateOf(false) }

    // Drag-and-drop & long-press state
    var draggingPlayerId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetPlayerId by remember { mutableStateOf<Long?>(null) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    // Ensure selected speaker is never an eliminated player if living players exist
    LaunchedEffect(players, currentStageIndex) {
        val currentSelected = players.firstOrNull { it.id == selectedPlayerId }
        if (currentSelected == null || currentSelected.isEliminated) {
            val firstLiving = players.firstOrNull { !it.isEliminated }
            if (firstLiving != null) {
                selectedPlayerId = firstLiving.id
            }
        }
    }

    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }

    // Existing targets saved in database for this speaker in this stage
    val existingTargetsForStage = remember(allTargets, currentStageIndex, selectedPlayerId) {
        allTargets
            .filter { it.stageIndex == currentStageIndex && it.sourcePlayerId == selectedPlayerId }
            .map { it.targetPlayerId }
            .toSet()
    }

    // Checked targets local state
    val checkedTargetIds = remember(existingTargetsForStage, selectedPlayerId) {
        mutableStateListOf<Long>().apply {
            addAll(existingTargetsForStage)
        }
    }

    // Map each player to how many targets they have recorded in this stage
    val stageTargetsCountByPlayer = remember(allTargets, currentStageIndex) {
        allTargets
            .filter { it.stageIndex == currentStageIndex }
            .groupBy { it.sourcePlayerId }
            .mapValues { it.value.size }
    }

    // Map scores by player ID
    val scoreMap = remember(scores) { scores.associateBy { it.playerId } }

    // Notes by player ID for this stage
    val notesByPlayer = remember(allNotes, currentStageIndex) {
        allNotes.filter { it.stageIndex <= currentStageIndex }.groupBy { it.playerId }
    }

    // Secret notes by player
    val secretNotesByPlayer = remember(secretNotes, currentStageIndex) {
        secretNotes.filter { it.stageIndex <= currentStageIndex }.groupBy { it.playerId }
    }

    val alivePlayers = remember(players) { players.filter { !it.isEliminated } }
    val playersWithTargets = remember(allTargets, currentStageIndex) {
        allTargets.filter { it.stageIndex == currentStageIndex }.map { it.sourcePlayerId }.toSet()
    }

    // Emergency "جا موندم" list: alive players who haven't recorded in current stage
    val missedPlayers = remember(alivePlayers, playersWithTargets) {
        alivePlayers.filter { it.id !in playersWithTargets }
    }

    val isAllTargetsComplete = alivePlayers.isNotEmpty() && missedPlayers.isEmpty()
    val currentSpeakerIndex = players.indexOfFirst { it.id == selectedPlayerId }
    val currentStage = GameStage.getStage(currentStageIndex)
    val nextStage = GameStage.getStage(currentStageIndex + 1)

    // Save helper: updates local state and triggers autosave
    val updateTargets: (List<Long>, Boolean) -> Unit = { newIds, isManual ->
        checkedTargetIds.clear()
        checkedTargetIds.addAll(newIds)
        onSaveTargets(selectedPlayerId, newIds, isManual)
    }

    // Navigation: jump to next living player (respecting clockwise / counter-clockwise)
    val moveToNextLivingPlayer: () -> Unit = {
        if (players.isNotEmpty()) {
            val total = players.size
            val step = if (isClockwise) 1 else -1
            var nextIdx = (currentSpeakerIndex + step + total) % total
            var found = false
            for (i in 0 until total) {
                val candidate = players[nextIdx]
                if (!candidate.isEliminated) {
                    selectedPlayerId = candidate.id
                    found = true
                    break
                }
                nextIdx = (nextIdx + step + total) % total
            }
            if (!found) {
                selectedPlayerId = players[(currentSpeakerIndex + step + total) % total].id
            }
        }
    }

    // Navigation: jump to previous living player
    val moveToPrevLivingPlayer: () -> Unit = {
        if (players.isNotEmpty()) {
            val total = players.size
            val step = if (isClockwise) -1 else 1
            var prevIdx = (currentSpeakerIndex + step + total) % total
            var found = false
            for (i in 0 until total) {
                val candidate = players[prevIdx]
                if (!candidate.isEliminated) {
                    selectedPlayerId = candidate.id
                    found = true
                    break
                }
                prevIdx = (prevIdx + step + total) % total
            }
            if (!found) {
                selectedPlayerId = players[(currentSpeakerIndex + step + total) % total].id
            }
        }
    }

    val ownerPlayer = remember(players, activeGame) {
        players.firstOrNull { it.isOwner || (activeGame?.ownerPlayerId != null && it.id == activeGame.ownerPlayerId) }
    }

    if (activeGame == null || players.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MafiaDarkBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = MafiaGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هیچ بازی فعالی وجود ندارد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "داده‌های بازی قبلی با موفقیت پاک شدند. از منوی تنظیمات یک بازی جدید تعریف کنید.",
                        fontSize = 13.sp,
                        color = TextSecondaryDark,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
    ) {
        if (!isArrangeMode) {
            // --- 1. STAGE TABS BAR ---
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                unlockedStageIndex = unlockedStageIndex,
                onStageSelected = onStageSelected
            )

            // --- 2. CONTROL TOOLBAR (حالت بازی) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(0.5.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Mode Selector Toggle (ثبت تارگت 🎯 vs چیدمان صندلی ✋) + Shortcut Quick Note 📝 + Eliminated 💀
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = {
                                    isArrangeMode = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MafiaSurfaceVariant,
                                border = BorderStroke(1.dp, MafiaBorder),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PanTool,
                                        contentDescription = null,
                                        tint = MafiaGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "✋ چیدمان",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Quick Note Shortcut Button
                            Surface(
                                onClick = {
                                    if (selectedPlayer != null) {
                                        noteTargetPlayer = selectedPlayer
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MafiaSurfaceVariant,
                                border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.6f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "یادداشت سریع",
                                        tint = MafiaGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "یادداشت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MafiaGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Elimination / Night Kill Management Button (کشته شب / حذف روز ۲)
                            val deadCount = players.count { it.isEliminated }
                            Surface(
                                onClick = {
                                    showEliminationDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (deadCount > 0) Color(0xFF3B1218) else MafiaSurfaceVariant,
                                border = BorderStroke(1.dp, if (deadCount > 0) MafiaCrimson else MafiaBorder),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = if (deadCount > 0) "💀 کشته‌ها ($deadCount)" else "💀 کشته شب / حذف",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (deadCount > 0) Color(0xFFFF8A80) else TextPrimaryDark
                                    )
                                }
                            }
                        }

                        // Right: Stealth or Close Stage Button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isStealthMafiaMode) {
                                Surface(
                                    onClick = { showStealthAnalysisSheet = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2A1B3D),
                                    border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.6f)),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = "⚡ اصطکاک",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFCE93D8)
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showCloseStageDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isAllTargetsComplete) MafiaGreen else MafiaBorder),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "پایان روز 🔒",
                                        fontSize = 11.sp,
                                        color = if (isAllTargetsComplete) MafiaGreen else TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- HEADER FOR ARRANGE MODE: خلوت، سریع و بدون منوهای مزاحم ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1425)),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✋ چیدمان صندلی‌ها",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MafiaGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Direction Switcher (ساعت‌گرد / پادساعت‌گرد)
                        Surface(
                            onClick = {
                                isClockwise = !isClockwise
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isClockwise) Color(0xFF1B5E20) else Color(0xFF0D47A1),
                            border = BorderStroke(1.dp, if (isClockwise) MafiaGreen else Color(0xFF64B5F6)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (isClockwise) "↷ ساعت‌گرد" else "↶ پادساعت‌گرد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Done & Confirm Button
                    Button(
                        onClick = {
                            isArrangeMode = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تأیید و خروج ✓",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // --- 3. CIRCULAR TABLE (میز دایره‌ای واقعی مافیا) ---
        // اجباری: محیط میز باید در مختصات LTR باشد تا محور X از چپ به راست افزایش یابد
        // در غیر این صورت در زبان فارسی (RTL)، تمام آفست‌ها و جابجایی‌ها برعکس (چپ به جای راست) اعمال می‌شوند
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
            val density = LocalDensity.current
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()
            val centerX = canvasWidthPx / 2f
            val centerY = canvasHeightPx / 2f

            val activePlayersOnTable = remember(players, isArrangeMode, hideEliminatedOnTable) {
                if (isArrangeMode || !hideEliminatedOnTable) players else players.filter { !it.isEliminated }
            }
            val totalPlayers = activePlayersOnTable.size
            val snapThresholdPx = with(density) { 56.dp.toPx() }

            // Usable boundary margins for rectangular table perimeter
            val marginX = with(density) { 40.dp.toPx() }
            val marginY = with(density) { 42.dp.toPx() }

            val rectLeft = marginX
            val rectRight = canvasWidthPx - marginX
            val rectTop = marginY
            val rectBottom = canvasHeightPx - marginY

            // Calculate rectangular slots with requested priority:
            // 1. Top row: up to 4 players (1, 2, 3, 4)
            // 2. Sides: left & right pairs row-by-row (5 & 6, 7 & 8, 9 & 10 ...)
            // 3. Bottom row: remaining players (e.g. 11, 12, 13, 14)
            val slotCenters = remember(totalPlayers, isClockwise, canvasWidthPx, canvasHeightPx) {
                if (totalPlayers == 0) emptyList()
                else {
                    val topCount: Int
                    val bottomCount: Int
                    val sideCount: Int

                    if (totalPlayers <= 4) {
                        topCount = totalPlayers
                        bottomCount = 0
                        sideCount = 0
                    } else if (totalPlayers in 5..7) {
                        topCount = 4
                        bottomCount = totalPlayers - 4
                        sideCount = 0
                    } else {
                        // >= 8 players: Top has 4, Bottom has 4, remainder goes to sides in pairs
                        topCount = 4
                        bottomCount = 4
                        sideCount = totalPlayers - 8
                    }

                    val sideRows = (sideCount + 1) / 2
                    val slots = mutableListOf<Offset>()

                    fun getTopX(i: Int): Float {
                        return if (topCount <= 1) centerX
                        else if (isClockwise) {
                            rectLeft + (i.toFloat() / (topCount - 1)) * (rectRight - rectLeft)
                        } else {
                            rectRight - (i.toFloat() / (topCount - 1)) * (rectRight - rectLeft)
                        }
                    }

                    fun getBottomX(i: Int): Float {
                        return if (bottomCount <= 1) centerX
                        else if (isClockwise) {
                            rectLeft + (i.toFloat() / (bottomCount - 1)) * (rectRight - rectLeft)
                        } else {
                            rectRight - (i.toFloat() / (bottomCount - 1)) * (rectRight - rectLeft)
                        }
                    }

                    // 1. اول بالا رو پر کن (1, 2, 3, 4)
                    for (i in 0 until topCount) {
                        slots.add(Offset(getTopX(i), rectTop))
                    }

                    // 2. بعد چپ و راست رو پر کن (5 و 6، 7 و 8، 9 و 10)
                    var addedSides = 0
                    for (row in 0 until sideRows) {
                        val y = rectTop + ((row + 1f) / (sideRows + 1f)) * (rectBottom - rectTop)
                        if (isClockwise) {
                            // Left side (5, 7, 9)
                            if (addedSides < sideCount) {
                                slots.add(Offset(rectLeft, y))
                                addedSides++
                            }
                            // Right side (6, 8, 10)
                            if (addedSides < sideCount) {
                                slots.add(Offset(rectRight, y))
                                addedSides++
                            }
                        } else {
                            // Right side first in counter-clockwise
                            if (addedSides < sideCount) {
                                slots.add(Offset(rectRight, y))
                                addedSides++
                            }
                            // Left side
                            if (addedSides < sideCount) {
                                slots.add(Offset(rectLeft, y))
                                addedSides++
                            }
                        }
                    }

                    // 3. آخر پایین رو پر کن (11, 12, 13, 14)
                    for (i in 0 until bottomCount) {
                        slots.add(Offset(getBottomX(i), rectBottom))
                    }

                    slots
                }
            }

            fun getSlotCenter(index: Int): Offset {
                return slotCenters.getOrElse(index) { Offset(centerX, centerY) }
            }

            // Outer decorative rectangular table felt (میز مستطیلی با فاصله مناسب)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF140D17).copy(alpha = 0.65f))
                    .border(
                        BorderStroke(1.5.dp, MafiaCrimsonDark.copy(alpha = 0.4f)),
                        RoundedCornerShape(28.dp)
                    )
            )

            // Draw Background Seat Slots on the table felt
            if (totalPlayers > 0) {
                for (slotIdx in 0 until totalPlayers) {
                    val slotCenter = getSlotCenter(slotIdx)
                    val playerAtSlot = activePlayersOnTable.getOrNull(slotIdx)
                    val seatNum = if (playerAtSlot != null) players.indexOfFirst { it.id == playerAtSlot.id } + 1 else slotIdx + 1
                    val isSlotHovered = (hoveredTargetPlayerId != null && playerAtSlot?.id == hoveredTargetPlayerId)

                    val slotXDp = with(density) { (slotCenter.x - with(density) { 26.dp.toPx() }).toDp() }
                    val slotYDp = with(density) { (slotCenter.y - with(density) { 26.dp.toPx() }).toDp() }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = with(density) { slotXDp.roundToPx() },
                                    y = with(density) { slotYDp.roundToPx() }
                                )
                            }
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSlotHovered) Color(0xFF00E676).copy(alpha = 0.25f)
                                else Color(0xFF1C1424).copy(alpha = 0.35f)
                            )
                            .border(
                                BorderStroke(
                                    width = if (isSlotHovered) 2.5.dp else 1.dp,
                                    color = if (isSlotHovered) Color(0xFF00E676) else MafiaBorder.copy(alpha = 0.4f)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSlotHovered) {
                            Text(
                                text = "مقصد ⇄",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        } else {
                            Text(
                                text = "$seatNum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedDark.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }

            // --- CENTER HUB: کپسول عریض مرکزی با چیپ‌های ۲ ستونه و اسکرول داخلی ---
            Box(
                modifier = Modifier
                    .width(178.dp)
                    .height(118.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (checkedTargetIds.isEmpty()) Color(0xFF160E1C).copy(alpha = 0.95f)
                        else Color(0xFF280B1E).copy(alpha = 0.95f)
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (checkedTargetIds.isEmpty()) MafiaBorder.copy(alpha = 0.6f)
                            else MafiaCrimsonLight
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .pointerInput(selectedPlayerId) {
                        detectTapGestures(
                            onTap = {
                                if (selectedPlayer != null) {
                                    noteTargetPlayer = selectedPlayer
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onLongPress = {
                                onToggleStealthMode()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isArrangeMode) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MafiaGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "چیدمان صندلی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                        Text(
                            text = "کشیدن صندلی‌ها برای جابجایی",
                            fontSize = 9.sp,
                            color = TextMutedDark
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Speaker Header Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPlayer?.name ?: "گوینده فعلی",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MafiaGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (checkedTargetIds.isNotEmpty()) {
                                Surface(
                                    color = MafiaCrimson,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${checkedTargetIds.size} تارگت",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 3.dp),
                            thickness = 0.5.dp,
                            color = MafiaBorder.copy(alpha = 0.4f)
                        )

                        if (checkedTargetIds.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "تارگت نزد 🚫",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = "لمس = یادداشت 📝",
                                    fontSize = 8.sp,
                                    color = TextMutedDark.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            // چیپ‌های ۲ ستونه همراه با اسکرول داخلی نرم
                            val targetPlayers = remember(checkedTargetIds, players) {
                                players.filter { it.id in checkedTargetIds }
                            }
                            val hubScrollState = rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(hubScrollState),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val chunkedTargets = targetPlayers.chunked(2)
                                for (rowPair in chunkedTargets) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (p in rowPair) {
                                            val seat = players.indexOfFirst { it.id == p.id } + 1
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF381023),
                                                border = BorderStroke(0.5.dp, Color(0xFFE040FB).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "🎯 $seat.",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFE040FB)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = p.name,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        if (rowPair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- RECTANGULAR SEATS: بازیکن‌ها دور میز مستطیلی ---
            if (totalPlayers > 0) {
                activePlayersOnTable.forEachIndexed { index, player ->
                    val seatNumber = players.indexOfFirst { it.id == player.id } + 1
                    val isOwner = (ownerPlayer?.id == player.id) || player.isOwner
                    val isSpeaker = player.id == selectedPlayerId
                    val isTargeted = checkedTargetIds.contains(player.id)
                    val recordedCount = stageTargetsCountByPlayer[player.id] ?: 0
                    val hasRecorded = recordedCount > 0

                    val defaultSlotCenter = getSlotCenter(index)
                    val isBeingDragged = (draggingPlayerId == player.id)
                    val isHoveredAsTarget = (hoveredTargetPlayerId == player.id)

                    val currentCenter = if (isBeingDragged) defaultSlotCenter + dragOffset else defaultSlotCenter

                    val nodeXDp = with(density) { (currentCenter.x - with(density) { 34.dp.toPx() }).toDp() }
                    val nodeYDp = with(density) { (currentCenter.y - with(density) { 34.dp.toPx() }).toDp() }

                    // Construct drag or tap modifier based on mode
                    val gestureModifier = if (isArrangeMode) {
                        // Arrange mode: responsive drag to swap seats + hold/long-press to rename
                        Modifier.pointerInput(player.id, players) {
                            detectDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                    draggingPlayerId = player.id
                                    dragOffset = Offset.Zero
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragDistance += dragAmount.getDistance()
                                    dragOffset += dragAmount

                                    // Find nearest target slot
                                    val currentPos = defaultSlotCenter + dragOffset
                                    val candidate = activePlayersOnTable.filter { it.id != player.id }.minByOrNull { other ->
                                        val otherIdx = activePlayersOnTable.indexOfFirst { it.id == other.id }
                                        (currentPos - getSlotCenter(otherIdx)).getDistance()
                                    }

                                    if (candidate != null) {
                                        val otherIdx = activePlayersOnTable.indexOfFirst { it.id == candidate.id }
                                        val dist = (currentPos - getSlotCenter(otherIdx)).getDistance()
                                        if (dist < snapThresholdPx) {
                                            if (hoveredTargetPlayerId != candidate.id) {
                                                hoveredTargetPlayerId = candidate.id
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else {
                                            hoveredTargetPlayerId = null
                                        }
                                    } else {
                                        hoveredTargetPlayerId = null
                                    }
                                },
                                onDragEnd = {
                                    val targetId = hoveredTargetPlayerId
                                    val elapsed = System.currentTimeMillis() - dragStartTime
                                    val thresholdMoved = with(density) { 16.dp.toPx() }
                                    if (targetId != null && targetId != player.id) {
                                        onSwapPlayers(player.id, targetId)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (totalDragDistance < thresholdMoved && elapsed >= 200L) {
                                        // User held mouse/finger on player without moving: Rename!
                                        playerToRename = player
                                        renameInputText = player.name
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    draggingPlayerId = null
                                    dragOffset = Offset.Zero
                                    hoveredTargetPlayerId = null
                                },
                                onDragCancel = {
                                    draggingPlayerId = null
                                    dragOffset = Offset.Zero
                                    hoveredTargetPlayerId = null
                                }
                            )
                        }
                    } else {
                        // Game Recording Mode: INSTANT TAP for target selection! Double-tap for quick note!
                        Modifier.pointerInput(player.id, selectedPlayerId, isTargeted, player.isEliminated) {
                            detectTapGestures(
                                onTap = {
                                    if (player.isEliminated) {
                                        showEliminationDialog = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (player.id == selectedPlayerId) {
                                        // Clicking speaker opens quick notes
                                        noteTargetPlayer = player
                                    } else {
                                        // INSTANT single-tap target toggle!
                                        val newSelection = if (isTargeted) {
                                            checkedTargetIds.filter { it != player.id }
                                        } else {
                                            checkedTargetIds + player.id
                                        }
                                        updateTargets(newSelection, false)
                                    }
                                },
                                onDoubleTap = {
                                    noteTargetPlayer = player
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onLongPress = {
                                    if (!player.isEliminated) {
                                        // Long press switches active speaker
                                        selectedPlayerId = player.id
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        showEliminationDialog = true
                                    }
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = with(density) { nodeXDp.roundToPx() },
                                    y = with(density) { nodeYDp.roundToPx() }
                                )
                            }
                            .zIndex(if (isBeingDragged) 10f else if (isHoveredAsTarget) 5f else 1f)
                            .then(gestureModifier)
                    ) {
                        // Player Circular Card Node
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(if (isArrangeMode) 78.dp else 68.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isBeingDragged -> Color(0xFF6A1B9A)
                                    isHoveredAsTarget -> Color(0xFF00C853)
                                    isArrangeMode -> MafiaCardElevated
                                    isSpeaker -> MafiaCrimson
                                    isTargeted -> Color(0xFF4A148C)
                                    player.isEliminated -> Color(0xFF212121)
                                    hasRecorded -> MafiaCardElevated
                                    else -> MafiaSurfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (isBeingDragged || isHoveredAsTarget) 3.dp else if (isArrangeMode) 2.dp else if (isSpeaker) 3.dp else if (isTargeted) 2.5.dp else 1.dp,
                                    color = when {
                                        isBeingDragged -> MafiaGold
                                        isHoveredAsTarget -> Color(0xFF69F0AE)
                                        isArrangeMode -> MafiaGold.copy(alpha = 0.8f)
                                        isSpeaker -> Color.White
                                        isTargeted -> Color(0xFFE040FB)
                                        hasRecorded -> MafiaGreen.copy(alpha = 0.8f)
                                        player.isEliminated -> MafiaBorder.copy(alpha = 0.3f)
                                        else -> MafiaBorder
                                    }
                                ),
                                shadowElevation = if (isBeingDragged) 12.dp else if (isSpeaker || isTargeted || isArrangeMode) 6.dp else 2.dp,
                                modifier = Modifier
                                    .size(54.dp)
                                    .scale(if (isBeingDragged) 1.15f else 1f)
                                    .testTag("player_circle_${player.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Seat number or dead skull or crown
                                        if (isArrangeMode) {
                                            Text(
                                                text = if (isOwner) "👑 $seatNumber" else "$seatNumber",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isOwner) 13.sp else 17.sp,
                                                color = if (isBeingDragged || isOwner) MafiaGold else Color.White
                                            )
                                        } else {
                                            Text(
                                                text = if (player.isEliminated) "💀" else if (isOwner) "👑" else "$seatNumber",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (player.isEliminated || isOwner) 15.sp else 13.sp,
                                                color = if (isOwner) MafiaGold else if (isSpeaker || isTargeted) Color.White else TextPrimaryDark
                                            )
                                        }

                                        // Status: targeted badge or count
                                        if (!isArrangeMode) {
                                            if (isTargeted) {
                                                Text(text = "🎯", fontSize = 11.sp)
                                            } else if (hasRecorded) {
                                                Text(
                                                    text = "✓$recordedCount",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSpeaker) MafiaGold else MafiaGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            if (isArrangeMode) {
                                // Tap on name in arrange mode also directly opens rename dialog
                                Surface(
                                    onClick = {
                                        playerToRename = player
                                        renameInputText = player.name
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isOwner) "👑 ${player.name}" else player.name,
                                            fontWeight = if (isBeingDragged || isOwner) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isBeingDragged || isOwner) MafiaGold else Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تغییر نام",
                                            tint = MafiaGold.copy(alpha = 0.85f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            } else {
                                // Player Name below circle (clearly visible and uncluttered)
                                val displayName = when {
                                    player.isEliminated -> "💀 ${player.name}"
                                    isOwner -> "👑 ${player.name}"
                                    else -> player.name
                                }
                                Text(
                                    text = displayName,
                                    fontWeight = if (isBeingDragged || isOwner) FontWeight.ExtraBold else if (isSpeaker) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = when {
                                        player.isEliminated -> TextMutedDark
                                        isBeingDragged -> MafiaGold
                                        isOwner -> MafiaGold
                                        isSpeaker -> MafiaCrimsonLight
                                        else -> TextPrimaryDark
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // --- 4. ACTION CONTROLS & NAVIGATION BAR ---
        if (!isArrangeMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Row 1: Speaker Info & Clear / No-Target Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speaker Label & Quick Note
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "گوینده فعلی:",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (selectedPlayer != null) {
                                Text(
                                    text = "صندلی ${currentSpeakerIndex + 1} (${selectedPlayer.name})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaCrimsonLight
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { noteTargetPlayer = selectedPlayer },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "یادداشت گوینده",
                                    tint = MafiaGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Button: «تارگت نزد» (Clear all targets & mark zero)
                        Button(
                            onClick = {
                                updateTargets(emptyList(), false)
                                if (autoAdvanceEnabled) {
                                    moveToNextLivingPlayer()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MafiaBorder),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_no_targets")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MafiaGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تارگت نزد 🚫",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MafiaGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Large One-Hand Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previous Player Button
                        Button(
                            onClick = moveToPrevLivingPlayer,
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_prev_player")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "بازیکن قبلی",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "قبلی ◀",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        // Next Living Player (دور میز) Button
                        Button(
                            onClick = moveToNextLivingPlayer,
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimsonDark),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MafiaCrimsonLight),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("btn_next_player")
                        ) {
                            Text(
                                text = if (isClockwise) "▶ بعدی (ساعت‌گرد ↷)" else "▶ بعدی (پادساعت‌گرد ↶)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازیکن بعدی",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // --- DEDICATED CLEAN FOOTER FOR ARRANGE MODE ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reverse
                    OutlinedButton(
                        onClick = {
                            if (players.size > 1) {
                                onReorderPlayers(players.reversed())
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MafiaGold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "معکوس ⇄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                    }

                    // Shift Forward / Backward
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                if (players.size > 1) {
                                    val rotated = listOf(players.last()) + players.dropLast(1)
                                    onReorderPlayers(rotated)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("↷ چرخش جلو", fontSize = 11.sp, color = TextPrimaryDark)
                        }

                        Button(
                            onClick = {
                                if (players.size > 1) {
                                    val rotated = players.drop(1) + listOf(players.first())
                                    onReorderPlayers(rotated)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("↶ چرخش عقب", fontSize = 11.sp, color = TextPrimaryDark)
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: جابجایی سریع صندلی‌ها (Fast Swap & Reorder Dialog) ---
    if (showQuickSwapSheet) {
        var firstSelectedId by remember { mutableStateOf<Long?>(null) }
        var secondSelectedId by remember { mutableStateOf<Long?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showQuickSwapSheet = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MafiaGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "چیدمان و جابجایی صندلی‌های میز",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = { showQuickSwapSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 1: Quick 2-Player Swap
                Text(
                    text = "۱. تعویض جای دو صندلی (۱ لمس روی اولی، ۱ لمس روی دومی):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MafiaGold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(players, key = { _, p -> p.id }) { index, p ->
                        val isFirst = (firstSelectedId == p.id)
                        val isSecond = (secondSelectedId == p.id)
                        val seatNum = index + 1

                        Surface(
                            onClick = {
                                if (firstSelectedId == null) {
                                    firstSelectedId = p.id
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (firstSelectedId == p.id) {
                                    firstSelectedId = null
                                } else {
                                    // Second player clicked: SWAP IMMEDIATELY!
                                    secondSelectedId = p.id
                                    onSwapPlayers(firstSelectedId!!, p.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    firstSelectedId = null
                                    secondSelectedId = null
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isFirst -> MafiaCrimson
                                isSecond -> Color(0xFF00C853)
                                else -> MafiaSurfaceVariant
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    isFirst -> Color.White
                                    isSecond -> Color(0xFF69F0AE)
                                    else -> MafiaBorder
                                }
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "$seatNum. ${p.name}",
                                    fontWeight = if (isFirst || isSecond) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isFirst || isSecond) Color.White else TextPrimaryDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Table Rotation (Clockwise & Counter-Clockwise)
                Text(
                    text = "۲. چرخش کل میز (همگام‌سازی سریع با سالن بازی):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MafiaGold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (players.size > 1) {
                                val rotated = listOf(players.last()) + players.dropLast(1)
                                onReorderPlayers(rotated)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("چرخش ساعتگرد ↷", fontSize = 11.sp, color = TextPrimaryDark)
                    }

                    Button(
                        onClick = {
                            if (players.size > 1) {
                                val rotated = players.drop(1) + listOf(players.first())
                                onReorderPlayers(rotated)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("چرخش پادساعتگرد ↶", fontSize = 11.sp, color = TextPrimaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Reorder List with Up/Down buttons
                Text(
                    text = "۳. تغییر دستی ردیف صندلی‌ها:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(players, key = { _, p -> p.id }) { index, p ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MafiaSurfaceVariant,
                            border = BorderStroke(0.5.dp, MafiaBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صندلی ${index + 1}: ${p.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val list = players.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index - 1, item)
                                                onReorderPlayers(list)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("⬆️", fontSize = 12.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < players.size - 1) {
                                                val list = players.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index + 1, item)
                                                onReorderPlayers(list)
                                            }
                                        },
                                        enabled = index < players.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("⬇️", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 1: یادداشت سریع (بدون منوی اضافی) ---
    noteTargetPlayer?.let { player ->
        val playerNotes = notesByPlayer[player.id] ?: emptyList()
        val playerSecretNotes = secretNotesByPlayer[player.id] ?: emptyList()
        var freeText by remember { mutableStateOf("") }
        var secretText by remember { mutableStateOf("") }
        val seatNum = players.indexOfFirst { it.id == player.id } + 1

        ModalBottomSheet(
            onDismissRequest = { noteTargetPlayer = null },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MafiaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "یادداشت سریع صندلی $seatNum (${player.name})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(
                        onClick = { noteTargetPlayer = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 8 One-Click Ready Tag Buttons
                Text(
                    text = "تگ‌های آماده یک‌کلیکه:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MafiaGold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QUICK_NOTE_TAGS.forEach { tag ->
                        val hasTag = playerNotes.any { it.text.contains(tag) }
                        Surface(
                            onClick = {
                                onAddQuickNote(player.id, tag, "")
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasTag) MafiaGold else MafiaSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (hasTag) MafiaGold else MafiaBorder
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                if (hasTag) {
                                    Text("✓ ", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = if (hasTag) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasTag) Color.Black else TextSecondaryDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Free Short Note TextField (Max 2 lines)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = freeText,
                        onValueChange = { if (it.length <= 100) freeText = it },
                        placeholder = { Text("یادداشت آزاد (حداکثر ۲ خط)...", fontSize = 12.sp, color = TextMutedDark) },
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MafiaCrimsonLight,
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (freeText.isNotBlank()) {
                                onAddQuickNote(player.id, "یادداشت", freeText)
                                freeText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("ثبت", fontSize = 12.sp)
                    }
                }

                // SECRET MAFIA MODE NOTE SECTION (ONLY VISIBLE IN STEALTH MODE)
                if (isStealthMafiaMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF23122E),
                        border = BorderStroke(1.dp, Color(0xFF8E24AA).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🔒 یادداشت فوق‌محرمانه:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCE93D8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = secretText,
                                    onValueChange = { secretText = it },
                                    placeholder = { Text("نکته خصوصی برای بازی مافیا...", fontSize = 11.sp, color = TextMutedDark) },
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFAB47BC),
                                        unfocusedBorderColor = Color(0xFF7B1FA2),
                                        focusedTextColor = TextPrimaryDark,
                                        unfocusedTextColor = TextPrimaryDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (secretText.isNotBlank()) {
                                            onAddSecretNote(player.id, secretText, "")
                                            secretText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("ذخیره امن", fontSize = 11.sp)
                                }
                            }

                            if (playerSecretNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                playerSecretNotes.forEach { sn ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "• ${sn.content}", fontSize = 11.sp, color = Color(0xFFE1BEE7))
                                        IconButton(onClick = { onDeleteSecretNote(sn.id) }, modifier = Modifier.size(20.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Existing Notes list for this player
                if (playerNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "سوابق یادداشت‌های ثبت‌شده:", fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    playerNotes.forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${note.text}", fontSize = 11.sp, color = TextPrimaryDark)
                            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(20.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "حذف", tint = TextMutedDark, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 2: مشکوک‌ترین‌ها الان ---
    if (showMostSuspiciousSheet) {
        val topSuspicious = remember(scores) {
            scores.sortedByDescending { it.totalScore }.take(5)
        }

        ModalBottomSheet(
            onDismissRequest = { showMostSuspiciousSheet = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MafiaCrimson, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "مشکوک‌ترین افراد در این لحظه", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryDark)
                    }
                    IconButton(onClick = { showMostSuspiciousSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                topSuspicious.forEachIndexed { rank, scoreItem ->
                    val playerObj = players.firstOrNull { it.id == scoreItem.playerId }
                    val seat = players.indexOfFirst { it.id == scoreItem.playerId } + 1
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MafiaSurfaceVariant,
                        border = BorderStroke(1.dp, if (rank == 0) MafiaCrimson else MafiaBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "#${rank + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MafiaGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "صندلی $seat: ${playerObj?.name ?: "نامشخص"}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
                                    val reason = when {
                                        scoreItem.targetFlipsCount > 0 -> "⚡ تغییر موضع ناگهانی نسبت به روز قبل"
                                        scoreItem.targetsReceivedCount > 2 -> "🎯 دریافت تارگت‌های سنگین از سایرین"
                                        scoreItem.suspiciousNotesCount > 0 -> "📝 ثبت رفتارهای متناقض و مشکوک"
                                        scoreItem.manualScore > 65 -> "📌 امتیاز شک دستی بالا"
                                        else -> "دریافت تارگت‌های متعدد از دیگران"
                                    }
                                    Text(text = reason, fontSize = 10.sp, color = TextSecondaryDark)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    scoreItem.totalScore >= 70 -> Color(0xFFB71C1C)
                                    scoreItem.totalScore >= 55 -> Color(0xFFE65100)
                                    else -> Color(0xFF1B5E20)
                                }
                            ) {
                                Text(
                                    text = "${scoreItem.totalScore}٪",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 3: لیست اضطراری جا مونده‌ها (EMERGENCY SHEET) ---
    if (showMissedPlayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMissedPlayersSheet = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "لیست جامانده‌ها در ${currentStage.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryDark)
                    }
                    IconButton(onClick = { showMissedPlayersSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (missedPlayers.isEmpty()) {
                    Text(
                        text = "همه بازیکنان زنده تارگت‌های خود را ثبت کرده‌اند! 🎉",
                        fontSize = 13.sp,
                        color = MafiaGreen,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = "با لمس هر بازیکن، نوبت فوراً به او منتقل می‌شود تا تارگت‌هایش را ثبت کنید:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    missedPlayers.forEach { p ->
                        val seat = players.indexOfFirst { it.id == p.id } + 1
                        Surface(
                            onClick = {
                                selectedPlayerId = p.id
                                showMissedPlayersSheet = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MafiaSurfaceVariant,
                            border = BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صندلی $seat: ${p.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "ثبت نوبت ◀",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaCrimsonLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 4: تحلیل اصطکاک شهروندان و پیشنهادات مخفی (حالت مافیا) ---
    if (showStealthAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStealthAnalysisSheet = false },
            containerColor = Color(0xFF1E102B),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ تحلیل اصطکاک و تارگت‌های ایمن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFCE93D8)
                    )
                    IconButton(onClick = { showStealthAnalysisSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFCE93D8))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "۱. اصطکاک‌های شدید بین شهروندان:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCE93D8)
                )
                Spacer(modifier = Modifier.height(4.dp))

                stealthConflicts.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF28183B),
                        border = BorderStroke(1.dp, Color(0xFF7B1FA2).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "⚔️ صندلی ${c.player1Seat} (${c.player1Name}) ↔ صندلی ${c.player2Seat} (${c.player2Name})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(text = c.description, fontSize = 11.sp, color = Color(0xFFCE93D8))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "💡 ترفند: ${c.exploitTip}",
                                fontSize = 10.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "۲. تارگت‌های پیشنهادی برای دودزا کردن:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCE93D8)
                )
                Spacer(modifier = Modifier.height(4.dp))

                stealthTactics.forEach { t ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF28183B),
                        border = BorderStroke(1.dp, Color(0xFF7B1FA2).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🎯 صندلی ${t.targetSeat} (${t.targetPlayerName}) - ${t.tacticTitle}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(text = t.tacticDescription, fontSize = 10.sp, color = Color(0xFFE1BEE7))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog for manually closing the stage (with Day 2 night kill option)
    if (showCloseStageDialog) {
        var selectedNightKillPlayerId by remember { mutableStateOf<Long?>(null) }
        val isEnteringDayStage = (currentStageIndex == 0 || currentStageIndex == 1)

        AlertDialog(
            onDismissRequest = { showCloseStageDialog = false },
            containerColor = MafiaCardBg,
            title = {
                Text(
                    text = "اعلام پایان «${currentStage.title}»",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "آیا اطمینان دارید که می‌خواهید پرونده «${currentStage.title}» را ببندید و وارد «${nextStage.title}» شوید؟",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )

                    if (isEnteringDayStage) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF281015),
                            border = BorderStroke(1.dp, MafiaCrimsonDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "💀 ثبت کشته شب ۱ (ورود به روز ۲):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                                Text(
                                    text = "اگر در فاز شب بازیکنی کشته شد انتخاب کنید تا از بازی حذف شود:",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedNightKillPlayerId == null,
                                            onClick = { selectedNightKillPlayerId = null },
                                            label = { Text("بدون کشته (شات نجات)", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MafiaGreen,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                    items(players.filter { !it.isEliminated }, key = { it.id }) { p ->
                                        val isSel = selectedNightKillPlayerId == p.id
                                        val seat = players.indexOfFirst { it.id == p.id } + 1
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedNightKillPlayerId = if (isSel) null else p.id },
                                            label = { Text("$seat. ${p.name}", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MafiaCrimson,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "پس از بستن، مرحله ${nextStage.title} باز و فعال خواهد شد.",
                        fontSize = 12.sp,
                        color = MafiaGold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val killId = selectedNightKillPlayerId
                        if (killId != null) {
                            val deadP = players.firstOrNull { it.id == killId }
                            if (deadP != null && !deadP.isEliminated) {
                                onTogglePlayerEliminated(deadP)
                            }
                        }
                        showCloseStageDialog = false
                        onFinishStageAndAdvance(currentStageIndex)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimson)
                ) {
                    Text("بستن پرونده و شروع ${nextStage.title}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseStageDialog = false }) {
                    Text("انصراف", color = TextSecondaryDark)
                }
            }
        )
    }

    // Rename Dialog for player (فعال با نگه داشتن ماوس روی بازیکن یا لمس نام)
    playerToRename?.let { player ->
        val seatNum = players.indexOfFirst { it.id == player.id } + 1
        AlertDialog(
            onDismissRequest = { playerToRename = null },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MafiaGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ویرایش نام صندلی $seatNum",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MafiaGold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "نام جدید بازیکن را وارد کنید:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MafiaGold,
                            unfocusedBorderColor = MafiaBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = MafiaGold
                        ),
                        placeholder = { Text("نام بازیکن", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInputText.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenamePlayer(player.id, trimmed)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        playerToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ذخیره نام", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToRename = null }) {
                    Text("انصراف", color = TextSecondaryDark)
                }
            }
        )
    }

    // Dialog for Managing Eliminated / Killed Players (کشته‌های بازی)
    if (showEliminationDialog) {
        AlertDialog(
            onDismissRequest = { showEliminationDialog = false },
            containerColor = MafiaCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💀 مدیریت بازیکنان حذف‌شده (کشته‌های بازی)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MafiaCrimsonLight
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    Text(
                        text = "در روز ۲ و روزهای بعد، بازیکنانی که در شب یا رأی‌گیری حذف شده‌اند را علامت بزنید تا از چرخه نوبت صحبت خارج شده و الگوریتم دقیق‌تر محاسبه کند:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle hide eliminated on table
                    Surface(
                        onClick = { hideEliminatedOnTable = !hideEliminatedOnTable },
                        shape = RoundedCornerShape(8.dp),
                        color = MafiaSurfaceVariant,
                        border = BorderStroke(1.dp, if (hideEliminatedOnTable) MafiaGold else MafiaBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚫 حذف کامل صندلی از دور میز",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (hideEliminatedOnTable) "صندلی کشته‌ها کلاً از میز برداشته شده است" else "صندلی‌ها با علامت 💀 خاکستری نمایش داده می‌شوند",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            Switch(
                                checked = hideEliminatedOnTable,
                                onCheckedChange = { hideEliminatedOnTable = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "وضعیت بازیکنان (${players.count { !it.isEliminated }} زنده / ${players.count { it.isEliminated }} کشته):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(players, key = { it.id }) { p ->
                            val seat = players.indexOfFirst { it.id == p.id } + 1
                            val isMe = (p.id == ownerPlayer?.id) || p.isOwner
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
                                        Text(
                                            text = if (p.isEliminated) "💀" else "🟢",
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "صندلی $seat: ${p.name}" + if (isMe) " (👑 من)" else "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (p.isEliminated) TextMutedDark else TextPrimaryDark
                                            )
                                            Text(
                                                text = if (p.isEliminated) "کشته شده (مرحله ${GameStage.getStage(p.eliminatedStageIndex ?: currentStageIndex).title})" else "در حال بازی",
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
                    onClick = { showEliminationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaGold)
                ) {
                    Text("تأیید و بازگشت به بازی", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
