package com.amchiyatri.rider.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.amchiyatri.rider.data.model.GeoPoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
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
            Marker(
                state = MarkerState(LatLng(it.lat, it.lng)),
                title = "Driver",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )
        }
        if (routePoints.isNotEmpty()) {
            Polyline(points = routePoints.map { LatLng(it.lat, it.lng) }, color = Color(0xFF00695C))
        }
    }
}
