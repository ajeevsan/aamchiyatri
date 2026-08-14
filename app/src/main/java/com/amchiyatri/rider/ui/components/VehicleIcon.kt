package com.amchiyatri.rider.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.amchiyatri.rider.data.model.VehicleType

/**
 * The icon/brand-color pair for a ride's vehicle - shared by the driver's live map marker
 * ([AmchiYatriMap]) and the ride history list ([com.amchiyatri.rider.ui.screens.history.RideHistoryScreen])
 * so "what vehicle was this ride" always looks the same wherever it's shown.
 */
fun vehicleIconFor(vehicleType: VehicleType?): Pair<ImageVector, Color> = when (vehicleType) {
    VehicleType.AUTO -> Icons.Filled.ElectricRickshaw to Color(0xFFEF6C00)
    VehicleType.BIKE -> Icons.Filled.TwoWheeler to Color(0xFF6D4C41)
    VehicleType.SEDAN, VehicleType.SUV, null -> Icons.Filled.DirectionsCar to Color(0xFF00695C)
}
