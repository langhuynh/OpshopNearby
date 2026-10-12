package au.edu.jcu.assessment.utilityapp.model

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Distance {
    private const val EARTH_RADIUS_KM = 6371.0

    // Straight-line distance between two points on Earth (the haversine formula), in km.
    fun km(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val km = 2 * EARTH_RADIUS_KM * asin(sqrt(a))
        return (km * 10).roundToInt() / 10.0
    }

    // Text for the screen: metres when under 1 km, otherwise km.
    fun label(km: Double?): String = when {
        km == null -> "Distance unknown"
        km < 1.0 -> "${(km * 1000).roundToInt()} m away"
        else -> String.format(Locale.US, "%.1f km away", km)
    }
}