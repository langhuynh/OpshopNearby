package au.edu.jcu.assessment.utilityapp.model

enum class DistanceUnit(val label: String) { KM("Kilometres"), MILES("Miles") }

enum class SortOption(val label: String) {
    DISTANCE("Closest first"),
    NAME("Name A–Z"),
    OPEN_FIRST("Open shops first")
}

// Everything the user can change on the Settings screen.
// These values change how the Utility screen searches and shows shops.
data class AppSettings(
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val openNowOnly: Boolean = false,
    val sortBy: SortOption = SortOption.DISTANCE,
    val unit: DistanceUnit = DistanceUnit.KM
) {
    companion object {
        const val DEFAULT_RADIUS_KM = 5
        val RADIUS_OPTIONS = listOf(2, 5, 10, 20)

        // Builds settings from saved values. Anything unknown or broken falls back to the default,
        // so an old or corrupted saved value can never crash the app.
        fun fromStored(radiusKm: Int, openNowOnly: Boolean, sortBy: String?, unit: String?): AppSettings =
            AppSettings(
                radiusKm = if (radiusKm in RADIUS_OPTIONS) radiusKm else DEFAULT_RADIUS_KM,
                openNowOnly = openNowOnly,
                sortBy = SortOption.entries.firstOrNull { it.name == sortBy } ?: SortOption.DISTANCE,
                unit = DistanceUnit.entries.firstOrNull { it.name == unit } ?: DistanceUnit.KM
            )
    }
}
