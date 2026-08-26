package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.Trip
import com.example.domain.model.TripPoint
import com.example.utils.Formatters
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripExportManager(private val context: Context) {

    private val exportDir: File by lazy {
        File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Exports list of trips to CSV format and returns the saved File
     */
    fun exportTripsToCsv(trips: List<Trip>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "TripTimer_Trips_$timestamp.csv")

        FileWriter(file).use { writer ->
            // CSV Header
            writer.append("Trip Number,Title,Date,Start Time,End Time,Total Duration,Moving Duration,Waiting Duration,Total Distance (m),Total Distance (km),Avg Speed (km/h),Max Speed (km/h),Start Latitude,Start Longitude,End Latitude,End Longitude,Notes\n")

            for (trip in trips) {
                val dateStr = Formatters.formatDate(trip.startTime)
                val startStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(trip.startTime))
                val endStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(trip.endTime))
                val totalDurStr = Formatters.formatDuration(trip.totalDurationMillis)
                val movingDurStr = Formatters.formatDuration(trip.movingDurationMillis)
                val waitingDurStr = Formatters.formatDuration(trip.waitingDurationMillis)
                val distKm = trip.totalDistanceMeters / 1000.0
                val avgSpeedKmh = trip.averageSpeedMps * 3.6
                val maxSpeedKmh = trip.maxSpeedMps * 3.6

                val line = buildString {
                    append(trip.tripNumber).append(",")
                    append("\"").append(trip.title.replace("\"", "\"\"")).append("\",")
                    append("\"").append(dateStr).append("\",")
                    append("\"").append(startStr).append("\",")
                    append("\"").append(endStr).append("\",")
                    append("\"").append(totalDurStr).append("\",")
                    append("\"").append(movingDurStr).append("\",")
                    append("\"").append(waitingDurStr).append("\",")
                    append(String.format(Locale.US, "%.1f", trip.totalDistanceMeters)).append(",")
                    append(String.format(Locale.US, "%.3f", distKm)).append(",")
                    append(String.format(Locale.US, "%.2f", avgSpeedKmh)).append(",")
                    append(String.format(Locale.US, "%.2f", maxSpeedKmh)).append(",")
                    append(trip.startLatitude ?: "").append(",")
                    append(trip.startLongitude ?: "").append(",")
                    append(trip.endLatitude ?: "").append(",")
                    append(trip.endLongitude ?: "").append(",")
                    append("\"").append(trip.notes.replace("\"", "\"\"")).append("\"\n")
                }
                writer.append(line)
            }
        }
        return file
    }

    /**
     * Exports full trips and GPS route points to JSON format
     */
    fun exportTripsToJson(tripsWithPoints: List<Pair<Trip, List<TripPoint>>>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "TripTimer_Complete_Route_$timestamp.json")

        val rootObject = JSONObject()
        rootObject.put("appName", "Trip Timer")
        rootObject.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        rootObject.put("totalTrips", tripsWithPoints.size)

        val tripsArray = JSONArray()
        for ((trip, points) in tripsWithPoints) {
            val tripObj = JSONObject()
            tripObj.put("id", trip.id)
            tripObj.put("tripNumber", trip.tripNumber)
            tripObj.put("title", trip.title)
            tripObj.put("startTime", trip.startTime)
            tripObj.put("endTime", trip.endTime)
            tripObj.put("totalDurationMillis", trip.totalDurationMillis)
            tripObj.put("movingDurationMillis", trip.movingDurationMillis)
            tripObj.put("waitingDurationMillis", trip.waitingDurationMillis)
            tripObj.put("totalDistanceMeters", trip.totalDistanceMeters)
            tripObj.put("averageSpeedMps", trip.averageSpeedMps)
            tripObj.put("maxSpeedMps", trip.maxSpeedMps)
            tripObj.put("startLatitude", trip.startLatitude)
            tripObj.put("startLongitude", trip.startLongitude)
            tripObj.put("endLatitude", trip.endLatitude)
            tripObj.put("endLongitude", trip.endLongitude)
            tripObj.put("notes", trip.notes)

            val pointsArray = JSONArray()
            for (p in points) {
                val pObj = JSONObject()
                pObj.put("seq", p.sequenceNumber)
                pObj.put("timestamp", p.timestamp)
                pObj.put("lat", p.latitude)
                pObj.put("lng", p.longitude)
                pObj.put("speedMps", p.speedMps)
                pObj.put("accuracyMeters", p.accuracyMeters)
                pObj.put("altitudeMeters", p.altitudeMeters)
                pObj.put("bearingDegrees", p.bearingDegrees)
                pObj.put("status", p.status.name)
                pointsArray.put(pObj)
            }
            tripObj.put("gpsRoute", pointsArray)
            tripsArray.put(tripObj)
        }

        rootObject.put("trips", tripsArray)

        file.writeText(rootObject.toString(2))
        return file
    }

    /**
     * Creates an Intent to share exported file using Android FileProvider
     */
    fun createShareIntent(file: File, mimeType: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Trip Timer Export: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
