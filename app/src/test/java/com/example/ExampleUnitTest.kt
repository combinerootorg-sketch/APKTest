package com.example

import com.example.domain.model.DistanceUnit
import com.example.utils.Formatters
import com.example.utils.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFormatDuration() {
        val millis = (1 * 3600 + 23 * 60 + 45) * 1000L // 01:23:45
        val formatted = Formatters.formatDuration(millis)
        assertEquals("01:23:45", formatted)
    }

    @Test
    fun testFormatDistance() {
        val meters = 5420.0
        val kmStr = Formatters.formatDistanceWithUnit(meters, DistanceUnit.KILOMETERS)
        assertEquals("5.42 km", kmStr)

        val milesStr = Formatters.formatDistanceWithUnit(meters, DistanceUnit.MILES)
        assertEquals("3.37 mi", milesStr)
    }

    @Test
    fun testFormatSpeed() {
        val speedMps = 10f // 36 km/h
        val speedKm = Formatters.formatSpeedWithUnit(speedMps, DistanceUnit.KILOMETERS)
        assertEquals("36.0 km/h", speedKm)
    }

    @Test
    fun testHaversineDistance() {
        // Distance between two points in London
        val lat1 = 51.5007
        val lon1 = -0.1246
        val lat2 = 51.5033
        val lon2 = -0.1195

        val distance = GeoUtils.calculateDistanceMeters(lat1, lon1, lat2, lon2)
        assertTrue(distance > 300.0 && distance < 600.0)
    }
}
