package au.edu.jcu.assessment.utilityapp.model

class Shop(
    val id: String,             // unique in OpenStreetMap
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,           // charity or second_hand
    val openingHours: String,
    val website: String,
    val phone: String,
    val distanceKm: Double? = null // filled in once we know where the user is
) {
    val typeLabel: String
        get() = when (type) {
            "charity" -> "Charity"
            "second_hand" -> "Second Hand"
            else -> "Unknown"
        }
}