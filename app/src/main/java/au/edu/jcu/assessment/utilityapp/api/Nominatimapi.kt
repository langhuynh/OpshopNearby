package au.edu.jcu.assessment.utilityapp.api

import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.cos

// BACKUP data source: Nominatim, OpenStreetMap's official search service (free, no API key).
// It is only used when all the Overpass servers fail, because Overpass gives more complete results.
// Rules: send the app's name (User-Agent, see RetrofitInstance) and at most 1 request per second.

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") text: String,              // what to look for "charity shop"
        @Query("viewbox") box: String,         // the area to look in: "west,north,east,south"
        @Query("bounded") bounded: Int = 1,    // 1 = only inside that area
        @Query("limit") limit: Int = 50,       // at most 50 results (Nominatim's maximum)
        @Query("extratags") extra: Int = 1,    // 1 = also send opening hours
        @Query("format") format: String = "jsonv2"
    ): List<NominatimPlace>
}
data class NominatimPlace(
    val osm_type: String?,
    val osm_id: Long?,
    val lat: String?,                     // Nominatim sends numbers as text
    val lon: String?,
    val category: String?,                // "shop"
    val type: String?,                    // "charity" or "second_hand"
    val name: String?,
    val extratags: Map<String, String>?   // opening_hours, website, ...
)

object NominatimQuery {
    // The area around the user, as Nominatim wants it: "west,north,east,south".
    // 1 degree of latitude is about 111 km; longitude degrees get smaller away from the equator.
    fun boxAround(lat: Double, lon: Double, radiusKm: Int): String {
        val dLat = radiusKm / 111.0
        val dLon = radiusKm / (111.0 * cos(Math.toRadians(lat)))
        return "${lon - dLon},${lat + dLat},${lon + dLon},${lat - dLat}"
    }

    // Turns Nominatim results into the same shape as the Overpass answer,
    // so ShopMapper can use them without any change.
    fun toOverpassResponse(places: List<NominatimPlace>): OverpassResponse =
        OverpassResponse(
            places
                .filter { it.category == "shop" }      // only shops
                .distinctBy { it.osm_id }              // the same shop can come back from both searches
                .map { place ->
                    val tags = mutableMapOf<String, String>()
                    tags.putAll(place.extratags.orEmpty())
                    place.name?.let { tags["name"] = it }
                    tags["shop"] = place.type ?: "second_hand"
                    OverpassElement(
                        type = place.osm_type,
                        id = place.osm_id,
                        lat = place.lat?.toDoubleOrNull(),
                        lon = place.lon?.toDoubleOrNull(),
                        center = null,
                        tags = tags
                    )
                }
        )
}