package au.edu.jcu.assessment.utilityapp.screen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.SettingsViewModel

// The Settings tab. Gets the settings from the ViewModel and passes them to the layout.
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()

    // Is the phone turned sideways?
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    SettingsContent(
        settings = settings,
        landscape = landscape,
        onRadius = { km -> vm.setRadius(km) },            // a chip was tapped
        onOpenNowOnly = { on -> vm.setOpenNowOnly(on) },  // the switch was flipped
        onReset = { vm.resetToDefaults() }                // Reset was tapped
    )
}

// The layout only (no ViewModel inside), so the Previews can show it with fake settings.
//   Sideways (landscape):  2 columns: settings on the left, Reset + credit on the right.
@Composable
fun SettingsContent(
    settings: AppSettings,
    landscape: Boolean,
    onRadius: (Int) -> Unit,
    onOpenNowOnly: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    if (landscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left column (60%): the title and the 2 settings
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .verticalScroll(rememberScrollState()),   // scrolls if it doesn't fit
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                DistanceSetting(settings, onRadius)
                HorizontalDivider()
                OpenNowSetting(settings, onOpenNowOnly)
            }
            // Right column (40%): Reset, the credit, and the picture filling the rest
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ResetAndCredit(onReset)
                ShopPicture(Modifier.weight(1f))   // weight(1f) = take all the space that is left
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            DistanceSetting(settings, onRadius)
            HorizontalDivider()
            OpenNowSetting(settings, onOpenNowOnly)
            HorizontalDivider()
            ResetAndCredit(onReset)
            ShopPicture(Modifier.weight(1f))   // weight(1f) = take all the space that is left
        }
    }
}
// 1. Search distance: one chip for each option (2, 5, 10, 20 km)
@Composable
private fun DistanceSetting(settings: AppSettings, onRadius: (Int) -> Unit) {
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
}

// 2. Open now only: text on the left, switch on the right
@Composable
private fun OpenNowSetting(settings: AppSettings, onOpenNowOnly: (Boolean) -> Unit) {
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
}

// 3. Reset button and the OpenStreetMap credit (its licence asks for this)
@Composable
private fun ResetAndCredit(onReset: () -> Unit) {
    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text("Reset to defaults")
    }
    Text(
        "Shop data © OpenStreetMap contributors, from the free Overpass API. " +
                "Wrong or missing details can be fixed by anyone at openstreetmap.org.",
        style = MaterialTheme.typography.bodySmall
    )
}

// 4. A picture of an op shop (res/drawable/thrift_shop.png).
@Composable
private fun ShopPicture(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)   // centre them
    ) {
        Image(
            painter = painterResource(id = R.drawable.thrift_shop),
            contentDescription = "Drawing of an op shop",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)  // as big as it needs, but never bigger than the free space
        )
        Text(
            "Happy thrifting! Every op shop purchase helps a charity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
// PREVIEWS (Android Studio only): one upright, one sideways

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun SettingsPreview() {
    MobileTech_UtilityAppTheme {
        SettingsContent(
            settings = AppSettings(),   // fake settings: 5 km, open now only off
            landscape = false,
            onRadius = {},              // {} = do nothing (it's only a preview)
            onOpenNowOnly = {},
            onReset = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
fun SettingsLandscapePreview() {
    MobileTech_UtilityAppTheme {
        SettingsContent(
            settings = AppSettings(),
            landscape = true,
            onRadius = {},
            onOpenNowOnly = {},
            onReset = {}
        )
    }
}