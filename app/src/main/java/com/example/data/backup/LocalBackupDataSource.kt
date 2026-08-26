package com.example.data.backup

import android.content.Context
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.FullTripBackup
import com.example.domain.model.GpsDiagnosticEvent
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimeFormat
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.domain.model.TripStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class LocalBackupDataSource(private val context: Context) {

    private val safetyBackupFile: File by lazy {
        File(context.cacheDir, "trip_timer_safety_snapshot.json")
    }

    /**
     * Serializes complete trip, route, GPS diagnostics, and settings data into versioned JSON.
     */
    fun createBackupJson(
        trips: List<Trip>,
        routes: List<TripPoint>,
        diagnostics: List<GpsDiagnosticEvent>,
        settings: AppSettings
    ): String {
        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("application", "Trip Timer")
        root.put("createdAt", System.currentTimeMillis())

        // Trips
        val tripsArray = JSONArray()
        for (trip in trips) {
            val tObj = JSONObject()
            tObj.put("id", trip.id)
            tObj.put("tripNumber", trip.tripNumber)
            tObj.put("title", trip.title)
            tObj.put("startTime", trip.startTime)
            tObj.put("endTime", trip.endTime)
            tObj.put("totalDurationMillis", trip.totalDurationMillis)
            tObj.put("movingDurationMillis", trip.movingDurationMillis)
            tObj.put("waitingDurationMillis", trip.waitingDurationMillis)
            tObj.put("totalDistanceMeters", trip.totalDistanceMeters)
            tObj.put("averageSpeedMps", trip.averageSpeedMps.toDouble())
            tObj.put("maxSpeedMps", trip.maxSpeedMps.toDouble())
            if (trip.startLatitude != null) tObj.put("startLatitude", trip.startLatitude)
            if (trip.startLongitude != null) tObj.put("startLongitude", trip.startLongitude)
            if (trip.endLatitude != null) tObj.put("endLatitude", trip.endLatitude)
            if (trip.endLongitude != null) tObj.put("endLongitude", trip.endLongitude)
            tObj.put("isCompleted", trip.isCompleted)
            tObj.put("notes", trip.notes)
            tripsArray.put(tObj)
        }
        root.put("trips", tripsArray)

        // Routes
        val routesArray = JSONArray()
        for (p in routes) {
            val rObj = JSONObject()
            rObj.put("id", p.id)
            rObj.put("tripId", p.tripId)
            rObj.put("sequenceNumber", p.sequenceNumber)
            rObj.put("timestamp", p.timestamp)
            rObj.put("latitude", p.latitude)
            rObj.put("longitude", p.longitude)
            rObj.put("speedMps", p.speedMps.toDouble())
            rObj.put("accuracyMeters", p.accuracyMeters.toDouble())
            if (p.altitudeMeters != null) rObj.put("altitudeMeters", p.altitudeMeters)
            if (p.bearingDegrees != null) rObj.put("bearingDegrees", p.bearingDegrees.toDouble())
            rObj.put("status", p.status.name)
            routesArray.put(rObj)
        }
        root.put("routes", routesArray)

        // GPS Diagnostics
        val diagArray = JSONArray()
        for (diag in diagnostics) {
            val dObj = JSONObject()
            dObj.put("id", diag.id)
            dObj.put("tripId", diag.tripId)
            dObj.put("gpsLostTime", diag.gpsLostTime)
            if (diag.gpsRecoveredTime != null) dObj.put("gpsRecoveredTime", diag.gpsRecoveredTime)
            if (diag.durationMillis != null) dObj.put("durationMillis", diag.durationMillis)
            diagArray.put(dObj)
        }
        root.put("gpsDiagnostics", diagArray)

        // Settings
        val sObj = JSONObject()
        sObj.put("movementThresholdKmh", settings.movementThresholdKmh.toDouble())
        sObj.put("idleDetectionDelaySeconds", settings.idleDetectionDelaySeconds)
        sObj.put("gpsUpdateIntervalSeconds", settings.gpsUpdateIntervalSeconds)
        sObj.put("distanceUnit", settings.distanceUnit.name)
        sObj.put("timeFormat", settings.timeFormat.name)
        sObj.put("themeMode", settings.themeMode.name)
        sObj.put("keepScreenAwake", settings.keepScreenAwake)
        sObj.put("powerSavingDimScreen", settings.powerSavingDimScreen)
        sObj.put("vibrationEnabled", settings.vibrationEnabled)
        sObj.put("soundEnabled", settings.soundEnabled)
        sObj.put("fuelPricePerLiter", settings.fuelPricePerLiter)
        sObj.put("fuelEconomyKmPerLiter", settings.fuelEconomyKmPerLiter)
        sObj.put("fuelCurrencySymbol", settings.fuelCurrencySymbol)
        root.put("settings", sObj)

        return root.toString(2)
    }

    /**
     * Parses and validates a backup JSON payload.
     * Throws IllegalArgumentException on invalid or corrupted data.
     */
    fun parseBackupJson(jsonString: String): FullTripBackup {
        if (jsonString.isBlank()) {
            throw IllegalArgumentException("Backup content is empty.")
        }
        val root = JSONObject(jsonString)

        val app = root.optString("application", "")
        if (app != "Trip Timer" && !app.contains("Trip", ignoreCase = true)) {
            throw IllegalArgumentException("Invalid backup file: Application identifier mismatch ('$app').")
        }

        val version = root.optInt("backupVersion", 1)
        if (version < 1) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val createdAt = root.optLong("createdAt", System.currentTimeMillis())

        // Parse Trips
        val tripsList = mutableListOf<Trip>()
        val tripsArray = root.optJSONArray("trips") ?: JSONArray()
        for (i in 0 until tripsArray.length()) {
            val tObj = tripsArray.getJSONObject(i)
            tripsList.add(
                Trip(
                    id = tObj.optLong("id", 0L),
                    tripNumber = tObj.optInt("tripNumber", i + 1),
                    title = tObj.optString("title", "Trip #${i + 1}"),
                    startTime = tObj.optLong("startTime", System.currentTimeMillis()),
                    endTime = tObj.optLong("endTime", System.currentTimeMillis()),
                    totalDurationMillis = tObj.optLong("totalDurationMillis", 0L),
                    movingDurationMillis = tObj.optLong("movingDurationMillis", 0L),
                    waitingDurationMillis = tObj.optLong("waitingDurationMillis", 0L),
                    totalDistanceMeters = tObj.optDouble("totalDistanceMeters", 0.0),
                    averageSpeedMps = tObj.optDouble("averageSpeedMps", 0.0).toFloat(),
                    maxSpeedMps = tObj.optDouble("maxSpeedMps", 0.0).toFloat(),
                    startLatitude = if (tObj.has("startLatitude")) tObj.optDouble("startLatitude") else null,
                    startLongitude = if (tObj.has("startLongitude")) tObj.optDouble("startLongitude") else null,
                    endLatitude = if (tObj.has("endLatitude")) tObj.optDouble("endLatitude") else null,
                    endLongitude = if (tObj.has("endLongitude")) tObj.optDouble("endLongitude") else null,
                    isCompleted = tObj.optBoolean("isCompleted", true),
                    notes = tObj.optString("notes", "")
                )
            )
        }

        // Parse Routes
        val routesList = mutableListOf<TripPoint>()
        val routesArray = root.optJSONArray("routes") ?: JSONArray()
        for (i in 0 until routesArray.length()) {
            val rObj = routesArray.getJSONObject(i)
            val statusStr = rObj.optString("status", TripStatus.MOVING.name)
            routesList.add(
                TripPoint(
                    id = rObj.optLong("id", 0L),
                    tripId = rObj.optLong("tripId", 0L),
                    sequenceNumber = rObj.optInt("sequenceNumber", i),
                    timestamp = rObj.optLong("timestamp", System.currentTimeMillis()),
                    latitude = rObj.getDouble("latitude"),
                    longitude = rObj.getDouble("longitude"),
                    speedMps = rObj.optDouble("speedMps", 0.0).toFloat(),
                    accuracyMeters = rObj.optDouble("accuracyMeters", 0.0).toFloat(),
                    altitudeMeters = if (rObj.has("altitudeMeters")) rObj.optDouble("altitudeMeters") else null,
                    bearingDegrees = if (rObj.has("bearingDegrees")) rObj.optDouble("bearingDegrees").toFloat() else null,
                    status = runCatching { TripStatus.valueOf(statusStr) }.getOrDefault(TripStatus.MOVING)
                )
            )
        }

        // Parse GPS Diagnostics
        val diagList = mutableListOf<GpsDiagnosticEvent>()
        val diagArray = root.optJSONArray("gpsDiagnostics") ?: JSONArray()
        for (i in 0 until diagArray.length()) {
            val dObj = diagArray.getJSONObject(i)
            diagList.add(
                GpsDiagnosticEvent(
                    id = dObj.optLong("id", 0L),
                    tripId = dObj.optLong("tripId", 0L),
                    gpsLostTime = dObj.getLong("gpsLostTime"),
                    gpsRecoveredTime = if (dObj.has("gpsRecoveredTime")) dObj.optLong("gpsRecoveredTime") else null,
                    durationMillis = if (dObj.has("durationMillis")) dObj.optLong("durationMillis") else null
                )
            )
        }

        // Parse Settings
        val sObj = root.optJSONObject("settings")
        val appSettings = if (sObj != null) {
            AppSettings(
                movementThresholdKmh = sObj.optDouble("movementThresholdKmh", 2.0).toFloat(),
                idleDetectionDelaySeconds = sObj.optInt("idleDetectionDelaySeconds", 10),
                gpsUpdateIntervalSeconds = sObj.optInt("gpsUpdateIntervalSeconds", 2),
                distanceUnit = sObj.optString("distanceUnit").let {
                    runCatching { DistanceUnit.valueOf(it) }.getOrDefault(DistanceUnit.KILOMETERS)
                },
                timeFormat = sObj.optString("timeFormat").let {
                    runCatching { TimeFormat.valueOf(it) }.getOrDefault(TimeFormat.H24)
                },
                themeMode = sObj.optString("themeMode").let {
                    runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                },
                keepScreenAwake = sObj.optBoolean("keepScreenAwake", false),
                powerSavingDimScreen = sObj.optBoolean("powerSavingDimScreen", false),
                vibrationEnabled = sObj.optBoolean("vibrationEnabled", true),
                soundEnabled = sObj.optBoolean("soundEnabled", false),
                fuelPricePerLiter = sObj.optDouble("fuelPricePerLiter", 0.0),
                fuelEconomyKmPerLiter = sObj.optDouble("fuelEconomyKmPerLiter", 0.0),
                fuelCurrencySymbol = sObj.optString("fuelCurrencySymbol", "Rs.")
            )
        } else {
            AppSettings()
        }

        return FullTripBackup(
            backupVersion = version,
            application = app,
            createdAt = createdAt,
            trips = tripsList,
            routes = routesList,
            gpsDiagnostics = diagList,
            settings = appSettings
        )
    }

    /**
     * Creates a temporary safety backup snapshot of the local database before restoring.
     */
    fun createSafetyBackupSnapshot(
        trips: List<Trip>,
        routes: List<TripPoint>,
        diagnostics: List<GpsDiagnosticEvent>,
        settings: AppSettings
    ): File {
        val json = createBackupJson(trips, routes, diagnostics, settings)
        FileWriter(safetyBackupFile).use { it.write(json) }
        return safetyBackupFile
    }

    /**
     * Loads the temporary safety backup snapshot if rollback is needed.
     */
    fun loadSafetyBackupSnapshot(): FullTripBackup? {
        if (!safetyBackupFile.exists() || safetyBackupFile.length() == 0L) return null
        return runCatching {
            val json = FileReader(safetyBackupFile).use { it.readText() }
            parseBackupJson(json)
        }.getOrNull()
    }

    /**
     * Deletes the temporary safety backup snapshot upon successful restoration.
     */
    fun deleteSafetyBackupSnapshot(): Boolean {
        return if (safetyBackupFile.exists()) {
            safetyBackupFile.delete()
        } else true
    }
}
