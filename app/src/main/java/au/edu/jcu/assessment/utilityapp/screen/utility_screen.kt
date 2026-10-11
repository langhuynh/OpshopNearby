package au.edu.jcu.assessment.utilityapp.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.model.AppSettings
import au.edu.jcu.assessment.utilityapp.model.Distance
import au.edu.jcu.assessment.utilityapp.model.DistanceUnit
import au.edu.jcu.assessment.utilityapp.model.OpenStatus
import au.edu.jcu.assessment.utilityapp.model.OpeningHours
import au.edu.jcu.assessment.utilityapp.model.Shop
import au.edu.jcu.assessment.utilityapp.model.ShopFilter
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.SettingsViewModel
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopUiState
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopViewModel

// =====================================================================================
// THE UTILITY TAB: op shops near the user as a list.
// That keeps the shops visible when the phone is turned sideways and the screen is short.
// =====================================================================================
@Composable
fun UtilityScreen(onAskPermission: () -> Unit) {
    // viewModel() gives the same ViewModel to every tab, so the list and the map always match.
    val shopVm: ShopViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val settings by settingsVm.settings.collectAsState()
    val query by shopVm.query.collectAsState()
    val view = shopResultView(onAskPermission)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShopsHeader(title = "Opshop NearBy")
            }
        }

        when (view) {
            is ResultView.Shops -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (query.isBlank()) "${view.shown.size} shops found"
                            else "${view.shown.size} of ${view.all.size} shops match",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { shopVm.refresh() }) { Text("Refresh") }
                    }
                }
                items(view.shown, key = { it.id }) { shop ->
                    ShopCard(shop, OpeningHours.statusNow(shop.openingHours), settings.unit) {
                        shopVm.selectShop(shop)
                    }
                }
                item { OsmCredit() }
            }
            else -> item { ResultMessage(view) }
        }
    }
}


// =====================================================================================
// THE PIECES the screen is built from (the Map tab can use them too: same package)
// =====================================================================================
// The top of the Utility and Map screens: title, the user's location, and the search box.
@Composable
fun ShopsHeader(title: String) {
    val shopVm: ShopViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val label by shopVm.locationLabel.collectAsState()
    val query by shopVm.query.collectAsState()
    val settings by settingsVm.settings.collectAsState()

    Text(title, style = MaterialTheme.typography.headlineMedium)
    LocationBar(label, onUpdate = { shopVm.refresh() })
    Text(
        settingsSummary(settings),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SearchField(query, shopVm::setQuery)
}

// Shows where the app is searching from. Update gets the location again and searches.
@Composable
fun LocationBar(label: String?, onUpdate: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Your location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    label ?: "Not set yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = onUpdate) { Text("Update") }
        }
    }
}

// Searches the shops already found, so it needs no extra internet request.
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or suburb") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

// What the Utility and Map tabs should show right now: a loading spinner, a message, or the shops.
sealed interface ResultView {
    data class Loading(val text: String) : ResultView
    data class Message(val text: String, val button: String? = null, val onClick: () -> Unit = {}) : ResultView
    data class Shops(val all: List<Shop>, val shown: List<Shop>) : ResultView
}

// Works out the ResultView from the ViewModel state, the search box and the Settings.
// "shown" is the list after the search text, the open-now filter and the sort order are applied.
@Composable
fun shopResultView(onAskPermission: () -> Unit): ResultView {
    val shopVm: ShopViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val state by shopVm.state.collectAsState()
    val query by shopVm.query.collectAsState()
    val settings by settingsVm.settings.collectAsState()

    return when (val s = state) {
        ShopUiState.NeedPermission -> ResultView.Message(
            "Allow location so the app can find op shops near you.",
            button = "Allow location", onClick = onAskPermission
        )
        ShopUiState.Locating -> ResultView.Loading("Finding your location…")
        is ShopUiState.Loading -> ResultView.Loading(s.message)
        is ShopUiState.Error -> ResultView.Message(s.message, button = "Try again", onClick = { shopVm.refresh() })
        is ShopUiState.Success -> {
            val shown = ShopFilter.apply(s.shops, settings, query) { OpeningHours.statusNow(it.openingHours) }
            when {
                s.shops.isEmpty() -> ResultView.Message(
                    "No op shops found within ${settings.radiusKm} km. Try a bigger distance in Settings.",
                    button = "Search again", onClick = { shopVm.refresh() }
                )
                shown.isEmpty() && query.isNotBlank() -> ResultView.Message(
                    "No shops match \"${query.trim()}\".",
                    button = "Clear search", onClick = { shopVm.setQuery("") }
                )
                shown.isEmpty() -> ResultView.Message(
                    "None of the ${s.shops.size} shops nearby are open right now. " +
                            "Turn off \"Open now only\" in Settings to see them all."
                )
                else -> ResultView.Shops(s.shops, shown)
            }
        }
    }
}

// Draws a Loading or Message view. (Shops are drawn by each tab in its own way.)
@Composable
fun ResultMessage(view: ResultView) {
    when (view) {
        is ResultView.Loading -> LoadingBox(view.text)
        is ResultView.Message -> MessageBox(view.text, view.button, view.onClick)
        is ResultView.Shops -> {}
    }
}

// OpenStreetMap's licence asks for this credit wherever its data is shown.
@Composable
fun OsmCredit() {
    Text("Data © OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall)
}

fun settingsSummary(s: AppSettings): String =
    "Within ${s.radiusKm} km · ${s.sortBy.label}" + if (s.openNowOnly) " · Open now only" else ""

@Composable
fun ShopCard(shop: Shop, status: OpenStatus, unit: DistanceUnit, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(shop.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(Distance.label(shop.distanceKm, unit), style = MaterialTheme.typography.bodySmall)
            }
            Text(shop.typeLabel, style = MaterialTheme.typography.bodySmall)
            Text(status.label, color = statusColor(status), style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun statusColor(status: OpenStatus): Color = when (status) {
    OpenStatus.OPEN -> Color(0xFF2E7D32)    // green
    OpenStatus.CLOSED -> Color(0xFFC62828)  // red
    OpenStatus.UNKNOWN -> Color.Gray
}

@Composable
fun LoadingBox(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text)
    }
}

@Composable
fun MessageBox(text: String, button: String? = null, onClick: () -> Unit = {}) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text)
            if (button != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClick) { Text(button) }
            }
        }
    }
}

// =====================================================================================
// PREVIEWS (Android Studio only: click "Split" at the top right to see them)
// =====================================================================================

@Preview(showBackground = true)
@Composable
fun LocationBarPreview() {
    MobileTech_UtilityAppTheme {
        LocationBar(label = "Brisbane, QLD", onUpdate = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ShopCardPreview() {
    MobileTech_UtilityAppTheme {
        ShopCard(
            shop = Shop(
                id = "node/1", name = "Example Charity Shop", latitude = 0.0, longitude = 0.0,
                type = "charity", openingHours = "Mo-Fr 09:00-16:00", address = null,
                website = null, phone = null, distanceKm = 0.8
            ),
            status = OpenStatus.OPEN,
            unit = DistanceUnit.KM,
            onClick = {}
        )
    }
}