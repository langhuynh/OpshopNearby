package au.edu.jcu.assessment.utilityapp.model

import au.edu.jcu.assessment.utilityapp.api.OverpassElement
import au.edu.jcu.assessment.utilityapp.api.OverpassResponse

// Turns the raw API answer into clean Shop objects for the screen:
//  - buildings use their centre point; places with no position at all are skipped
//  - shops with no name get "Unnamed op shop"
//  - address parts are joined into one line
//  - each shop gets its distance from the user, then the list is sorted closest first
object ShopMapper {
    fun toShops(response: OverpassResponse, userLat: Double, userLon: Double): List<Shop> =
        response.elements.orEmpty()
            .mapNotNull { toShop(it) }
            .distinctBy { it.id }
            .map { it.copy(distanceKm = Distance.km(userLat, userLon, it.latitude, it.longitude)) }
            .sortedBy { it.distanceKm }

    fun toShop(e: OverpassElement): Shop? {
        val tags = e.tags ?: return null
        val lat = e.lat ?: e.center?.lat ?: return null
        val lon = e.lon ?: e.center?.lon ?: return null

        return Shop(
            id = "${e.type ?: "node"}/${e.id ?: 0}",
            name = tags.text("name") ?: tags.text("brand") ?: "Unnamed op shop",
            latitude = lat,
            longitude = lon,
            type = tags.text("shop") ?: "second_hand",
            openingHours = tags.text("opening_hours"),
            address = address(tags),
            website = tags.text("website") ?: tags.text("contact:website"),
            phone = tags.text("phone") ?: tags.text("contact:phone")
        )
    }
    //  from the separate addr:* tags. null if nothing useful.
    private fun address(tags: Map<String, String>): String? {
        val street = listOfNotNull(tags.text("addr:housenumber"), tags.text("addr:street"))
            .joinToString(" ")
            .ifBlank { null }
        val area = tags.text("addr:suburb") ?: tags.text("addr:city")
        return listOfNotNull(street, area).joinToString(", ").ifBlank { null }
    }
    // A tag's value, or null when it's missing or empty.
    private fun Map<String, String>.text(key: String): String? = this[key]?.trim()?.ifEmpty { null }
}
