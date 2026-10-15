package au.edu.jcu.assessment.utilityapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import au.edu.jcu.assessment.utilityapp.data.SettingsStore
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

// ViewModel for the Settings screen. Changes are saved straight away and shared with the other screens.
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore.get(application)

    // The screen watches this and redraws when a setting changes.
    val settings: StateFlow<AppSettings> = store.settings

    // copy() = the same settings, with one value changed.
    fun setRadius(km: Int) = store.save(settings.value.copy(radiusKm = km))
    fun setOpenNowOnly(on: Boolean) = store.save(settings.value.copy(openNowOnly = on))
    fun resetToDefaults() = store.save(AppSettings())
}