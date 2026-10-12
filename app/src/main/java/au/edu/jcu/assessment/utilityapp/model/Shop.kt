package au.edu.jcu.assessment.utilityapp.model

// One op shop (charity or second-hand shop), built from OpenStreetMap data.
data class Shop(
    val id: String,                 // unique in OpenStreetMap
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,               // "charity" or "second_hand"
    val openingHours: String?,      // ? = can be missing
    val address: String?,
    val website: String?,
    val phone: String?,
    val distanceKm: Double? = null  // filled in once we know where the user is
) {
    val typeLabel: String
        get() = if (type == "charity") "Charity shop" else "Second-hand shop"
}