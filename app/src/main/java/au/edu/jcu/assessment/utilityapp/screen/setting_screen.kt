package au.edu.jcu.assessment.utilityapp.screen

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
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.SettingsViewModel

// The Settings tab. Gets the settings from the ViewModel and passes them to the layout.
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()

    SettingsContent(
        settings = settings,
        onRadius = { km -> vm.setRadius(km) },            // a chip was tapped
        onOpenNowOnly = { on -> vm.setOpenNowOnly(on) },  // the switch was flipped
        onReset = { vm.resetToDefaults() }                // Reset was tapped
    )
}

// The layout only (no ViewModel inside), so the Preview can show it with fake settings.
@Composable
fun SettingsContent(
    settings: AppSettings,
    onRadius: (Int) -> Unit,
    onOpenNowOnly: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),   // scrolls if the phone is turned sideways
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // 1. Search distance: one chip for each option (2, 5, 10, 20 km)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Search distance", style = MaterialTheme.typography.titleMedium)
            Text("How far from you to look for op shops.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (km in AppSettings.RADIUS_OPTIONS) {
                    FilterChip(
                        selected = km == settings.radiusKm,   // highlight the current distance
                        onClick = { onRadius(km) },
                        label = { Text("$km km") }
                    )
                }
            }
        }
        HorizontalDivider()

        // 2. Open now only: text on the left, switch on the right
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

        // 3. Reset to 5 km and Open now only off
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Reset to defaults")
        }

        // OpenStreetMap's licence asks for this credit
        Text(
            "Shop data © OpenStreetMap contributors, from the free Overpass API. " +
                    "Wrong or missing details can be fixed by anyone at openstreetmap.org.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    MobileTech_UtilityAppTheme {
        SettingsContent(
            settings = AppSettings(),   // fake settings: 5 km, open now only off
            onRadius = {},              // {} = do nothing (it's only a preview)
            onOpenNowOnly = {},
            onReset = {}
        )
    }
}