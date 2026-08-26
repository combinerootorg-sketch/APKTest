package com.example.domain.calculator

import com.example.domain.model.DistanceUnit
import java.util.Locale

/**
 * Data model representing the result of a fuel cost calculation.
 */
data class TripCostEstimate(
    val distanceMeters: Double,
    val distanceKm: Double,
    val fuelUsedLiters: Double,
    val totalCost: Double,
    val fuelPricePerLiter: Double,
    val fuelEconomyKmPerLiter: Double,
    val currencySymbol: String = "Rs.",
    val isConfigured: Boolean = true
) {
    /**
     * Formats the total trip cost with currency symbol (e.g. "Rs. 527.00")
     */
    fun formattedCost(): String {
        return String.format(Locale.US, "%s %.2f", currencySymbol, totalCost)
    }

    /**
     * Formats fuel consumed in litres (e.g. "1.70 L")
     */
    fun formattedFuelUsed(): String {
        return String.format(Locale.US, "%.2f L", fuelUsedLiters)
    }

    /**
     * Formats fuel economy (e.g. "15.0 km/L")
     */
    fun formattedFuelEconomy(): String {
        return String.format(Locale.US, "%.1f km/L", fuelEconomyKmPerLiter)
    }

    /**
     * Formats fuel price per litre (e.g. "Rs. 310.00 / L")
     */
    fun formattedFuelPrice(): String {
        return String.format(Locale.US, "%s %.2f / L", currencySymbol, fuelPricePerLiter)
    }

    /**
     * Formats trip distance according to selected unit
     */
    fun formattedDistance(unit: DistanceUnit): String {
        return if (unit == DistanceUnit.MILES) {
            val miles = distanceKm * 0.621371
            String.format(Locale.US, "%.2f mi", miles)
        } else {
            String.format(Locale.US, "%.2f km", distanceKm)
        }
    }
}

/**
 * Independent, pure calculation engine for Trip Cost Calculator.
 *
 * Formula:
 * Estimated Fuel Used (Litres) = Trip Distance (km) / Fuel Economy (km/L)
 * Estimated Trip Cost = Estimated Fuel Used * Fuel Price per Litre
 */
object TripCostCalculator {

    /**
     * Calculates trip fuel usage and estimated cost given distance in meters,
     * fuel price per litre, and fuel economy in km/L.
     *
     * Returns null if inputs are invalid or unconfigured (<= 0).
     */
    fun calculate(
        distanceMeters: Double,
        fuelPricePerLiter: Double,
        fuelEconomyKmPerLiter: Double,
        currencySymbol: String = "Rs."
    ): TripCostEstimate? {
        if (fuelPricePerLiter <= 0.0 || fuelEconomyKmPerLiter <= 0.0) {
            return null
        }

        val nonNegativeMeters = distanceMeters.coerceAtLeast(0.0)
        val distanceKm = nonNegativeMeters / 1000.0
        val fuelUsedLiters = distanceKm / fuelEconomyKmPerLiter
        val totalCost = fuelUsedLiters * fuelPricePerLiter

        return TripCostEstimate(
            distanceMeters = nonNegativeMeters,
            distanceKm = distanceKm,
            fuelUsedLiters = fuelUsedLiters,
            totalCost = totalCost,
            fuelPricePerLiter = fuelPricePerLiter,
            fuelEconomyKmPerLiter = fuelEconomyKmPerLiter,
            currencySymbol = currencySymbol.ifBlank { "Rs." },
            isConfigured = true
        )
    }

    /**
     * Calculates fuel cost directly from kilometres.
     */
    fun calculateFromKm(
        distanceKm: Double,
        fuelPricePerLiter: Double,
        fuelEconomyKmPerLiter: Double,
        currencySymbol: String = "Rs."
    ): TripCostEstimate? {
        return calculate(
            distanceMeters = distanceKm * 1000.0,
            fuelPricePerLiter = fuelPricePerLiter,
            fuelEconomyKmPerLiter = fuelEconomyKmPerLiter,
            currencySymbol = currencySymbol
        )
    }
}
