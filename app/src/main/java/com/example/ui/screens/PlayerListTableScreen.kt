package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Round table view of the active players with numbered seats and
 * tap-to-open-the-speaker-targeting-flow.
 *
 * Layout: a single big circle in the middle, with player circles
 * evenly distributed around its perimeter. Each player circle shows
 * their seat number (1-based) and name. Eliminated players appear
 * dimmed below the table.
 */
@Composable
fun PlayerListTableScreen(
    players: List<PlayerEntity>,
    targets: List<TargetEntity>,
    activeSpeakersCount: Int,
    onStartTargeting: () -> Unit,
    onPlayerTap: (PlayerEntity) -> Unit = {}
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

            // Active players around the table
            val active = players.filter { !it.isEliminated }
            val eliminated = players.filter { it.isEliminated }

            if (active.isEmpty() && eliminated.isEmpty()) {
                EmptyPlayersHint()
            } else {
                if (active.isNotEmpty()) {
                    Text(
                        text = "میز گرد • روی بازیکن بزنید تا سخنران شروع شود",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    RoundTable(
                        players = active,
                        onPlayerTap = onPlayerTap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
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
                    LinearPlayerGrid(players = eliminated, onPlayerTap = {}, dimmed = true)
                }
            }
        }
    }
}

@Composable
private fun RoundTable(
    players: List<PlayerEntity>,
    onPlayerTap: (PlayerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MafiaCardBg.copy(alpha = 0.5f))
            .border(1.dp, MafiaBorder, RoundedCornerShape(16.dp))
    ) {
        val size = min(maxWidth.value, maxHeight.value)
        val density = LocalDensity.current
        val sidePx = with(density) { size.dp.toPx() }
        val radius = sidePx * 0.36f
        val center = Offset(sidePx / 2f, sidePx / 2f)

        // Center table label
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🕵️",
                    color = MafiaGold,
                    fontSize = 36.sp
                )
                Text(
                    text = "میز تحلیل",
                    color = TextMutedDark,
                    fontSize = 12.sp
                )
            }
        }

        // Players around the perimeter. We start at the top (-90deg) and
        // go clockwise. The list is given seat numbers 1..N based on order.
        players.forEachIndexed { index, player ->
            val theta = (-Math.PI / 2.0) + (2.0 * Math.PI * index / players.size)
            val x = (center.x + radius * cos(theta)).toFloat()
            val y = (center.y + radius * sin(theta)).toFloat()
            val xDp = with(density) { x.toDp() }
            val yDp = with(density) { y.toDp() }
            // Anchor the player at the (x, y) coordinate. The player circle
            // is 60.dp so we offset by -30.dp from the anchor.
            Box(
                modifier = Modifier
                    .offset(x = xDp - 30.dp, y = yDp - 30.dp)
            ) {
                RoundTablePlayerChip(
                    player = player,
                    seatNumber = index + 1,
                    onClick = { onPlayerTap(player) }
                )
            }
        }
    }
}

@Composable
private fun RoundTablePlayerChip(
    player: PlayerEntity,
    seatNumber: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MafiaCrimson)
                .border(2.dp, MafiaGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = seatNumber.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 64.dp)
            )
        }
    }
}

@Composable
private fun LinearPlayerGrid(
    players: List<PlayerEntity>,
    onPlayerTap: (PlayerEntity) -> Unit,
    dimmed: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { p ->
                    PlayerChipSmall(
                        player = p,
                        dimmed = dimmed,
                        onClick = { onPlayerTap(p) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PlayerChipSmall(
    player: PlayerEntity,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (dimmed) MafiaCardBg.copy(alpha = 0.4f) else MafiaCrimson.copy(alpha = 0.85f))
                .border(1.dp, if (dimmed) MafiaBorder else MafiaCrimsonLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.take(2),
                color = if (dimmed) TextMutedDark else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = player.name,
            color = if (dimmed) TextMutedDark else TextPrimaryDark,
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
