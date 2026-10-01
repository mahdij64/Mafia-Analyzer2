package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
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
import com.example.data.local.TargetEntity
import com.example.data.model.GameStage
import com.example.ui.components.StageSelectorTabs
import com.example.ui.theme.*

@Composable
fun RelationshipMapScreen(
    currentStageIndex: Int,
    players: List<PlayerEntity>,
    allTargets: List<TargetEntity>,
    onStageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = GameStage.getStage(currentStageIndex)
    val playerMap = players.associateBy { it.id }

    // Targets for current stage
    val stageTargets = allTargets.filter { it.stageIndex == currentStageIndex }

    // Set of pairs (source, target)
    val targetPairs = stageTargets.map { it.sourcePlayerId to it.targetPlayerId }.toSet()

    // Identify mutual conflicts (A->B and B->A)
    val mutualConflicts = remember(targetPairs) {
        val list = mutableListOf<Pair<Long, Long>>()
        val seen = mutableSetOf<Pair<Long, Long>>()
        for ((src, tgt) in targetPairs) {
            if (targetPairs.contains(tgt to src) && !seen.contains(tgt to src)) {
                list.add(src to tgt)
                seen.add(src to tgt)
                seen.add(tgt to src)
            }
        }
        list
    }

    // Identify target chains (A -> B -> C)
    val targetChains = remember(targetPairs) {
        val chains = mutableListOf<Triple<Long, Long, Long>>()
        for ((a, b) in targetPairs) {
            for ((b2, c) in targetPairs) {
                if (b == b2 && c != a && !targetPairs.contains(c to a)) {
                    chains.add(Triple(a, b, c))
                }
            }
        }
        chains.distinctBy { "${it.first}-${it.second}-${it.third}" }.take(8)
    }

    // Day-to-day comparison if stage > 0
    val prevStageTargets = remember(allTargets, currentStageIndex) {
        if (currentStageIndex > 0) {
            allTargets.filter { it.stageIndex == currentStageIndex - 1 }
        } else emptyList()
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
                onStageSelected = onStageSelected
            )
        }

        // Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🗺️ نقشه خطوط تارگت و تقابل‌ها (${stage.title})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MafiaGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "شناسایی تقابل‌های مستقیم دوطرفه و کانون‌های اتهام در مرحله جاری",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    if (mutualConflicts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚔️ تقابل‌های مستقیم دوطرفه شناسایی‌شده:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaCrimsonLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        mutualConflicts.forEach { (p1, p2) ->
                            val n1 = playerMap[p1]?.name ?: "?"
                            val n2 = playerMap[p2]?.name ?: "?"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MafiaCrimson.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MafiaCrimson.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                        contentDescription = null,
                                        tint = MafiaCrimsonLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$n1 ⟷ $n2 (تارگت متقابل به همدیگر)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    // Target chains
                    if (targetChains.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🔗 زنجیره‌های تارگت متوالی (A → B → C):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        targetChains.forEach { (pA, pB, pC) ->
                            val nA = playerMap[pA]?.name ?: "?"
                            val nB = playerMap[pB]?.name ?: "?"
                            val nC = playerMap[pC]?.name ?: "?"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MafiaSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MafiaBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• $nA → $nB → $nC",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }

                    // Day-to-day comparison
                    if (currentStageIndex > 0) {
                        val prevStageTitle = GameStage.getStage(currentStageIndex - 1).title
                        val positionChanges = mutableListOf<String>()

                        players.forEach { p ->
                            val prevTargets = prevStageTargets.filter { it.sourcePlayerId == p.id }.map { it.targetPlayerId }.toSet()
                            val currTargets = stageTargets.filter { it.sourcePlayerId == p.id }.map { it.targetPlayerId }.toSet()

                            val dropped = prevTargets - currTargets
                            val added = currTargets - prevTargets

                            if (dropped.isNotEmpty() || added.isNotEmpty()) {
                                val parts = mutableListOf<String>()
                                if (added.isNotEmpty()) {
                                    val addedNames = added.mapNotNull { playerMap[it]?.name }.joinToString("، ")
                                    parts.add("افزودن تارگت به ($addedNames)")
                                }
                                if (dropped.isNotEmpty()) {
                                    val droppedNames = dropped.mapNotNull { playerMap[it]?.name }.joinToString("، ")
                                    parts.add("برداشتن تارگت از ($droppedNames)")
                                }
                                positionChanges.add("${p.name}: ${parts.joinToString(" و ")}")
                            }
                        }

                        if (positionChanges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🔄 تغییر موضع تارگت‌ها نسبت به $prevStageTitle:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64B5F6)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            positionChanges.forEach { changeText ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF64B5F6).copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "• $changeText",
                                        fontSize = 12.sp,
                                        color = TextPrimaryDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "جریان تارگت‌های خروجی و ورودی هر بازیکن:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        // Each player's target relationships card
        items(players, key = { it.id }) { player ->
            val outgoingTargets = stageTargets.filter { it.sourcePlayerId == player.id }
            val incomingTargets = stageTargets.filter { it.targetPlayerId == player.id }

            Card(
                colors = CardDefaults.cardColors(containerColor = MafiaCardBg),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MafiaBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("relation_card_${player.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name + if (player.isEliminated) " 💀" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MafiaSurfaceVariant
                            ) {
                                Text(
                                    text = "${outgoingTargets.size} تارگت زد",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (incomingTargets.size >= 2) MafiaCrimsonDark else MafiaSurfaceVariant
                            ) {
                                Text(
                                    text = "${incomingTargets.size} تارگت خورد",
                                    fontSize = 11.sp,
                                    color = if (incomingTargets.size >= 2) MafiaCrimsonLight else TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Outgoing
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "→ تارگت به: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaGold,
                            modifier = Modifier.width(85.dp)
                        )
                        if (outgoingTargets.isEmpty()) {
                            Text(text = "هیچ‌کس", fontSize = 13.sp, color = TextMutedDark)
                        } else {
                            val targetsNames = outgoingTargets.mapNotNull { playerMap[it.targetPlayerId]?.name }.joinToString("، ")
                            Text(text = targetsNames, fontSize = 13.sp, color = TextPrimaryDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Incoming
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "← تارگت از: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MafiaCrimsonLight,
                            modifier = Modifier.width(85.dp)
                        )
                        if (incomingTargets.isEmpty()) {
                            Text(text = "هیچ‌کس", fontSize = 13.sp, color = TextMutedDark)
                        } else {
                            val attackers = incomingTargets.mapNotNull { playerMap[it.sourcePlayerId]?.name }.joinToString("، ")
                            Text(text = attackers, fontSize = 13.sp, color = TextPrimaryDark)
                        }
                    }
                }
            }
        }
    }
}
