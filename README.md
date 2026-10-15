# Thrift Finder – MobileTech Utility App

This is an Android utility app for **Assessment 1: Utility App**.
It finds **op shops (charity and second-hand shops) near you**, shows how far away they are and whether they are open now, using **Jetpack Compose** and **Material Design 3**.

---

## Getting Started

### How to Run
1. Clone or download this repo  
2. Open in Android Studio  
3. Run on an emulator or physical device (API 26+ recommended) with internet  
4. Allow location when the app asks (on an emulator, set a location in **⋮ → Location** first)  

---

## Composables

### UtilityApp()
- Contains the screen layout using a Scaffold
- Toggles content between Utility, Map and Settings
- Asks for the location permission when the app opens

### UtilityScreen()
- Displays the op shops near you, closest first
- Each card shows the name, type, distance and open status (Open now / Closed now / Hours unknown)

### MapScreen()
- Displays the same shops as pins on an OpenStreetMap map
- Tap a pin to see the shop's name and whether it is open

### SettingsScreen()
- Search distance: 2, 5, 10 or 20 km
- Open now only: hides closed shops
- Reset to defaults (settings are saved after the app closes)

---

## Project Structure

```
app/src/main/java/au/edu/jcu/assessment/utilityapp/
├── MainActivity.kt            App start, location permission, bottom tabs
├── api/
│   ├── OverpassApi.kt         Retrofit interface, the Overpass question, the 3 servers
│   └── OverpassModels.kt      Shape of the JSON answer
├── data/
│   └── SettingsStore.kt       Saves settings (SharedPreferences)
├── model/
│   ├── AppSettings.kt         The 2 settings and their defaults
│   ├── Distance.kt            Haversine distance + "1.5 km away" text
│   ├── OpeningHours.kt        Open now / Closed now / Hours unknown
│   ├── PlaceName.kt           "Brisbane City, QLD"
│   ├── Shop.kt                One op shop
│   ├── ShopFilter.kt          "Open now only" filter
│   └── ShopMapper.kt          API answer → list of Shop
├── screen/
│   ├── utility_screen.kt      Utility tab (list) + shared pieces (header, cards, messages)
│   ├── Map_screen.kt          Map tab (osmdroid)
│   └── setting_screen.kt      Settings tab
├── ui/theme/                  Colours and fonts (template)
└── viewmodel/
    ├── ShopViewModel.kt       Location, API request, screen state
    └── SettingsViewModel.kt   Settings screen logic
```

## Key Concepts Covered

| Week | Concept                        | Used In                          |
|------|--------------------------------|----------------------------------|
| 1    | Kotlin + Android Studio        | MainActivity.kt, model/ |
| 2    | Jetpack Compose Layouts        | UtilityApp(), UtilityScreen(), MapScreen(), SettingsScreen() |
| 3    | Material Design 3              | MobileTech_UtilityAppTheme, MaterialTheme.typography |
| 4    | ViewModel                      | ShopViewModel, SettingsViewModel |
| 5    | Retrofit                       | OverpassApi (OpenStreetMap Overpass API) |


## Suggested Extensions
- Tap a shop to see its address, phone and website  
- Open directions to a shop in Google Maps  
- Save favourite shops  

---

## 📚 License  
Shop and map data © OpenStreetMap contributors.

