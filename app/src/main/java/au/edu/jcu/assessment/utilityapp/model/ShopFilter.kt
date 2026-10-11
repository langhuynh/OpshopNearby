package au.edu.jcu.assessment.utilityapp.model

// Applies what the user chose: the search text, the "open now" filter and the sort order.
// statusOf is passed in so tests can decide which shops are open, without depending on the clock.
object ShopFilter {

    fun apply(
        shops: List<Shop>,
        settings: AppSettings,
        query: String = "",
        statusOf: (Shop) -> OpenStatus
    ): List<Shop> {
        val searched = shops.filter { matches(it, query) }
        val filtered = if (settings.openNowOnly) searched.filter { statusOf(it) == OpenStatus.OPEN } else searched
        val byDistance = compareBy<Shop> { it.distanceKm ?: Double.MAX_VALUE }
        return when (settings.sortBy) {
            SortOption.DISTANCE -> filtered.sortedWith(byDistance)
            SortOption.NAME -> filtered.sortedWith(compareBy<Shop> { it.name.lowercase() }.then(byDistance))
            SortOption.OPEN_FIRST -> filtered.sortedWith(
                compareBy<Shop> { statusOf(it) != OpenStatus.OPEN }.then(byDistance)
            )
        }
    }

    // Search looks in the name, address and shop type, ignoring capital letters and extra spaces.
    // Every word must match somewhere, so "vinnies" finds the Vinnies.
    fun matches(shop: Shop, query: String): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val text = listOfNotNull(shop.name, shop.address, shop.typeLabel).joinToString(" ").lowercase()
        return words.all { it in text }
    }
}
