package au.edu.jcu.assessment.utilityapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import au.edu.jcu.assessment.utilityapp.data.SettingsStore
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

// ViewModel for the Settings screen. Changes are saved straight away and shared with the Utility screen.
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    // StateFlow: the screen redraws whenever a setting changes.
    val settings: StateFlow<AppSettings> = store.settings

    fun setRadius(km: Int) = store.update { it.copy(radiusKm = km) }
    fun setOpenNowOnly(on: Boolean) = store.update { it.copy(openNowOnly = on) }
    fun resetToDefaults() = store.update { AppSettings() }
}