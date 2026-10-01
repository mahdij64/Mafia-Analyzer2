package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerEntity
import com.example.data.local.VoteEntity
import com.example.data.model.GameStage
import com.example.ui.components.PersianConfirmDialog
import com.example.ui.components.StageSelectorTabs
import com.example.ui.theme.*

@Composable
fun VotingScreen(
    currentStageIndex: Int,
    unlockedStageIndex: Int = 4,
    players: List<PlayerEntity>,
    allVotes: List<VoteEntity>,
    onStageSelected: (Int) -> Unit,
    onRecordVote: (voterId: Long, targetId: Long) -> Unit,
    onClearVotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    val stage = GameStage.getStage(currentStageIndex)
    val stageVotes = allVotes.filter { it.stageIndex == currentStageIndex }
    val voteMap = stageVotes.associate { it.voterId to it.targetId }
    val playerMap = players.associateBy { it.id }

    // Summary of votes received in this stage
    val votesReceivedCounts = stageVotes
        .filter { it.targetId >= 0 }
        .groupBy { it.targetId }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    if (showClearDialog) {
        PersianConfirmDialog(
            title = "پاک کردن آرای ${stage.title}",
            message = "آیا مطمئن هستید که می‌خواهید تمام آرای ثبت‌شده برای مرحله ${stage.title} را پاک کنید؟",
            confirmText = "پاک کن",
            isDestructive = true,
            onConfirm = {
                onClearVotes()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MafiaDarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        item {
            StageSelectorTabs(
                currentStageIndex = currentStageIndex,
                unlockedStageIndex = unlockedStageIndex,
                onStageSelected = onStageSelected
            )
        }

        // Voting Info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ثبت آراء مرحله ${stage.title}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (currentStageIndex == 0) "مرحله معارفه معمولاً بدون رأی‌گیری است (اختیاری)"
                                else "ثبت رأی خروج هر بازیکن به متهم موردنظر",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }

                        if (stageVotes.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.testTag("btn_clear_stage_votes")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "پاک کردن آراء",
                                    tint = MafiaCrimsonLight
                                )
                            }
                        }
                    }

                    if (votesReceivedCounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "نتایج آراء دریافتی تا الان:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            votesReceivedCounts.take(4).forEach { (targetId, count) ->
                                val targetName = playerMap[targetId]?.name ?: "ناشناس"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MafiaCrimson.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "$targetName: $count رأی",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of Voters
        item {
            Text(
                text = "تعیین رأی هر بازیکن:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(players, key = { it.id }) { voter ->
            val currentTargetId = voteMap[voter.id]
            val currentTargetName = if (currentTargetId != null && currentTargetId >= 0) {
                playerMap[currentTargetId]?.name ?: "انتخاب شده"
            } else {
                "بدون رأی / سفید"
            }

            var expandedDropdown by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (currentTargetId != null && currentTargetId >= 0) MafiaCrimson.copy(alpha = 0.6f) else MafiaBorder
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voter_row_${voter.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = if (currentTargetId != null && currentTargetId >= 0) MafiaCrimson else TextMutedDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = voter.name + if (voter.isEliminated) " 💀" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentTargetId != null && currentTargetId >= 0) MafiaCrimsonDark.copy(alpha = 0.3f) else MafiaSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentTargetId != null && currentTargetId >= 0) MafiaCrimson else MafiaBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("vote_dropdown_${voter.id}")
                        ) {
                            Text(
                                text = "به: $currentTargetName",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (currentTargetId != null && currentTargetId >= 0) TextPrimaryDark else TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.background(MafiaCardBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون رأی / صرف‌نظر", color = TextMutedDark) },
                                onClick = {
                                    onRecordVote(voter.id, -1L)
                                    expandedDropdown = false
                                }
                            )

                            val targetCandidates = players.filter { it.id != voter.id }
                            targetCandidates.forEach { candidate ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = candidate.name + if (candidate.isEliminated) " (خارج شده)" else "",
                                            color = TextPrimaryDark,
                                            fontWeight = if (candidate.id == currentTargetId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onRecordVote(voter.id, candidate.id)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
