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
    private const val MILES_PER_KM = 0.621371

    // Straight-line distance between two points on Earth (the haversine formula), in km.
    fun km(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))   // CHANGE 1: no rounding here any more
    }

    // Text for the screen, in the unit chosen on the Settings screen.
    fun label(km: Double?, unit: DistanceUnit = DistanceUnit.KM): String = when {
        km == null -> "Distance unknown"
        unit == DistanceUnit.MILES -> String.format(Locale.US, "%.1f mi away", km * MILES_PER_KM)
        km < 1.0 -> "${((km * 1000) / 10).roundToInt() * 10} m away"   // CHANGE 2: nearest 10 m, e.g. 0.552 km -> "550 m away"
        else -> String.format(Locale.US, "%.1f km away", km)
    }
}