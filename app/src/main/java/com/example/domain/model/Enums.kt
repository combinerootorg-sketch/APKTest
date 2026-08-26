package com.example.domain.model

enum class TripStatus {
    NOT_STARTED,
    MOVING,
    WAITING,
    PAUSED,
    STOPPED
}

enum class GpsStatus {
    AVAILABLE,
    WEAK,
    UNAVAILABLE,
    PERMISSION_REQUIRED
}

enum class DistanceUnit(val displayName: String, val unitSymbol: String, val speedSymbol: String) {
    KILOMETERS("Kilometres (km, km/h)", "km", "km/h"),
    MILES("Miles (mi, mph)", "mi", "mph")
}

enum class TimeFormat(val displayName: String) {
    H24("24-Hour (14:30)"),
    H12("12-Hour (2:30 PM)")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}
