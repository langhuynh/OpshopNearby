package au.edu.jcu.assessment.utilityapp.data

import android.content.Context
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Saves the Settings with SharedPreferences, so they are kept after the app is closed.
// Both ViewModels use this one store: when Settings change, the Utility screen reacts straight away.
class SettingsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("op_shop_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    fun update(change: (AppSettings) -> AppSettings) {
        val newSettings = change(_settings.value)
        prefs.edit()
            .putInt(KEY_RADIUS, newSettings.radiusKm)
            .putBoolean(KEY_OPEN_NOW, newSettings.openNowOnly)
            .apply()   // saves in the background
        _settings.value = newSettings
    }
    private fun load(): AppSettings = AppSettings.fromStored(
        radiusKm = prefs.getInt(KEY_RADIUS, AppSettings.DEFAULT_RADIUS_KM),
        openNowOnly = prefs.getBoolean(KEY_OPEN_NOW, false)
    )
    companion object {
        private const val KEY_RADIUS = "radius_km"
        private const val KEY_OPEN_NOW = "open_now_only"

        @Volatile
        private var instance: SettingsStore? = null

        // One shared store for the whole app (the singleton pattern).
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}