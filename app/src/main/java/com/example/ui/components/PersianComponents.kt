package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameStage
import com.example.ui.theme.*

@Composable
fun StageSelectorTabs(
    currentStageIndex: Int,
    maxStageIndex: Int = 4,
    unlockedStageIndex: Int = 4,
    onStageSelected: (Int) -> Unit,
    onLockedStageClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = currentStageIndex.coerceAtMost(maxStageIndex),
        edgePadding = 12.dp,
        containerColor = MafiaCardBg,
        contentColor = MafiaCrimson,
        divider = {
            HorizontalDivider(color = MafiaBorder, thickness = 1.dp)
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("stage_tabs")
    ) {
        val count = maxOf(5, maxOf(currentStageIndex, unlockedStageIndex) + 1)
        for (i in 0 until count) {
            val stage = GameStage.getStage(i)
            val isSelected = i == currentStageIndex
            val isUnlocked = i <= unlockedStageIndex
            val isCompleted = i < unlockedStageIndex

            Tab(
                selected = isSelected,
                onClick = {
                    if (isUnlocked) {
                        onStageSelected(i)
                    } else {
                        onLockedStageClick?.invoke(i)
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!isUnlocked) {
                            Text(
                                text = "🔒 ",
                                fontSize = 11.sp
                            )
                        } else if (isCompleted) {
                            Text(
                                text = "✓ ",
                                fontSize = 12.sp,
                                color = MafiaGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stage.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MafiaCrimson
                                isCompleted -> TextPrimaryDark
                                isUnlocked -> TextSecondaryDark
                                else -> TextMutedDark.copy(alpha = 0.5f)
                            },
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .testTag("stage_tab_$i")
            )
        }
    }
}

@Composable
fun SuspicionScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (score) {
        in 0..25 -> SuspicionGreen.copy(alpha = 0.2f) to SuspicionGreen
        in 26..45 -> Color(0xFF64B5F6).copy(alpha = 0.2f) to Color(0xFF64B5F6)
        in 46..65 -> SuspicionYellow.copy(alpha = 0.2f) to SuspicionYellow
        in 66..80 -> SuspicionOrange.copy(alpha = 0.2f) to SuspicionOrange
        else -> SuspicionRed.copy(alpha = 0.2f) to SuspicionRed
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$score",
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 15.sp
            )
            Text(
                text = "/100",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
fun SuspicionProgressBar(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )
    val color = when (score) {
        in 0..25 -> SuspicionGreen
        in 26..45 -> Color(0xFF64B5F6)
        in 46..65 -> SuspicionYellow
        in 66..80 -> SuspicionOrange
        else -> SuspicionRed
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MafiaSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val chipColor = when (score) {
        in 0..25 -> SuspicionGreen
        in 26..45 -> Color(0xFF64B5F6)
        in 46..65 -> SuspicionYellow
        in 66..80 -> SuspicionOrange
        else -> SuspicionRed
    }

    Surface(
        color = chipColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, chipColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = chipColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PersianConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "تأیید",
    cancelText: String = "انصراف",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) MafiaCrimson else TextPrimaryDark
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondaryDark,
                lineHeight = 24.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MafiaCrimson else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text(text = cancelText, color = TextMutedDark)
            }
        },
        containerColor = MafiaCardBg,
        tonalElevation = 6.dp
    )
}
