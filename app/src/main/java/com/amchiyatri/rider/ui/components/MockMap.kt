package com.amchiyatri.rider.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.amchiyatri.rider.data.model.GeoPoint
import kotlin.math.max
import kotlin.math.min

/**
 * A stand-in for a real map. It draws pickup/drop/driver markers and a route line by projecting
 * lat/lng onto the canvas bounds, so the whole booking + live-tracking UI works with zero Maps
 * API key.
 *
 * To go live: add `com.google.maps.android:maps-compose`, drop a `GoogleMap { Marker(...) }`
 * composable in behind this same call site (pickup/drop/driver props line up 1:1 with
 * `MarkerState`), and put your Maps API key in `local.properties` / the manifest as usual.
 */
@Composable
fun MockMap(
    modifier: Modifier = Modifier,
    pickup: GeoPoint? = null,
    drop: GeoPoint? = null,
    driver: GeoPoint? = null,
) {
    Box(modifier = modifier.background(Color(0xFFE8ECE6))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = listOfNotNull(pickup, drop, driver)
            if (points.isEmpty()) return@Canvas

            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }
            val latSpan = max(maxLat - minLat, 0.004)
            val lngSpan = max(maxLng - minLng, 0.004)

            val paddingPx = 90f
            fun project(point: GeoPoint): Offset {
                val xFraction = ((point.lng - minLng) / lngSpan).toFloat()
                val yFraction = ((point.lat - minLat) / latSpan).toFloat()
                val x = paddingPx + xFraction * (size.width - 2 * paddingPx)
                // Screen y grows downward; latitude grows upward, so invert.
                val y = size.height - (paddingPx + yFraction * (size.height - 2 * paddingPx))
                return Offset(x.coerceIn(0f, size.width), y.coerceIn(0f, size.height))
            }

            // Faint road-grid texture so it reads as a map, not a blank canvas.
            val gridColor = Color(0xFFD3DACE)
            val step = min(size.width, size.height) / 8f
            var gx = 0f
            while (gx < size.width) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 2f)
                gx += step
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f)
                gy += step
            }

            if (pickup != null && drop != null) {
                drawLine(
                    color = Color(0xFF00695C),
                    start = project(pickup),
                    end = project(drop),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
                )
            }

            pickup?.let {
                drawCircle(Color(0xFF00695C), radius = 14f, center = project(it))
                drawCircle(Color.White, radius = 5f, center = project(it))
            }
            drop?.let {
                drawCircle(Color(0xFFC77700), radius = 14f, center = project(it))
                drawCircle(Color.White, radius = 5f, center = project(it))
            }
            driver?.let {
                drawCircle(Color.White, radius = 17f, center = project(it))
                drawCircle(Color(0xFF111111), radius = 13f, center = project(it), style = Stroke(width = 3f))
            }
        }
    }
}

@Composable
fun MapPinIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        imageVector = Icons.Filled.Place,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(20.dp),
    )
}
