package au.edu.jcu.assessment.utilityapp.model

import au.edu.jcu.assessment.utilityapp.api.OverpassElement
import au.edu.jcu.assessment.utilityapp.api.OverpassResponse

// Turns the answer from the internet (OverpassResponse) into a list of Shop for the screen.
object ShopMapper {

    // The whole answer -> a list of shops, closest first.
    fun toShops(response: OverpassResponse, userLat: Double, userLon: Double): List<Shop> {
        val shops = mutableListOf<Shop>()
        val elements = response.elements ?: return shops   // no elements -> empty list

        for (e in elements) {
            val shop = toShop(e, userLat, userLon)
            if (shop != null) shops.add(shop)                // skip the ones we can't use
        }
        return shops.sortedBy { it.distanceKm }               // closest first
    }

    // One element from the answer -> one Shop. Returns null if it can't be used.
    fun toShop(e: OverpassElement, userLat: Double, userLon: Double): Shop? {
        val tags = e.tags
        if (tags == null) return null                        // no details -> skip

        // Where is it? A dot on the map has lat/lon. A building has a center instead.
        var lat = e.lat
        var lon = e.lon
        if (lat == null || lon == null) {
            lat = e.center?.lat
            lon = e.center?.lon
        }
        if (lat == null || lon == null) return null          // no position -> skip

        return Shop(
            id = "${e.type}/${e.id}",                        // e.g. "node/123"
            name = tags["name"] ?: "Unnamed op shop",
            latitude = lat,
            longitude = lon,
            type = tags["shop"] ?: "second_hand",            // "charity" or "second_hand"
            openingHours = tags["opening_hours"],            // e.g. "Mo-Fr 09:00-17:00", or null
            distanceKm = Distance.km(userLat, userLon, lat, lon)
        )
    }
}