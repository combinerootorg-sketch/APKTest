package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calculator.TripCostCalculator
import com.example.domain.calculator.TripCostEstimate
import com.example.domain.model.DistanceUnit
import com.example.ui.theme.ElegantAccentGold
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.StatusMovingGreen
import com.example.ui.theme.StatusWaitingAmber

@Composable
fun TripCostCard(
    distanceMeters: Double,
    fuelPricePerLiter: Double,
    fuelEconomyKmPerLiter: Double,
    currencySymbol: String = "Rs.",
    distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    modifier: Modifier = Modifier,
    title: String = "Trip Cost Calculator",
    onConfigureClick: (() -> Unit)? = null
) {
    val estimate: TripCostEstimate? = TripCostCalculator.calculate(
        distanceMeters = distanceMeters,
        fuelPricePerLiter = fuelPricePerLiter,
        fuelEconomyKmPerLiter = fuelEconomyKmPerLiter,
        currencySymbol = currencySymbol
    )

    Card(
        modifier = modifier
            .testTag("trip_cost_calculator_card")
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (estimate != null) ElegantAccentGold.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Fuel Calculator",
                            tint = if (estimate != null) ElegantAccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (estimate != null) "Real-time fuel expenditure estimate" else "Optional fuel economy calculation",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (estimate != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusMovingGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = StatusMovingGreen
                        )
                    }
                }
            }

            if (estimate != null) {
                // Main Cost Hero Readout
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ESTIMATED TRIP COST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = estimate.formattedCost(),
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = ElegantAccentGold,
                            modifier = Modifier.testTag("estimated_trip_cost_text")
                        )
                    }
                }

                // 2x2 Metric Breakdown Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CostMetricItem(
                        icon = Icons.Default.LocalGasStation,
                        label = "Fuel Used",
                        value = estimate.formattedFuelUsed(),
                        iconColor = ElegantAccentGold,
                        modifier = Modifier
                            .testTag("estimated_fuel_used_stat")
                            .weight(1f)
                    )
                    CostMetricItem(
                        icon = Icons.Default.Straighten,
                        label = "Trip Distance",
                        value = estimate.formattedDistance(distanceUnit),
                        iconColor = ElegantPrimary,
                        modifier = Modifier
                            .testTag("calculator_distance_stat")
                            .weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CostMetricItem(
                        icon = Icons.Default.Paid,
                        label = "Fuel Price",
                        value = estimate.formattedFuelPrice(),
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .testTag("calculator_fuel_price_stat")
                            .weight(1f)
                    )
                    CostMetricItem(
                        icon = Icons.Default.Speed,
                        label = "Fuel Economy",
                        value = estimate.formattedFuelEconomy(),
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .testTag("calculator_fuel_economy_stat")
                            .weight(1f)
                    )
                }
            } else {
                // Unconfigured State Informational Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unconfigured_cost_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusWaitingAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configure fuel price and fuel economy to calculate trip cost.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onConfigureClick != null) {
                    OutlinedButton(
                        onClick = onConfigureClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("configure_fuel_cost_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Configure in Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CostMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
