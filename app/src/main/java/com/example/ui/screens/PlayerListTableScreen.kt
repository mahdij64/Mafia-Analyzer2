package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.GameEntity
import com.example.data.local.PlayerEntity
import com.example.ui.theme.*
import com.example.ui.util.PlayerDisplay

/**
 * Linear scrollable list of active players with seat numbers and
 * tap-to-open-the-speaker-targeting-flow. Owner of a non-citizen
 * game sees their own name replaced with a stable numeric code
 * (e.g. "کد ۰۱").
 *
 * Includes an inline seat-edit mode that exposes per-player up/down
 * arrows and a one-tap swap selector. Drag-and-drop is intentionally
 * avoided because it was unreliable on some devices.
 */
@Composable
fun PlayerListTableScreen(
    players: List<PlayerEntity>,
    activeSpeakersCount: Int,
    activeGame: GameEntity? = null,
    onStartTargeting: () -> Unit,
    onPlayerTap: (PlayerEntity) -> Unit = {},
    onSwapPlayers: (Long, Long) -> Unit = { _, _ -> },
    onMovePlayer: (PlayerEntity, Boolean) -> Unit = { _, _ -> }
) {
    var isEditMode by remember { mutableStateOf(false) }
    var pendingSwapFromId by remember { mutableStateOf<Long?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MafiaDarkBg)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                HeaderCard(
                    activeSpeakersCount = activeSpeakersCount,
                    isEditMode = isEditMode,
                    onToggleEdit = { isEditMode = !isEditMode },
                    onStartTargeting = onStartTargeting
                )
            }

            val active = players.filter { !it.isEliminated }
            val eliminated = players.filter { it.isEliminated }

            if (active.isEmpty() && eliminated.isEmpty()) {
                item { EmptyPlayersHint() }
            } else {
                if (active.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isEditMode) {
                                if (pendingSwapFromId == null)
                                    "برای عوض کردن: یک نفر را انتخاب کن، سپس نفر دوم را بزنید"
                                else
                                    "حالا نفر دوم را بزنید تا جا عوض کنید"
                            } else {
                                "بازیکنان • روی هر کس بزنید تا سخنران شروع شود"
                            },
                            color = if (isEditMode) MafiaCrimsonLight else TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    itemsIndexed(active, key = { _, p -> p.id }) { index, player ->
                        ActivePlayerRow(
                            player = player,
                            seatNumber = index + 1,
                            isFirst = index == 0,
                            isLast = index == active.lastIndex,
                            isEditMode = isEditMode,
                            isSwapSelected = pendingSwapFromId == player.id,
                            activeGame = activeGame,
                            onTap = {
                                if (isEditMode) {
                                    val current = pendingSwapFromId
                                    if (current == null) {
                                        pendingSwapFromId = player.id
                                    } else if (current == player.id) {
                                        pendingSwapFromId = null
                                    } else {
                                        onSwapPlayers(current, player.id)
                                        pendingSwapFromId = null
                                    }
                                } else {
                                    onPlayerTap(player)
                                }
                            },
                            onMoveUp = { onMovePlayer(player, true) },
                            onMoveDown = { onMovePlayer(player, false) }
                        )
                    }
                }

                if (eliminated.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "حذف‌شده‌ها",
                            color = TextMutedDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    itemsIndexed(eliminated, key = { _, p -> p.id }) { _, player ->
                        EliminatedPlayerRow(player = player)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    activeSpeakersCount: Int,
    isEditMode: Boolean,
    onToggleEdit: () -> Unit,
    onStartTargeting: () -> Unit
) {
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
                    text = "$activeSpeakersCount بازیکن فعال",
                    color = TextMutedDark,
                    fontSize = 12.sp
                )
            }
            OutlinedButton(
                onClick = onToggleEdit,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isEditMode) MafiaCrimson else Color.Transparent,
                    contentColor = if (isEditMode) Color.White else MafiaGold
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MafiaGold),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = if (isEditMode) "پایان چیدمان" else "چیدمان صندلی",
                    fontSize = 12.sp
                )
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
                Text("شروع تارگت‌زنی", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ActivePlayerRow(
    player: PlayerEntity,
    seatNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isEditMode: Boolean,
    isSwapSelected: Boolean,
    activeGame: GameEntity?,
    onTap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val display = remember(player.id, activeGame?.id, activeGame?.ownerRole) {
        PlayerDisplay.forPlayer(player, activeGame)
    }
    val borderColor = when {
        isSwapSelected -> MafiaCrimson
        display.isHidden -> MafiaCrimsonLight
        display.isOwner -> MafiaGold
        else -> MafiaBorder
    }
    Surface(
        color = MafiaCardBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSwapSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seat number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSwapSelected -> MafiaCrimson
                            display.isHidden -> MafiaCrimson
                            else -> MafiaGold.copy(alpha = 0.85f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seatNumber.toString(),
                    color = if (isSwapSelected || display.isHidden) Color.White else Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            // Player name with owner crown
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (display.isOwner) {
                    Text(text = "👑", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = display.displayName,
                    color = if (display.isHidden) MafiaCrimsonLight else TextPrimaryDark,
                    fontWeight = if (display.isOwner) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
            // Edit-mode controls
            if (isEditMode) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "بالا",
                        tint = if (!isFirst) MafiaGold else TextMutedDark.copy(alpha = 0.3f)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "پایین",
                        tint = if (!isLast) MafiaGold else TextMutedDark.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EliminatedPlayerRow(player: PlayerEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MafiaCardBg.copy(alpha = 0.4f))
                .border(1.dp, MafiaBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(1),
                color = TextMutedDark,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = player.name,
            color = TextMutedDark,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyPlayersHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
