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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun TargetRecordingScreen(
    currentStageIndex: Int,
    unlockedStageIndex: Int = 0,
    players: List<PlayerEntity>,
    allTargets: List<TargetEntity>,
    allNotes: List<PlayerNoteEntity>,
    scores: List<PlayerScoreAnalysis>,
    isStealthMafiaMode: Boolean = false,
    secretNotes: List<SecretNoteEntity> = emptyList(),
    stealthConflicts: List<CitizenConflict> = emptyList(),
    stealthTactics: List<MisdirectionTactic> = emptyList(),
    onStageSelected: (Int) -> Unit,
    onSaveTargets: (sourcePlayerId: Long, targetPlayerIds: List<Long>, isManual: Boolean) -> Unit,
    onFinishStageAndAdvance: (stageIndex: Int) -> Unit = {},
    onAddQuickNote: (playerId: Long, tag: String, freeText: String) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onAddSecretNote: (playerId: Long, content: String, suggestion: String) -> Unit = { _, _, _ -> },
    onDeleteSecretNote: (noteId: Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Active speaker player ID
    var selectedPlayerId by remember(players) {
        mutableStateOf(players.firstOrNull()?.id ?: 0L)
    }

    // Auto-advance toggle (حالت دور میز)
    var autoAdvanceEnabled by remember { mutableStateOf(true) }

    // Dialog & Sheet States
    var showMissedPlayersSheet by remember { mutableStateOf(false) }
    var showMostSuspiciousSheet by remember { mutableStateOf(false) }
    var showStealthAnalysisSheet by remember { mutableStateOf(false) }
    var noteTargetPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var showCloseStageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(players) {
        if (players.isNotEmpty() && players.none { it.id == selectedPlayerId }) {
            selectedPlayerId = players.first().id
        }
    }

    val selectedPlayer = players.firstOrNull { it.id == selectedPlayerId }
    val candidateTargets = remember(players, selectedPlayerId) {
        players.filter { it.id != selectedPlayerId }
    }

    // Existing targets saved in database for this player in this stage
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

    // Emergency "جا موندم" list: alive players who have 0 targets recorded
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

    // Navigation: jump to next living player
    val moveToNextLivingPlayer: () -> Unit = {
        if (players.isNotEmpty()) {
            val total = players.size
            var nextIdx = (currentSpeakerIndex + 1) % total
            var found = false
            for (i in 0 until total) {
                val candidate = players[nextIdx]
                if (!candidate.isEliminated) {
                    selectedPlayerId = candidate.id
                    found = true
                    break
                }
                nextIdx = (nextIdx + 1) % total
            }
            if (!found) {
                selectedPlayerId = players[(currentSpeakerIndex + 1) % total].id
            }
        }
    }

    // Navigation: jump to previous living player
    val moveToPrevLivingPlayer: () -> Unit = {
        if (players.isNotEmpty()) {
            val total = players.size
            var prevIdx = (currentSpeakerIndex - 1 + total) % total
            var found = false
            for (i in 0 until total) {
                val candidate = players[prevIdx]
                if (!candidate.isEliminated) {
                    selectedPlayerId = candidate.id
                    found = true
                    break
                }
                prevIdx = (prevIdx - 1 + total) % total
            }
            if (!found) {
                selectedPlayerId = players[(currentSpeakerIndex - 1 + total) % total].id
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 1. Stage Selector Tabs
        item {
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                unlockedStageIndex = unlockedStageIndex,
                onStageSelected = onStageSelected
            )
        }

        // 2. High-Speed Utility Header (جا موندم + مشکوک‌ترین‌ها + پایان روز + ترفند مخفی)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emergency "جا موندم" button
                Button(
                    onClick = { showMissedPlayersSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (missedPlayers.isNotEmpty()) Color(0xFFB71C1C) else MafiaSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_emergency_missed")
                ) {
                    Icon(
                        imageVector = if (missedPlayers.isNotEmpty()) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (missedPlayers.isNotEmpty()) Color.White else MafiaGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (missedPlayers.isNotEmpty()) "جا موندم (${missedPlayers.size})" else "تکمیل ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (missedPlayers.isNotEmpty()) Color.White else MafiaGreen
                    )
                }

                // Middle: Most Suspicious button
                OutlinedButton(
                    onClick = { showMostSuspiciousSheet = true },
                    border = BorderStroke(1.dp, MafiaGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MafiaGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشکوک‌ترین‌ها الان",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MafiaGold
                    )
                }

                // Right: Stealth analysis (if active) OR Close Stage button
                if (isStealthMafiaMode) {
                    Surface(
                        onClick = { showStealthAnalysisSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2A1B3D),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.6f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "⚡ تحلیل اصطکاک",
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "پایان ${currentStage.title} 🔒",
                            fontSize = 11.sp,
                            color = if (isAllTargetsComplete) MafiaGreen else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // 3. ناوبری بر اساس شماره صندلی (Seat Navigation Bar)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "شماره صندلی (دور میز):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (selectedPlayer != null) {
                                Text(
                                    text = "صندلی ${currentSpeakerIndex + 1} (${selectedPlayer.name})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaCrimsonLight
                                )
                            }
                        }

                        // Auto-advance toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { autoAdvanceEnabled = !autoAdvanceEnabled }
                        ) {
                            Text(
                                text = "پرش خودکار",
                                fontSize = 10.sp,
                                color = if (autoAdvanceEnabled) MafiaGreen else TextMutedDark
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Switch(
                                checked = autoAdvanceEnabled,
                                onCheckedChange = { autoAdvanceEnabled = it },
                                modifier = Modifier.height(20.dp),
                                thumbContent = null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Row of Seats (1 .. N)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(players, key = { _, it -> it.id }) { index, p ->
                            val seatNumber = index + 1
                            val isSelected = p.id == selectedPlayerId
                            val recordedCount = stageTargetsCountByPlayer[p.id] ?: 0
                            val hasRecorded = recordedCount > 0

                            Surface(
                                selected = isSelected,
                                onClick = { selectedPlayerId = p.id },
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> MafiaCrimsonDark
                                    p.isEliminated -> MafiaDarkBg.copy(alpha = 0.5f)
                                    hasRecorded -> MafiaCardElevated
                                    else -> MafiaSurfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = when {
                                        isSelected -> MafiaCrimsonLight
                                        hasRecorded -> MafiaGreen.copy(alpha = 0.6f)
                                        p.isEliminated -> MafiaBorder.copy(alpha = 0.4f)
                                        else -> MafiaBorder
                                    }
                                ),
                                modifier = Modifier
                                    .size(width = 44.dp, height = 48.dp)
                                    .testTag("seat_btn_$seatNumber")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Seat number or dead skull
                                    Text(
                                        text = if (p.isEliminated) "💀" else "$seatNumber",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) TextPrimaryDark else if (p.isEliminated) TextMutedDark else TextSecondaryDark
                                    )
                                    // Status dot/count
                                    if (hasRecorded) {
                                        Text(
                                            text = "🎯$recordedCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MafiaGold else MafiaGreen
                                        )
                                    } else if (!p.isEliminated) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE53935))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. دکمه‌های بزرگ «قبلی» و «بعدی» برای ناوبری سریع با یک دست
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Large "قبلی" Button (48dp height minimum touch target)
                Button(
                    onClick = moveToPrevLivingPlayer,
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_prev_player")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "بازیکن قبلی",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "قبلی ◀",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                // Large "بعدی" Button (48dp height minimum touch target)
                Button(
                    onClick = {
                        if (autoAdvanceEnabled) {
                            moveToNextLivingPlayer()
                        } else {
                            moveToNextLivingPlayer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimsonDark),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MafiaCrimsonLight),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_next_player")
                ) {
                    Text(
                        text = "▶ بعدی (صندلی بعد)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازیکن بعدی",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. قسمت ثبت تارگت‌ها (FlowRow فشرده + یادداشت روی همان صفحه)
        if (selectedPlayer != null) {
            item {
                val speakerScore = scoreMap[selectedPlayer.id]?.totalScore ?: 50
                val speakerNotes = notesByPlayer[selectedPlayer.id] ?: emptyList()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MafiaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Speaker header with note button and suspicion score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MafiaCrimsonDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${currentSpeakerIndex + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "تارگت‌های «${selectedPlayer.name}»:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Suspicion score tag for speaker
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when {
                                                speakerScore >= 70 -> Color(0xFFB71C1C)
                                                speakerScore >= 55 -> Color(0xFFE65100)
                                                else -> Color(0xFF1B5E20)
                                            }
                                        ) {
                                            Text(
                                                text = "$speakerScore٪",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${checkedTargetIds.size} نفر تارگت شده",
                                        fontSize = 11.sp,
                                        color = if (checkedTargetIds.isEmpty()) TextMutedDark else MafiaCrimsonLight
                                    )
                                }
                            }

                            // Quick actions: Note button for speaker + Clear All
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // 📝 Quick Note for speaker
                                IconButton(
                                    onClick = { noteTargetPlayer = selectedPlayer },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "یادداشت گوینده",
                                        tint = MafiaGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // "پاک کردن همه" (Clear all)
                                Button(
                                    onClick = { updateTargets(emptyList(), false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MafiaSurfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("btn_clear_all_targets")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = TextSecondaryDark
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "پاک کردن همه",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }

                        // Display speaker's active behavioral tags
                        if (speakerNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(speakerNotes.take(5)) { note ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MafiaGold.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, MafiaGold.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = note.text,
                                            fontSize = 9.sp,
                                            color = MafiaGold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Compact FlowRow with all candidate players
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            candidateTargets.forEach { candidate ->
                                val isChecked = checkedTargetIds.contains(candidate.id)
                                val candidateSeat = players.indexOfFirst { it.id == candidate.id } + 1
                                val candidateScore = scoreMap[candidate.id]?.totalScore ?: 50
                                val candidateNotes = notesByPlayer[candidate.id] ?: emptyList()
                                val scoreAnalysis = scoreMap[candidate.id]
                                val hasFlip = (scoreAnalysis?.targetFlipsCount ?: 0) > 0

                                val chipBgColor by animateColorAsState(
                                    targetValue = if (isChecked) MafiaCrimsonDark else MafiaSurfaceVariant,
                                    animationSpec = tween(durationMillis = 100),
                                    label = "chipBgColor"
                                )
                                val chipBorderColor by animateColorAsState(
                                    targetValue = if (isChecked) MafiaCrimsonLight else MafiaBorder,
                                    animationSpec = tween(durationMillis = 100),
                                    label = "chipBorderColor"
                                )

                                Surface(
                                    selected = isChecked,
                                    onClick = {
                                        val newSelection = if (isChecked) {
                                            checkedTargetIds.filter { it != candidate.id }
                                        } else {
                                            checkedTargetIds + candidate.id
                                        }
                                        updateTargets(newSelection, false)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = chipBgColor,
                                    border = BorderStroke(
                                        width = if (isChecked) 1.5.dp else 1.dp,
                                        color = chipBorderColor
                                    ),
                                    modifier = Modifier
                                        .testTag("target_checkbox_${candidate.id}")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Seat indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isChecked) MafiaCrimson else MafiaCardBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "انتخاب شده",
                                                        tint = TextPrimaryDark,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "$candidateSeat",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = TextSecondaryDark
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Name
                                            Text(
                                                text = candidate.name,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isChecked) TextPrimaryDark else TextSecondaryDark
                                            )

                                            // Suspicion bar & percentage
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = when {
                                                    candidateScore >= 70 -> Color(0xFFB71C1C)
                                                    candidateScore >= 55 -> Color(0xFFE65100)
                                                    else -> Color(0xFF1B5E20)
                                                }
                                            ) {
                                                Text(
                                                    text = "$candidateScore٪",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }

                                            // Quick 📝 Note button for candidate
                                            IconButton(
                                                onClick = { noteTargetPlayer = candidate },
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .padding(start = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EditNote,
                                                    contentDescription = "یادداشت",
                                                    tint = MafiaGold,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            if (isChecked) {
                                                Text(" 🎯", fontSize = 11.sp)
                                            }

                                            if (hasFlip) {
                                                Text(" ⚡", fontSize = 10.sp)
                                            }
                                        }

                                        // Render small behavioral tag preview if any exist
                                        if (candidateNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                candidateNotes.take(2).forEach { note ->
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),
                                                        color = MafiaGold.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = note.text.take(12),
                                                            fontSize = 8.sp,
                                                            color = MafiaGold,
                                                            modifier = Modifier.padding(horizontal = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom status bar: Auto-save message
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MafiaCardElevated, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MafiaGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ذخیره خودکار آنی (Auto-Save) فعال است",
                                    fontSize = 10.sp,
                                    color = MafiaGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Manual save button as backup
                            TextButton(
                                onClick = {
                                    updateTargets(checkedTargetIds.toList(), true)
                                    if (autoAdvanceEnabled) moveToNextLivingPlayer()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(24.dp)
                                    .testTag("btn_save_targets")
                            ) {
                                Text(
                                    text = "ثبت و بعدی ◀",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MafiaCrimsonLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET 1: یادداشت و مشاهده سریع (Quick Notes & Tags) ---
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
                            text = "یادداشت صندلی $seatNum (${player.name})",
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
                    text = "تگ‌های سریع رفتار:",
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
                        colors = ButtonDefaults.buttonColors(containerColor = MafiaCrimsonDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("ثبت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SECRET NOTE SECTION (Only visible if Stealth Mafia Mode is active)
                if (isStealthMafiaMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF241633),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🔒 یادداشت محرمانه (کاملاً مخفی):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCE93D8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = secretText,
                                    onValueChange = { secretText = it },
                                    placeholder = { Text("نکته کاملاً خصوصی برای سوءاستفاده...", fontSize = 11.sp, color = TextMutedDark) },
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

                // Existing Notes for this player
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

    // --- BOTTOM SHEET 2: مشکوک‌ترین‌ها الان (Most Suspicious Right Now) ---
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

                topSuspicious.forEachIndexed { rank, item ->
                    val seat = players.indexOfFirst { it.id == item.playerId } + 1
                    val topReason = item.positiveFactors.maxByOrNull { it.points }?.title ?: item.statusLabel

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MafiaCardElevated,
                        border = BorderStroke(1.dp, if (rank == 0) MafiaCrimsonLight else MafiaBorder),
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
                                Text(
                                    text = "#${rank + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (rank == 0) MafiaCrimsonLight else TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "صندلی $seat: ${item.playerName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "دلیل: $topReason",
                                        fontSize = 11.sp,
                                        color = MafiaGold
                                    )
                                }
                            }

                            // Score badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    item.totalScore >= 70 -> Color(0xFFB71C1C)
                                    item.totalScore >= 55 -> Color(0xFFE65100)
                                    else -> Color(0xFF1B5E20)
                                }
                            ) {
                                Text(
                                    text = "${item.totalScore}٪",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 3: اضطراری «جا موندم» (Missed Players Sheet) ---
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
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بازیکن‌های بدون تارگت در ${currentStage.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryDark
                        )
                    }
                    IconButton(onClick = { showMissedPlayersSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (missedPlayers.isEmpty()) {
                    Text(
                        text = "عالی! تمام بازیکنان حاضر تارگت‌های خود را ثبت کرده‌اند.",
                        fontSize = 12.sp,
                        color = MafiaGreen,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = "با لمس هر بازیکن، نوبت فوراً به او منتقل می‌شود تا تارگت‌هایش را ثبت کنید:",
                        fontSize = 11.sp,
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
                            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    text = "ثبت سریع ◀",
                                    fontSize = 11.sp,
                                    color = MafiaCrimsonLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- BOTTOM SHEET 4: تحلیل اصطکاک و ترفندهای گمراه کردن (Stealth Mode Sheet) ---
    if (showStealthAnalysisSheet && isStealthMafiaMode) {
        ModalBottomSheet(
            onDismissRequest = { showStealthAnalysisSheet = false },
            containerColor = Color(0xFF1E122A),
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
                        text = "اطلاعات اصطکاک و شکاف شهروندان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFE1BEE7)
                    )
                    IconButton(onClick = { showStealthAnalysisSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "۱. عمیق‌ترین درگیری‌های دور میز (فرصت سوءاستفاده):",
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

    // Confirmation dialog for manually closing the stage
    if (showCloseStageDialog) {
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
}
