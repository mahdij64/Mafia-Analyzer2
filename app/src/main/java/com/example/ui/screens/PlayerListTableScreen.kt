package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun PlayerListTableScreen(
    players: List<PlayerEntity>,
    activeSpeakersCount: Int,
    activeGame: GameEntity? = null,
    currentStageIndex: Int = 0,
    completedSpeakerIds: Set<Long> = emptySet(),
    targets: List<com.example.data.local.TargetEntity> = emptyList(),
    onStartTargeting: () -> Unit,
    onPlayerTap: (PlayerEntity) -> Unit = {},
    onSwapPlayers: (Long, Long) -> Unit = { _, _ -> },
    onMovePlayer: (PlayerEntity, Boolean) -> Unit = { _, _ -> },
    onTogglePlayerEliminated: (PlayerEntity) -> Unit = {},
    onEndDay: () -> Unit = {}
) {
    var isEditMode by remember { mutableStateOf(false) }
    var isNightKillMode by remember { mutableStateOf(false) }
    var showEndDayReportDialog by remember { mutableStateOf(false) }

    // ---- Drag & drop state ----
    var draggedPlayerId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetId by remember { mutableStateOf<Long?>(null) }
    val playerPositions = remember { mutableStateMapOf<Long, Pair<Float, Float>>() }

    val active = remember(players) { players.filter { !it.isEliminated } }
    val eliminated = remember(players) { players.filter { it.isEliminated } }
    val showNightKillButton = currentStageIndex >= 1  // Day 1 onwards (index 1+)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MafiaDarkBg)
        ) {
            // ---- Header card ----
            HeaderCard(
                activeSpeakersCount = activeSpeakersCount,
                isEditMode = isEditMode,
                isNightKillMode = isNightKillMode,
                showNightKillButton = showNightKillButton,
                showEndDayButton = !isEditMode && !isNightKillMode && active.isNotEmpty() &&
                    active.all { it.id in completedSpeakerIds },
                onToggleEdit = {
                    isEditMode = !isEditMode
                    isNightKillMode = false
                    if (!isEditMode) {
                        draggedPlayerId = null
                        dropTargetId = null
                        dragOffset = Offset.Zero
                    }
                },
                onToggleNightKill = {
                    isNightKillMode = !isNightKillMode
                    isEditMode = false
                },
                onStartTargeting = onStartTargeting,
                onShowEndDayReport = { showEndDayReportDialog = true },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // ---- Edit mode hint ----
            if (isEditMode) {
                Surface(
                    color = MafiaCrimson.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔀 یک بازیکن را بکشید و روی دیگری رها کنید تا جایشان عوض شود",
                        color = MafiaCrimsonLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // ---- Night kill mode hint ----
            if (isNightKillMode) {
                Surface(
                    color = Color(0xFF1A237E).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🌙 روی بازیکنی که کشته شب شده بزنید تا حذف شود",
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (active.isEmpty() && eliminated.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPlayersHint()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (active.isNotEmpty()) {
                            Text(
                                text = when {
                                    isEditMode -> "🔀 چیدمان صندلی"
                                    isNightKillMode -> "🌙 کشته‌های شب"
                                    else -> "بازیکنان فعال (${active.size})"
                                },
                                color = if (isEditMode) MafiaCrimsonLight
                                        else if (isNightKillMode) Color(0xFF90CAF9)
                                        else TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            active.chunked(4).forEach { rowPlayers ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowPlayers.forEach { player ->
                                        val seatNumber = active.indexOf(player) + 1
                                        val isDragged = draggedPlayerId == player.id
                                        val isTarget = dropTargetId == player.id

                                        DraggablePlayerCircle(
                                            player = player,
                                            seatNumber = seatNumber,
                                            activeGame = activeGame,
                                            isEditMode = isEditMode,
                                            isNightKillMode = isNightKillMode,
                                            isCompleted = player.id in completedSpeakerIds,
                                            isDragged = isDragged,
                                            isDropTarget = isTarget,
                                            dragOffset = if (isDragged) dragOffset else Offset.Zero,
                                            onDragStart = {
                                                draggedPlayerId = player.id
                                                dragOffset = Offset.Zero
                                                dropTargetId = null
                                            },
                                            onDrag = { change, offset ->
                                                change.consume()
                                                dragOffset = dragOffset + offset
                                                val myPos = playerPositions[player.id]
                                                if (myPos != null) {
                                                    val absX = myPos.first + dragOffset.x
                                                    val absY = myPos.second + dragOffset.y
                                                    val closest = playerPositions.entries
                                                        .filter { it.key != player.id }
                                                        .minByOrNull { entry ->
                                                            val dx = entry.value.first - absX
                                                            val dy = entry.value.second - absY
                                                            dx * dx + dy * dy
                                                        }
                                                    if (closest != null) {
                                                        val dx = closest.value.first - absX
                                                        val dy = closest.value.second - absY
                                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                        dropTargetId = if (dist < 120f) closest.key else null
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                val fromId = draggedPlayerId
                                                val toId = dropTargetId
                                                if (fromId != null && toId != null && fromId != toId) {
                                                    onSwapPlayers(fromId, toId)
                                                }
                                                draggedPlayerId = null
                                                dragOffset = Offset.Zero
                                                dropTargetId = null
                                            },
                                            onDragCancel = {
                                                draggedPlayerId = null
                                                dragOffset = Offset.Zero
                                                dropTargetId = null
                                            },
                                            onTap = {
                                                when {
                                                    isNightKillMode -> onTogglePlayerEliminated(player)
                                                    !isEditMode -> onPlayerTap(player)
                                                }
                                            },
                                            onRegisterPosition = { x, y ->
                                                playerPositions[player.id] = Pair(x, y)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(4 - rowPlayers.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (eliminated.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MafiaBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                text = "💀 حذف‌شده‌ها (${eliminated.size})",
                                color = TextMutedDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )

                            eliminated.chunked(4).forEach { rowPlayers ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowPlayers.forEach { player ->
                                        EliminatedCircle(
                                            player = player,
                                            isNightKillMode = isNightKillMode,
                                            onTap = {
                                                if (isNightKillMode) {
                                                    onTogglePlayerEliminated(player)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(4 - rowPlayers.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ====================================================================
// Draggable player circle — seat number only, name below
// ====================================================================

@Composable
private fun DraggablePlayerCircle(
    player: PlayerEntity,
    seatNumber: Int,
    activeGame: GameEntity?,
    isEditMode: Boolean,
    isNightKillMode: Boolean,
    isCompleted: Boolean,
    isDragged: Boolean,
    isDropTarget: Boolean,
    dragOffset: Offset,
    onDragStart: () -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onTap: () -> Unit,
    onRegisterPosition: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOwner = activeGame?.ownerPlayerId == player.id

    val animatedScale by animateFloatAsState(
        targetValue = when {
            isDragged -> 1.2f
            isDropTarget -> 1.12f
            else -> 1f
        },
        animationSpec = tween(150),
        label = "scale"
    )

    val circleBg = when {
        isDragged -> MafiaCrimson.copy(alpha = 0.9f)
        isDropTarget -> MafiaCrimsonDark.copy(alpha = 0.7f)
        isNightKillMode -> Color(0xFF1A237E).copy(alpha = 0.4f)
        isOwner -> MafiaGold.copy(alpha = 0.15f)
        else -> MafiaCardBg
    }

    val circleBorder = when {
        isDragged -> MafiaGold
        isDropTarget -> MafiaCrimsonLight
        isNightKillMode -> Color(0xFF5C6BC0)
        isOwner -> MafiaGold
        else -> MafiaBorder
    }

    val borderWidth = when {
        isDragged || isDropTarget -> 2.5.dp
        else -> 1.5.dp
    }

    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    onRegisterPosition(
                        pos.x + coords.size.width / 2f,
                        pos.y + coords.size.height / 2f
                    )
                }
                .absoluteOffset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                .zIndex(if (isDragged) 10f else 0f)
                .scale(animatedScale)
                .size(56.dp)
                .shadow(
                    elevation = if (isDragged) 16.dp else if (isDropTarget) 8.dp else 2.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(circleBg)
                .border(borderWidth, circleBorder, CircleShape)
                .then(
                    if (isEditMode) {
                        Modifier.pointerInput(player.id, isEditMode) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDrag = { change, offset -> onDrag(change, offset) },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragCancel() }
                            )
                        }
                    } else {
                        Modifier.clickable(onClick = onTap)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Only seat number inside the circle — no initials
            Text(
                text = "$seatNumber",
                color = when {
                    isDragged -> Color.White
                    isNightKillMode -> Color(0xFF90CAF9)
                    isOwner -> MafiaGold
                    else -> MafiaGold.copy(alpha = 0.9f)
                },
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // Owner crown
        if (isOwner) {
            Text(text = "👑", fontSize = 10.sp, lineHeight = 12.sp)
        }

        // Completed targeting checkmark
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "تارگت‌زنی تکمیل شده",
                tint = MafiaGreen,
                modifier = Modifier.size(14.dp)
            )
        }

        // Full name below circle — always visible, auto-size
        Text(
            text = player.name,
            color = when {
                isNightKillMode -> Color(0xFF90CAF9)
                isOwner -> MafiaGold
                else -> TextPrimaryDark
            },
            fontSize = 11.sp,
            fontWeight = if (isOwner) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )

        if (isDropTarget) {
            Text(
                text = "⇅ جابجایی",
                color = MafiaCrimsonLight,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ====================================================================
// Eliminated player circle
// ====================================================================

@Composable
private fun EliminatedCircle(
    player: PlayerEntity,
    isNightKillMode: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isNightKillMode) Color(0xFF1A237E).copy(alpha = 0.3f)
                    else MafiaCardBg.copy(alpha = 0.5f)
                )
                .border(
                    1.dp,
                    if (isNightKillMode) Color(0xFF5C6BC0).copy(alpha = 0.5f)
                    else MafiaBorder.copy(alpha = 0.5f),
                    CircleShape
                )
                .then(
                    if (isNightKillMode) Modifier.clickable(onClick = onTap)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💀", fontSize = 16.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = player.name,
            color = TextMutedDark,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp)
        )
    }
}

// ====================================================================
// Header card — with night kill button (day 2+)
// ====================================================================

@Composable
private fun HeaderCard(
    activeSpeakersCount: Int,
    isEditMode: Boolean,
    isNightKillMode: Boolean,
    showNightKillButton: Boolean,
    showEndDayButton: Boolean,
    onToggleEdit: () -> Unit,
    onToggleNightKill: () -> Unit,
    onStartTargeting: () -> Unit,
    onShowEndDayReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MafiaCardBg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "میز بازی",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$activeSpeakersCount بازیکن فعال",
                        color = TextMutedDark,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onStartTargeting,
                    enabled = activeSpeakersCount > 0 && !isEditMode && !isNightKillMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MafiaCrimson,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("شروع تارگت‌زنی", fontSize = 12.sp)
                }
            }

            // Second row: edit + night kill buttons
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleEdit,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isEditMode) MafiaCrimson else Color.Transparent,
                        contentColor = if (isEditMode) Color.White else MafiaGold
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MafiaGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isEditMode) "پایان چیدمان" else "چیدمان صندلی",
                        fontSize = 11.sp
                    )
                }

                if (showNightKillButton) {
                    OutlinedButton(
                        onClick = onToggleNightKill,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isNightKillMode) Color(0xFF1A237E) else Color.Transparent,
                            contentColor = if (isNightKillMode) Color.White else Color(0xFF90CAF9)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C6BC0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.NightsStay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isNightKillMode) "پایان کشته شب" else "کشته‌های شب",
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Third row: end day button (shown when all speakers completed)
            if (showEndDayButton) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onShowEndDayReport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuspicionGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "✅ پایان روز — رفتن به مرحله بعد",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // End Day Report Dialog
        if (showEndDayReportDialog) {
            EndDayReportDialog(
                players = players,
                targets = targets,
                currentStageIndex = currentStageIndex,
                onConfirm = {
                    showEndDayReportDialog = false
                    onEndDay()
                },
                onDismiss = { showEndDayReportDialog = false }
            )
        }
    }
}

@Composable
private fun EndDayReportDialog(
    players: List<PlayerEntity>,
    targets: List<com.example.data.local.TargetEntity>,
    currentStageIndex: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentStageTargets = targets.filter { it.stageIndex == currentStageIndex }
    val targetsReceivedCount = players.associate { p ->
        p.id to currentStageTargets.count { it.targetPlayerId == p.id }
    }
    
    val mostTargeted = targetsReceivedCount.maxByOrNull { it.value }
    val leastTargeted = targetsReceivedCount.filter { it.value > 0 }.minByOrNull { it.value }
    val zeroTargets = targetsReceivedCount.filter { it.value == 0 }.keys
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MafiaCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MafiaGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "📊 گزارش پایان روز",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MafiaGold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "خلاصه تارگت‌های امروز:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Most targeted
                if (mostTargeted != null && mostTargeted.value > 0) {
                    val player = players.firstOrNull { it.id == mostTargeted.key }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MafiaCrimsonDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MafiaCrimson, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "🎯 بیشترین تارگت",
                                fontSize = 12.sp,
                                color = MafiaCrimsonLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${player?.name ?: "?"} (${mostTargeted.value} تارگت)",
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                // Least targeted
                if (leastTargeted != null) {
                    val player = players.firstOrNull { it.id == leastTargeted.key }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(SuspicionGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SuspicionGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "🛡️ کمترین تارگت",
                                fontSize = 12.sp,
                                color = SuspicionGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${player?.name ?: "?"} (${leastTargeted.value} تارگت)",
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                // Zero targets
                if (zeroTargets.isNotEmpty()) {
                    val zeroNames = zeroTargets.mapNotNull { id -> players.firstOrNull { it.id == id }?.name }.joinToString("، ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "👻 بدون تارگت",
                                fontSize = 12.sp,
                                color = Color(0xFFFF6F00),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                zeroNames,
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text(
                    "آیا می‌خواهید به روز بعد بروید؟",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SuspicionGreen, contentColor = Color.White)
            ) {
                Text("بله، برو به روز بعد", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondaryDark)
            }
        }
    )
}

// ====================================================================
// Empty state
// ====================================================================

@Composable
private fun EmptyPlayersHint() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = TextMutedDark,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "بازیکنی ثبت نشده است",
            color = TextMutedDark,
            fontSize = 14.sp
        )
        Text(
            text = "از تنظیمات → مدیریت بازی‌ها اضافه کنید",
            color = TextMutedDark,
            fontSize = 12.sp
        )
    }
}
