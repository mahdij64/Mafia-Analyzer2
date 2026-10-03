package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.data.local.PlayerEntity
import com.example.data.local.TargetEntity
import com.example.ui.theme.*

/**
 * Simplified "table" view: shows the active players in a 4-per-row circle
 * grid plus a clear CTA to start the speaker-targeting flow. The legacy
 * drag-and-drop CircularTableTargetScreen has been removed in favor of the
 * one-speaker-at-a-time SpeakerTargetScreen flow.
 */
@Composable
fun PlayerListTableScreen(
    players: List<PlayerEntity>,
    targets: List<TargetEntity>,
    activeSpeakersCount: Int,
    onStartTargeting: () -> Unit
) {
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
                    Button(
                        onClick = onStartTargeting,
                        enabled = activeSpeakersCount > 0,
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

            // Active players grid (4 per row)
            val active = players.filter { !it.isEliminated }
            val eliminated = players.filter { it.isEliminated }

            if (active.isEmpty() && eliminated.isEmpty()) {
                EmptyPlayersHint()
            } else {
                Text(
                    text = "بازیکنان فعال",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                PlayerGrid(active)

                if (eliminated.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "حذف‌شده‌ها",
                        color = TextMutedDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    PlayerGrid(eliminated, dimmed = true)
                }
            }
        }
    }
}

@Composable
private fun PlayerGrid(players: List<PlayerEntity>, dimmed: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { p ->
                    PlayerChip(player = p, dimmed = dimmed, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PlayerChip(
    player: PlayerEntity,
    dimmed: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (dimmed) MafiaCardBg.copy(alpha = 0.4f) else MafiaCrimson.copy(alpha = 0.85f))
                .border(1.dp, if (dimmed) MafiaBorder else MafiaCrimsonLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(2),
                color = if (dimmed) TextMutedDark else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = player.name,
            color = if (dimmed) TextMutedDark else TextPrimaryDark,
            fontSize = 11.sp,
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
