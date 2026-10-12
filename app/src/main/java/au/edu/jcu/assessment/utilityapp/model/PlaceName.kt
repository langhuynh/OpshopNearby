package au.edu.jcu.assessment.utilityapp.model

import java.util.Locale

// Turns the parts of an address into a short place name for the top of the screen,
// e.g. suburb "Brisbane" + state "Queensland" -> "Brisbane, QLD".
object PlaceName {

    private val AU_STATES = mapOf(
        "queensland" to "QLD",
        "new south wales" to "NSW",
        "victoria" to "VIC",
        "tasmania" to "TAS",
        "south australia" to "SA",
        "western australia" to "WA",
        "northern territory" to "NT",
        "australian capital territory" to "ACT"
    )

    // suburb: the most local name (e.g. "Kirwan"); city: used if there's no suburb; state: e.g. "Queensland"
    fun format(suburb: String?, city: String?, state: String?): String? {
        val place = suburb.clean() ?: city.clean()
        val shortState = state.clean()?.let { AU_STATES[it.lowercase()] ?: it }
        return listOfNotNull(place, shortState).joinToString(", ").ifEmpty { null }
    }

    // Used when no place name can be found: shows the coordinates instead.
    fun coordinates(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.4f, %.4f", lat, lon)

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}
