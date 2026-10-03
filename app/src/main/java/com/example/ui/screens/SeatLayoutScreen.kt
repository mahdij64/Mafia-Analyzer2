package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerEntity
import com.example.ui.theme.*

/**
 * Visual seat-layout editor. Lists active players in seat order with
 * controls to move a player up/down by one seat or swap two seats.
 *
 * The order of the list IS the seating order: index 0 = seat 1, etc.
 * Changes are persisted via MafiaViewModel.reorderPlayers / swapPlayers.
 */
@Composable
fun SeatLayoutScreen(
    players: List<PlayerEntity>,
    onSwap: (Long, Long) -> Unit,
    onMove: (PlayerEntity, Boolean) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var pendingSwapFrom by remember { mutableStateOf<Long?>(null }

        val active = players.filter { !it.isEliminated }
        val eliminated = players.filter { it.isEliminated }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MafiaDarkBg)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "چیدمان صندلی",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ترتیب نمایش در میز گرد برابر همین ترتیب است. برای جابجایی از فلش‌ها و برای تعویض با بازیکن دیگر از دکمه «عوض کن» استفاده کنید.",
                            color = TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "صندلی‌های فعال (${active.size})",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            itemsIndexed(active, key = { _, p -> p.id }) { index, player ->
                SeatRow(
                    player = player,
                    seatNumber = index + 1,
                    isFirst = index == 0,
                    isLast = index == active.lastIndex,
                    isSwapSelected = pendingSwapFrom == player.id,
                    onMoveUp = { onMove(player, true) },
                    onMoveDown = { onMove(player, false) },
                    onSwapClick = {
                        val current = pendingSwapFrom
                        if (current == null) {
                            pendingSwapFrom = player.id
                        } else if (current == player.id) {
                            pendingSwapFrom = null
                        } else {
                            onSwap(current, player.id)
                            pendingSwapFrom = null
                        }
                    }
                )
            }

            if (eliminated.isNotEmpty()) {
                item {
                    Text(
                        text = "حذف‌شده‌ها (${eliminated.size})",
                        color = TextMutedDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                itemsIndexed(eliminated, key = { _, p -> p.id }) { _, player ->
                    SeatRow(
                        player = player,
                        seatNumber = 0,
                        isFirst = true,
                        isLast = true,
                        isSwapSelected = false,
                        onMoveUp = {},
                        onMoveDown = {},
                        onSwapClick = {},
                        dimmed = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatRow(
    player: PlayerEntity,
    seatNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isSwapSelected: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSwapClick: () -> Unit,
    dimmed: Boolean = false
) {
    val border = if (isSwapSelected) MafiaCrimsonLight else MafiaBorder
    Surface(
        color = if (dimmed) MafiaCardBg.copy(alpha = 0.4f) else MafiaCardBg,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSwapSelected) 1.5.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth()
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSwapSelected) MafiaCrimson else MafiaGold.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (seatNumber > 0) seatNumber.toString() else "—",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            // Player name
            Text(
                text = player.name,
                color = if (dimmed) TextMutedDark else TextPrimaryDark,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            // Move up
            IconButton(
                onClick = onMoveUp,
                enabled = !dimmed && !isFirst
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "بالا",
                    tint = if (!dimmed && !isFirst) MafiaGold else TextMutedDark.copy(alpha = 0.3f)
                )
            }
            // Move down
            IconButton(
                onClick = onMoveDown,
                enabled = !dimmed && !isLast
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = "پایین",
                    tint = if (!dimmed && !isLast) MafiaGold else TextMutedDark.copy(alpha = 0.3f)
                )
            }
            // Swap
            IconButton(
                onClick = onSwapClick,
                enabled = !dimmed
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "عوض کن",
                    tint = if (isSwapSelected) MafiaCrimsonLight else if (dimmed) TextMutedDark.copy(alpha = 0.3f) else TextPrimaryDark
                )
            }
        }
    }
}
