package au.edu.jcu.assessment.utilityapp.model

// One op shop (charity or second-hand shop), built from OpenStreetMap data.
data class Shop(
    val id: String,                 // unique in OpenStreetMap, e.g. "node/123"
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,               // "charity" or "second_hand"
    val openingHours: String?,      // ? = can be missing
    val distanceKm: Double? = null  // how far from the user
) {
    val typeLabel: String
        get() = if (type == "charity") "Charity shop" else "Second-hand shop"
}