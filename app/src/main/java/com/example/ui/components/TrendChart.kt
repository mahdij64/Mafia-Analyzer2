package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameStage
import com.example.ui.theme.*

@Composable
fun SuspicionTrendChart(
    stageScores: List<Pair<Int, Int>>, // List of (stageIndex, score)
    playerName: String,
    modifier: Modifier = Modifier
) {
    if (stageScores.isEmpty()) return

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MafiaCardElevated, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "روند تغییرات سوءظن: $playerName",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark
            )
            val latestScore = stageScores.lastOrNull()?.second ?: 50
            SuspicionScoreBadge(score = latestScore)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 30f
                val paddingRight = 30f
                val paddingTop = 20f
                val paddingBottom = 30f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Draw horizontal guide lines (25, 50, 75, 100)
                val gridLevels = listOf(25, 50, 75)
                for (level in gridLevels) {
                    val y = paddingTop + chartHeight * (1f - level / 100f)
                    drawLine(
                        color = MafiaBorder.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f
                    )
                }

                // Middle 50 baseline
                val midY = paddingTop + chartHeight * 0.5f
                drawLine(
                    color = MafiaBorder,
                    start = Offset(paddingLeft, midY),
                    end = Offset(width - paddingRight, midY),
                    strokeWidth = 1.5f
                )

                val points = stageScores.mapIndexed { index, pair ->
                    val x = if (stageScores.size > 1) {
                        if (isRtl) {
                            (width - paddingRight) - (chartWidth / (stageScores.size - 1)) * index
                        } else {
                            paddingLeft + (chartWidth / (stageScores.size - 1)) * index
                        }
                    } else {
                        width / 2f
                    }
                    val y = paddingTop + chartHeight * (1f - (pair.second.coerceIn(0, 100) / 100f))
                    Offset(x, y)
                }

                // Draw path connecting points
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = MafiaCrimson,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw dots and score values
                points.forEachIndexed { i, pt ->
                    val score = stageScores[i].second
                    val dotColor = when (score) {
                        in 0..45 -> SuspicionGreen
                        in 46..65 -> SuspicionYellow
                        else -> SuspicionRed
                    }

                    // Outer halo
                    drawCircle(
                        color = dotColor.copy(alpha = 0.3f),
                        radius = 8.dp.toPx(),
                        center = pt
                    )
                    // Inner solid dot
                    drawCircle(
                        color = dotColor,
                        radius = 4.5.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Stage labels below chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stageScores.forEach { (stgIdx, score) ->
                val stageTitle = GameStage.getStage(stgIdx).title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stageTitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$score",
                        fontSize = 12.sp,
                        color = if (score >= 65) MafiaCrimson else TextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
