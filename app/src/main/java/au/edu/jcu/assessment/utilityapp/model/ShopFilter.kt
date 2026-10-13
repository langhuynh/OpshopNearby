package au.edu.jcu.assessment.utilityapp.model

// Applies the "Open now only" setting, then puts the closest shops first.
// statusOf is passed in so tests can decide which shops are open, without depending on the clock.
object ShopFilter {
    fun apply(
        shops: List<Shop>,
        settings: AppSettings,
        statusOf: (Shop) -> OpenStatus
    ): List<Shop> {
        val filtered = if (settings.openNowOnly) shops.filter { statusOf(it) == OpenStatus.OPEN } else shops
        return filtered.sortedBy { it.distanceKm ?: Double.MAX_VALUE }   // unknown distance goes last
    }
}