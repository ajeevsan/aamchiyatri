package com.amchiyatri.rider.util

import com.amchiyatri.rider.data.model.GeoPoint

/**
 * Decodes Google's encoded polyline format (the `overview_polyline.points` field returned by the
 * Directions API) into a list of lat/lng points. Standard algorithm, see:
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
fun decodePolyline(encoded: String): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var shift = 0
        var result = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val deltaLat = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
        lat += deltaLat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val deltaLng = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
        lng += deltaLng

        points += GeoPoint(lat / 1e5, lng / 1e5)
    }
    return points
}
