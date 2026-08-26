package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.model.DistanceUnit
import com.example.domain.model.GpsStatus
import com.example.domain.model.TripStatus
import com.example.ui.components.TripStatusBadge
import com.example.ui.components.TripTimerDisplay
import com.example.ui.theme.TripTimerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun trip_timer_display_screenshot() {
        composeTestRule.setContent {
            TripTimerTheme {
                TripTimerDisplay(
                    totalDurationMillis = 4500000L,
                    movingDurationMillis = 3600000L,
                    waitingDurationMillis = 900000L,
                    totalDistanceMeters = 34500.0,
                    distanceUnit = DistanceUnit.KILOMETERS
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
