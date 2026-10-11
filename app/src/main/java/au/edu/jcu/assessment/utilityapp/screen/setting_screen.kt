package au.edu.jcu.assessment.utilityapp.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import au.edu.jcu.assessment.utilityapp.model.DistanceUnit
import au.edu.jcu.assessment.utilityapp.model.SortOption
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.SettingsViewModel

// Settings change how the Utility screen behaves, and are saved after the app closes.
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()

    SettingsContent(
        settings = settings,
        onRadius = vm::setRadius,
        onOpenNowOnly = vm::setOpenNowOnly,
        onSortBy = vm::setSortBy,
        onUnit = vm::setUnit,
        onReset = vm::resetToDefaults
    )
}

// The layout only, with no ViewModel inside, so it can be shown in a Preview.
@Composable
fun SettingsContent(
    settings: AppSettings,
    onRadius: (Int) -> Unit,
    onOpenNowOnly: (Boolean) -> Unit,
    onSortBy: (SortOption) -> Unit,
    onUnit: (DistanceUnit) -> Unit,
    onReset: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Section("Search distance", "How far from you to look for op shops.") {
            ChipRow(AppSettings.RADIUS_OPTIONS, settings.radiusKm, { "$it km" }, onRadius)
        }
        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Open now only", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Hide shops that are closed, or whose hours aren't known.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = settings.openNowOnly, onCheckedChange = onOpenNowOnly)
        }
        HorizontalDivider()

        Section("Sort shops by", null) {
            ChipRow(SortOption.entries, settings.sortBy, { it.label }, onSortBy)
        }
        HorizontalDivider()

        Section("Distance units", null) {
            ChipRow(DistanceUnit.entries, settings.unit, { it.label }, onUnit)
        }
        HorizontalDivider()

        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Reset to defaults")
        }

        Text(
            "Shop data © OpenStreetMap contributors, from the free Overpass API. " +
                    "Wrong or missing details can be fixed by anyone at openstreetmap.org.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Section(title: String, hint: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall)
        content()
    }
}

// A row of chips where exactly one is selected (reused by every section).
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    MobileTech_UtilityAppTheme {
        SettingsContent(AppSettings(), {}, {}, {}, {}, {})
    }
}
