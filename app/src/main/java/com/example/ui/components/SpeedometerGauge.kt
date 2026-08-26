package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DistanceUnit
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.ElegantWaitingAmber
import com.example.utils.Formatters

@Composable
fun SpeedometerGauge(
    currentSpeedMps: Float,
    maxSpeedMps: Float,
    averageSpeedMps: Float,
    unit: DistanceUnit,
    modifier: Modifier = Modifier
) {
    val speedValue = if (unit == DistanceUnit.KILOMETERS) currentSpeedMps * 3.6f else currentSpeedMps * 2.23694f
    val maxGaugeSpeed = if (unit == DistanceUnit.KILOMETERS) 140f else 90f

    val progress = (speedValue / maxGaugeSpeed).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "speedProgress"
    )

    Column(
        modifier = modifier
            .testTag("speedometer_gauge")
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            val activeBrush = Brush.sweepGradient(
                listOf(
                    ElegantPrimary,
                    ElegantMovingGreen,
                    ElegantWaitingAmber,
                    ElegantPrimary
                )
            )

            Canvas(modifier = Modifier.size(210.dp)) {
                val strokeWidth = 10.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                // Background Track Arc
                drawArc(
                    color = trackColor,
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active Speed Arc
                if (animatedProgress > 0.005f) {
                    drawArc(
                        brush = activeBrush,
                        startAngle = 150f,
                        sweepAngle = 240f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Central Speed Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%.0f", speedValue.coerceAtLeast(0f)),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-2).sp,
                    color = ElegantPrimary,
                    modifier = Modifier.testTag("current_speed_text")
                )
                Text(
                    text = unit.speedSymbol.lowercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Average & Max Speed Sub-Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SpeedStatPill(
                label = "AVG SPEED",
                value = Formatters.formatSpeedWithUnit(averageSpeedMps, unit),
                accentColor = ElegantPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            SpeedStatPill(
                label = "MAX SPEED",
                value = Formatters.formatSpeedWithUnit(maxSpeedMps, unit),
                accentColor = ElegantWaitingAmber
            )
        }
    }
}

@Composable
private fun SpeedStatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}
