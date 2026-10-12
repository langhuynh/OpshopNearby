package au.edu.jcu.assessment.utilityapp.api

// THIS FILE = the ANSWER that comes back from the link in OverpassApi.kt.
//
// The server sends back JSON like this:
//
//   {
//     "elements": [
//       { "type": "node", "id": 123, "lat": -27.47, "lon": 153.02,
//         "tags": { "name": "Vinnies", "shop": "charity", "opening_hours": "Mo-Fr 09:00-17:00" } },
//       { "type": "way", "id": 456, "center": { "lat": -27.48, "lon": 153.01 },
//         "tags": { "name": "Lifeline Shop", "shop": "charity" } }
//     ]
//   }
//
// Gson copies it into the classes below. The names must match the JSON names exactly.
// Everything has "?" (can be null) because some shops are missing details.

// The whole answer: a list of shops.
data class OverpassResponse(
    val elements: List<OverpassElement>?
)

// One shop.
data class OverpassElement(
    val type: String?,
    val id: Long?,
    val lat: Double?,                    // a shop drawn as a dot on the map has lat/lon here...
    val lon: Double?,
    val center: OverpassCenter?,         // ...a shop drawn as a building has its middle point here
    val tags: Map<String, String>?       // name, shop type, opening hours, address...
)

// The middle point of a building.
data class OverpassCenter(
    val lat: Double?,
    val lon: Double?
)