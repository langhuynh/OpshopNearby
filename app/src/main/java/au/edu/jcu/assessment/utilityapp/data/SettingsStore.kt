package au.edu.jcu.assessment.utilityapp.data

import android.content.Context
import android.content.SharedPreferences
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow

// Saves the Settings on the phone (SharedPreferences), so they are kept after the app is closed.
// "object" = there is only ONE SettingsStore in the whole app.
// Both ViewModels use it, so when Settings change, the Utility and Map screens change too.
object SettingsStore {

    private lateinit var prefs: SharedPreferences   // the saved file (opened in get)
    private var opened = false

    // The current settings. Screens and ViewModels watch this.
    val settings = MutableStateFlow(AppSettings())

    // Called by both ViewModels. The first time, it opens the saved file and reads the settings.
    fun get(context: Context): SettingsStore {
        if (!opened) {
            prefs = context.getSharedPreferences("op_shop_settings", Context.MODE_PRIVATE)
            settings.value = AppSettings.fromStored(
                radiusKm = prefs.getInt("radius_km", AppSettings.DEFAULT_RADIUS_KM),
                openNowOnly = prefs.getBoolean("open_now_only", false)
            )
            opened = true
        }
        return this
    }

    // Saves new settings on the phone, then tells everyone watching.
    fun save(newSettings: AppSettings) {
        prefs.edit()
            .putInt("radius_km", newSettings.radiusKm)
            .putBoolean("open_now_only", newSettings.openNowOnly)
            .apply()                    // writes the file in the background
        settings.value = newSettings
    }
}