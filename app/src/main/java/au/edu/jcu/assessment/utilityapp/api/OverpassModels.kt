package au.edu.jcu.assessment.utilityapp.api
data class OverpassResponse(
    val elements: List<OverpassElement>?
)

// One shop.
data class OverpassElement(
    val type: String?,
    val id: Long?,
    val lat: Double?,                    // a shop drawn as a dot on the map has lat/lon here...
    val lon: Double?,
    val center: OverpassCenter?,         // a shop drawn as a building has its middle point here
    val tags: Map<String, String>?       // name, shop type, opening hours, address...
)

// The middle point of a building.
data class OverpassCenter(
    val lat: Double?,
    val lon: Double?
)