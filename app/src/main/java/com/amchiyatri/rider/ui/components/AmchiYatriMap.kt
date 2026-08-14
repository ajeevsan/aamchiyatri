package com.amchiyatri.rider.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.amchiyatri.rider.data.model.GeoPoint
import com.amchiyatri.rider.data.model.VehicleType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * The real Google Map, replacing [MockMap] now that a Maps API key is configured (see
 * app/secrets.properties, SETUP.md). Same pickup/drop/driver shape as MockMap, plus an optional
 * decoded route polyline from the Directions API.
 */
@Composable
fun AmchiYatriMap(
    modifier: Modifier = Modifier,
    pickup: GeoPoint? = null,
    drop: GeoPoint? = null,
    driver: GeoPoint? = null,
    vehicleType: VehicleType? = null,
    routePoints: List<GeoPoint> = emptyList(),
) {
    val context = LocalContext.current
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    val focusPoint = pickup ?: drop ?: driver ?: GeoPoint(19.0760, 72.8777) // Mumbai city centre
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(focusPoint.lat, focusPoint.lng), 14f)
    }

    LaunchedEffect(pickup, drop, driver) {
        val points = listOfNotNull(pickup, drop, driver)
        if (points.isEmpty()) return@LaunchedEffect
        if (points.size == 1) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(points[0].lat, points[0].lng), 15f),
            )
        } else {
            val bounds = LatLngBounds.Builder()
            points.forEach { bounds.include(LatLng(it.lat, it.lng)) }
            runCatching {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds.build(), 160))
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
    ) {
        pickup?.let {
            Marker(
                state = MarkerState(LatLng(it.lat, it.lng)),
                title = "Pickup",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
            )
        }
        drop?.let {
            Marker(
                state = MarkerState(LatLng(it.lat, it.lng)),
                title = "Drop",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
            )
        }
        driver?.let {
            MarkerComposable(
                state = MarkerState(LatLng(it.lat, it.lng)),
                title = "Driver",
            ) {
                VehicleMarkerIcon(vehicleType)
            }
        }
        if (routePoints.isNotEmpty()) {
            Polyline(points = routePoints.map { LatLng(it.lat, it.lng) }, color = Color(0xFF00695C))
        }
    }
}

/**
 * The driver's live marker: a vehicle-specific icon (auto-rickshaw/bike/car) in a colored circle,
 * rather than a generic map pin - rendered as real Compose content via [MarkerComposable] (which
 * rasterizes it to a [com.google.android.gms.maps.model.BitmapDescriptor] under the hood) so it's
 * a plain vector icon like anywhere else in the app, not a bitmap asset to ship.
 */
@Composable
private fun VehicleMarkerIcon(vehicleType: VehicleType?) {
    val (icon, background) = vehicleIconFor(vehicleType)
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(background, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Driver vehicle",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
