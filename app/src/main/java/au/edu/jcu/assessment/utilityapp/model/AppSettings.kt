package au.edu.jcu.assessment.utilityapp.model

// Everything the user can change on the Settings screen.
// These values change how the Utility and Map screens search and show shops.
data class AppSettings(
    val radiusKm: Int = DEFAULT_RADIUS_KM,   // how far to look for shops
    val openNowOnly: Boolean = false         // hide closed shops?
) {
    companion object {
        const val DEFAULT_RADIUS_KM = 5
        val RADIUS_OPTIONS = listOf(2, 5, 10, 20)

        // Builds settings from saved values. A broken saved distance falls back to the default,
        // so an old or corrupted saved value can never crash the app.
        fun fromStored(radiusKm: Int, openNowOnly: Boolean): AppSettings =
            AppSettings(
                radiusKm = if (radiusKm in RADIUS_OPTIONS) radiusKm else DEFAULT_RADIUS_KM,
                openNowOnly = openNowOnly
            )
    }
}