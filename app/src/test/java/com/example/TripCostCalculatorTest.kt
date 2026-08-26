package com.example

import com.example.domain.calculator.TripCostCalculator
import com.example.domain.calculator.TripCostEstimate
import com.example.domain.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCostCalculatorTest {

    @Test
    fun `standard calculation with Rs 310 per litre and 15 km per L on 25_5 km trip`() {
        val distanceMeters = 25500.0 // 25.5 km
        val fuelPrice = 310.0
        val fuelEconomy = 15.0
        val currency = "Rs."

        val estimate = TripCostCalculator.calculate(
            distanceMeters = distanceMeters,
            fuelPricePerLiter = fuelPrice,
            fuelEconomyKmPerLiter = fuelEconomy,
            currencySymbol = currency
        )

        assertNotNull("Estimate should not be null when inputs are valid", estimate)
        estimate?.let {
            // Distance check: 25.5 km
            assertEquals(25.5, it.distanceKm, 0.001)

            // Fuel Used: 25.5 km / 15.0 km/L = 1.70 L
            assertEquals(1.70, it.fuelUsedLiters, 0.001)

            // Estimated Cost: 1.70 L * 310.0 = 527.00
            assertEquals(527.0, it.totalCost, 0.001)

            // Formatted checks
            assertEquals("Rs. 527.00", it.formattedCost())
            assertEquals("1.70 L", it.formattedFuelUsed())
            assertEquals("15.0 km/L", it.formattedFuelEconomy())
            assertEquals("Rs. 310.00 / L", it.formattedFuelPrice())
            assertEquals("25.50 km", it.formattedDistance(DistanceUnit.KILOMETERS))
        }
    }

    @Test
    fun `returns null when fuel price is zero or negative`() {
        // Zero price
        val zeroPrice = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = 0.0,
            fuelEconomyKmPerLiter = 15.0
        )
        assertNull("Zero fuel price should return null (unconfigured)", zeroPrice)

        // Negative price
        val negativePrice = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = -50.0,
            fuelEconomyKmPerLiter = 15.0
        )
        assertNull("Negative fuel price should return null (unconfigured)", negativePrice)
    }

    @Test
    fun `returns null when fuel economy is zero or negative`() {
        // Zero economy
        val zeroEconomy = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = 310.0,
            fuelEconomyKmPerLiter = 0.0
        )
        assertNull("Zero fuel economy should return null (unconfigured)", zeroEconomy)

        // Negative economy
        val negativeEconomy = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = 310.0,
            fuelEconomyKmPerLiter = -12.0
        )
        assertNull("Negative fuel economy should return null (unconfigured)", negativeEconomy)
    }

    @Test
    fun `handles zero distance without divide-by-zero errors`() {
        val estimate = TripCostCalculator.calculate(
            distanceMeters = 0.0,
            fuelPricePerLiter = 300.0,
            fuelEconomyKmPerLiter = 12.0
        )

        assertNotNull(estimate)
        assertEquals(0.0, estimate!!.distanceKm, 0.001)
        assertEquals(0.0, estimate.fuelUsedLiters, 0.001)
        assertEquals(0.0, estimate.totalCost, 0.001)
    }

    @Test
    fun `handles negative distance defensively`() {
        val estimate = TripCostCalculator.calculate(
            distanceMeters = -500.0,
            fuelPricePerLiter = 300.0,
            fuelEconomyKmPerLiter = 12.0
        )

        assertNotNull(estimate)
        assertEquals(0.0, estimate!!.distanceKm, 0.001)
        assertEquals(0.0, estimate.fuelUsedLiters, 0.001)
        assertEquals(0.0, estimate.totalCost, 0.001)
    }

    @Test
    fun `calculates correctly as distance increases in real time`() {
        val fuelPrice = 250.0
        val fuelEconomy = 10.0 // 10 km per L -> 0.1 L per km -> 25.0 currency per km

        // At 10 km (10,000 meters)
        val step1 = TripCostCalculator.calculate(10000.0, fuelPrice, fuelEconomy)
        assertNotNull(step1)
        assertEquals(1.0, step1!!.fuelUsedLiters, 0.001)
        assertEquals(250.0, step1.totalCost, 0.001)

        // At 20 km (20,000 meters)
        val step2 = TripCostCalculator.calculate(20000.0, fuelPrice, fuelEconomy)
        assertNotNull(step2)
        assertEquals(2.0, step2!!.fuelUsedLiters, 0.001)
        assertEquals(500.0, step2.totalCost, 0.001)

        // At 50 km (50,000 meters)
        val step3 = TripCostCalculator.calculate(50000.0, fuelPrice, fuelEconomy)
        assertNotNull(step3)
        assertEquals(5.0, step3!!.fuelUsedLiters, 0.001)
        assertEquals(1250.0, step3.totalCost, 0.001)
    }

    @Test
    fun `paused trip maintains constant cost while distance is static`() {
        val fuelPrice = 280.0
        val fuelEconomy = 14.0

        val distanceWhenPaused = 42000.0 // 42 km -> 3.0 L -> 840.0 cost

        val estimateAtPause = TripCostCalculator.calculate(distanceWhenPaused, fuelPrice, fuelEconomy)
        val estimateDuringPause1 = TripCostCalculator.calculate(distanceWhenPaused, fuelPrice, fuelEconomy)
        val estimateDuringPause2 = TripCostCalculator.calculate(distanceWhenPaused, fuelPrice, fuelEconomy)

        assertEquals(estimateAtPause!!.totalCost, estimateDuringPause1!!.totalCost, 0.001)
        assertEquals(estimateAtPause.totalCost, estimateDuringPause2!!.totalCost, 0.001)
        assertEquals(840.0, estimateAtPause.totalCost, 0.001)
    }

    @Test
    fun `miles distance unit formatting converts correctly`() {
        val distanceMeters = 16093.44 // exactly 10 miles = 16.09344 km
        val estimate = TripCostCalculator.calculate(
            distanceMeters = distanceMeters,
            fuelPricePerLiter = 100.0,
            fuelEconomyKmPerLiter = 10.0
        )

        assertNotNull(estimate)
        val milesFormatted = estimate!!.formattedDistance(DistanceUnit.MILES)
        assertTrue("Formatted miles should contain 'mi'", milesFormatted.contains("mi"))
        assertEquals("10.00 mi", milesFormatted)
    }

    @Test
    fun `custom currency symbol is properly propagated`() {
        val estimateDollar = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = 1.50,
            fuelEconomyKmPerLiter = 10.0,
            currencySymbol = "$"
        )
        assertNotNull(estimateDollar)
        assertEquals("$ 1.50", estimateDollar!!.formattedCost())
        assertEquals("$ 1.50 / L", estimateDollar.formattedFuelPrice())

        val estimateEuro = TripCostCalculator.calculate(
            distanceMeters = 10000.0,
            fuelPricePerLiter = 1.80,
            fuelEconomyKmPerLiter = 10.0,
            currencySymbol = "€"
        )
        assertNotNull(estimateEuro)
        assertEquals("€ 1.80", estimateEuro!!.formattedCost())
    }
}
