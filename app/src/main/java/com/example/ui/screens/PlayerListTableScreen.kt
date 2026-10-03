package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TargetEntity
import com.example.ui.theme.*
import com.example.ui.util.PlayerDisplay

/**
 * Linear 4-per-row grid of active players with seat numbers and
 * tap-to-open-the-speaker-targeting-flow.
 *
 * For each player, a small subtitle below the seat number shows who
 * they have already targeted in the current stage, so the user can
 * confirm progress when returning to the table.
 *
 * Eliminated players appear in a dimmed grid below.
 */
@Composable
fun PlayerListTableScreen(
    players: List<PlayerEntity>,
    targets: List<TargetEntity>,
    activeSpeakersCount: Int,
    activeGame: GameEntity? = null,
    onStartTargeting: () -> Unit,
    onPlayerTap: (PlayerEntity) -> Unit = {},
    onSwapPlayers: (Long, Long) -> Unit = { _, _ -> }
) {
    var isEditMode by remember { mutableStateOf(false) }
    var dragSourceId by remember { mutableStateOf<Long?>(null) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            // Header summary
            Surface(
                color = MafiaCardBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            text = "$activeSpeakersCount بازیکن فعال • ${targets.size} تارگت ثبت شده",
                            color = TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                    // Toggle seat-edit mode (drag & drop)
                    OutlinedButton(
                        onClick = { isEditMode = !isEditMode },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isEditMode) MafiaCrimson else Color.Transparent,
                            contentColor = if (isEditMode) Color.White else MafiaGold
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MafiaGold),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(if (isEditMode) "پایان چیدمان" else "چیدمان صندلی", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onStartTargeting,
                        enabled = activeSpeakersCount > 0 && !isEditMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MafiaCrimson,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("شروع تارگت‌زنی", fontSize = 13.sp)
                    }
                }
            }

            // Active players
            val active = players.filter { !it.isEliminated }
            val eliminated = players.filter { it.isEliminated }

            if (active.isEmpty() && eliminated.isEmpty()) {
                EmptyPlayersHint()
            } else {
                if (active.isNotEmpty()) {
                    Text(
                        text = if (isEditMode)
                            "یک نفر را بکش و روی نفر دیگر رها کن تا جا عوض کنند"
                        else
                            "بازیکنان • روی هر کس بزنید تا سخنران شروع شود",
                        color = if (isEditMode) MafiaCrimsonLight else TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    ActivePlayerGrid(
                        players = active,
                        targets = targets,
                        activeGame = activeGame,
                        onPlayerTap = onPlayerTap,
                        isEditMode = isEditMode,
                        dragSourceId = dragSourceId,
                        onDragStart = { id -> dragSourceId = id },
                        onDragEnd = { targetId ->
                            val src = dragSourceId
                            if (src != null && src != targetId) {
                                onSwapPlayers(src, targetId)
                            }
                            dragSourceId = null
                        }
                    )
                }

                if (eliminated.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "حذف‌شده‌ها",
                        color = TextMutedDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    EliminatedPlayerGrid(players = eliminated)
                }
            }
        }
    }
}

@Composable
private fun ActivePlayerGrid(
    players: List<PlayerEntity>,
    targets: List<TargetEntity>,
    activeGame: GameEntity?,
    onPlayerTap: (PlayerEntity) -> Unit,
    isEditMode: Boolean,
    dragSourceId: Long?,
    onDragStart: (Long) -> Unit,
    onDragEnd: (Long) -> Unit
) {
    // 4 per row. For each player, look up who they targeted in the
    // current stage and show a one-line summary.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { p ->
                    ActivePlayerTile(
                        player = p,
                        seatNumber = players.indexOf(p) + 1,
                        targetNames = remember(targets, p.id, players) {
                            targets
                                .filter { it.sourcePlayerId == p.id }
                                .mapNotNull { t ->
                                    val tp = players.firstOrNull { it.id == t.targetPlayerId }
                                    tp?.let { PlayerDisplay.forPlayer(it, activeGame).displayName }
                                }
                                .take(3)
                        },
                        totalTargets = remember(targets, p.id) {
                            targets.count { it.sourcePlayerId == p.id }
                        },
                        onClick = { onPlayerTap(p) },
                        activeGame = activeGame,
                        modifier = Modifier.weight(1f),
                        isEditMode = isEditMode,
                        isDragSource = dragSourceId == p.id,
                        onDragStart = { onDragStart(p.id) },
                        onDragEnd = { onDragEnd(p.id) }
                    )
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActivePlayerTile(
    player: PlayerEntity,
    seatNumber: Int,
    targetNames: List<String>,
    totalTargets: Int,
    onClick: () -> Unit,
    activeGame: GameEntity?,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    isDragSource: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val display = remember(player.id, activeGame?.id, activeGame?.ownerRole) {
        PlayerDisplay.forPlayer(player, activeGame)
    }
    val isOwner = display.isOwner
    val borderColor = when {
        isDragSource -> MafiaCrimson
        display.isHidden -> MafiaCrimsonLight
        isOwner -> MafiaGold
        else -> MafiaBorder
    }
    val tileModifier = if (isEditMode) {
        modifier
            .padding(vertical = 2.dp)
            .pointerInput(player.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { _, _ -> }
                )
            }
    } else {
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    }
    Surface(
        color = MafiaCardBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = tileModifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown badge for owner (no role words in the UI)
            if (isOwner) {
                Text(
                    text = "👑",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(2.dp))
            }
            // Seat number on top
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (display.isHidden) MafiaCrimson else MafiaGold),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seatNumber.toString(),
                    color = if (display.isHidden) Color.White else Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            // Player name (big and clear) — hidden name shown as code
            Text(
                text = display.displayName,
                color = if (display.isHidden) MafiaCrimsonLight else TextPrimaryDark,
                fontWeight = if (display.isHidden) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (display.isHidden) 12.sp else 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            // Target summary
            if (totalTargets == 0) {
                Text(
                    text = "بدون تارگت",
                    color = TextMutedDark,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val summary = if (targetNames.size < totalTargets) {
                    targetNames.joinToString("، ") + " +${totalTargets - targetNames.size}"
                } else {
                    targetNames.joinToString("، ")
                }
                Text(
                    text = "→ $summary",
                    color = MafiaCrimsonLight,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EliminatedPlayerGrid(players: List<PlayerEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { p ->
                    EliminatedPlayerTile(player = p, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EliminatedPlayerTile(
    player: PlayerEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MafiaCardBg.copy(alpha = 0.4f))
                .border(1.dp, MafiaBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(1),
                color = TextMutedDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = player.name,
            color = TextMutedDark,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyPlayersHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
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
}
