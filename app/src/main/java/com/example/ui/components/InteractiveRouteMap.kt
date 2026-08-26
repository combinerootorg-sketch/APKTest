package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.TripPoint
import com.example.ui.theme.ElegantError
import com.example.ui.theme.ElegantMovingGreen
import com.example.ui.theme.ElegantPrimary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun InteractiveRouteMap(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    var showOsmMap by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .testTag("interactive_route_map")
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty()) {
                // Empty state when no GPS points recorded
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No GPS route points recorded for this trip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (showOsmMap) {
                // Osmdroid OpenStreetMap Native View
                OsmdroidMapView(
                    points = points,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Offline Canvas Vector Route Visualizer
                CanvasRouteVisualizer(
                    points = points,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Map Layer Toggle (OSM Map vs Offline Vector Route)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showOsmMap = !showOsmMap },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showOsmMap) Icons.Default.Layers else Icons.Default.CloudOff,
                            contentDescription = "Switch Map Mode",
                            tint = ElegantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (showOsmMap) "OSM" else "Vector",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            // Legend on bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElegantMovingGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElegantError)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${points.size} pts",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmdroidMapView(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                controller.setZoom(15.0)

                val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

                if (geoPoints.isNotEmpty()) {
                    // Polyline for Route with Elegant Primary lavender color
                    val line = Polyline().apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.parseColor("#D0BCFF")
                        outlinePaint.strokeWidth = 10f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    overlayManager.add(line)

                    // Start Marker (Green)
                    val startMarker = Marker(this).apply {
                        position = geoPoints.first()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Start Location"
                        snippet = "Trip Start"
                    }
                    overlayManager.add(startMarker)

                    // End Marker (Coral)
                    if (geoPoints.size > 1) {
                        val endMarker = Marker(this).apply {
                            position = geoPoints.last()
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Finish Location"
                            snippet = "Trip Destination"
                        }
                        overlayManager.add(endMarker)
                    }

                    // Fit bounds
                    try {
                        val lats = geoPoints.map { it.latitude }
                        val lons = geoPoints.map { it.longitude }
                        val maxLat = lats.maxOrNull() ?: geoPoints.first().latitude
                        val minLat = lats.minOrNull() ?: geoPoints.first().latitude
                        val maxLon = lons.maxOrNull() ?: geoPoints.first().longitude
                        val minLon = lons.minOrNull() ?: geoPoints.first().longitude

                        val deltaLat = (maxLat - minLat).coerceAtLeast(0.005)
                        val deltaLon = (maxLon - minLon).coerceAtLeast(0.005)

                        val boundingBox = BoundingBox(
                            maxLat + deltaLat * 0.15,
                            maxLon + deltaLon * 0.15,
                            minLat - deltaLat * 0.15,
                            minLon - deltaLon * 0.15
                        )
                        zoomToBoundingBox(boundingBox, true, 80)
                    } catch (_: Exception) {
                        controller.setCenter(geoPoints.first())
                    }
                }
                onResume()
            }
        },
        update = {
            // Map updates on points change
        },
        onRelease = { mapView ->
            mapView.onPause()
            mapView.onDetach()
        }
    )
}

@Composable
private fun CanvasRouteVisualizer(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color(0xFF141218))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty()) return@Canvas

            val lats = points.map { it.latitude }
            val lons = points.map { it.longitude }
            val minLat = lats.minOrNull() ?: 0.0
            val maxLat = lats.maxOrNull() ?: 0.0
            val minLon = lons.minOrNull() ?: 0.0
            val maxLon = lons.maxOrNull() ?: 0.0

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
            val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)

            val padding = 50.dp.toPx()
            val drawWidth = size.width - (padding * 2)
            val drawHeight = size.height - (padding * 2)

            // Draw subtle background grid
            val gridColor = Color(0xFF2B2930)
            for (i in 1..4) {
                val gx = (size.width / 5) * i
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                val gy = (size.height / 5) * i
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }

            fun projectPoint(lat: Double, lon: Double): Offset {
                val normX = ((lon - minLon) / lonSpan).toFloat()
                val normY = (1.0f - ((lat - minLat) / latSpan).toFloat()) // Invert Y for canvas

                val px = padding + (normX * drawWidth)
                val py = padding + (normY * drawHeight)

                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val scaledX = (px - centerX) * scale + centerX + offsetX
                val scaledY = (py - centerY) * scale + centerY + offsetY

                return Offset(scaledX, scaledY)
            }

            val path = Path()
            points.forEachIndexed { index, pt ->
                val p = projectPoint(pt.latitude, pt.longitude)
                if (index == 0) {
                    path.moveTo(p.x, p.y)
                } else {
                    path.lineTo(p.x, p.y)
                }
            }

            // Draw Route Polyline
            drawPath(
                path = path,
                color = ElegantPrimary,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Start Point Marker (Green Pin)
            val startOffset = projectPoint(points.first().latitude, points.first().longitude)
            drawCircle(color = ElegantMovingGreen, radius = 8.dp.toPx(), center = startOffset)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = startOffset)

            // End Point Marker (Coral Pin)
            if (points.size > 1) {
                val endOffset = projectPoint(points.last().latitude, points.last().longitude)
                drawCircle(color = ElegantError, radius = 8.dp.toPx(), center = endOffset)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = endOffset)
            }
        }

        // Reset zoom button
        IconButton(
            onClick = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset Zoom",
                tint = ElegantPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
