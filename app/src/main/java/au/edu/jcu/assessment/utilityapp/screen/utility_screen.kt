package au.edu.jcu.assessment.utilityapp.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.model.Distance
import au.edu.jcu.assessment.utilityapp.model.OpenStatus
import au.edu.jcu.assessment.utilityapp.model.OpeningHours
import au.edu.jcu.assessment.utilityapp.model.Shop
import au.edu.jcu.assessment.utilityapp.model.ShopFilter
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.SettingsViewModel
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopUiState
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopViewModel
// THE UTILITY TAB: op shops near the user as a list.
@Composable
fun UtilityScreen(onAskPermission: () -> Unit) {
    val view = shopResultView(onAskPermission)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ShopsHeader(title = "Thrift Finder")
            }
        }

        when (view) {
            is ResultView.Shops -> {
                item {
                    Text("${view.shown.size} shops found", style = MaterialTheme.typography.labelLarge)
                }
                items(view.shown, key = { it.id }) { shop ->
                    ShopCard(shop, OpeningHours.statusNow(shop.openingHours))
                }
                item { OsmCredit() }
            }
            else -> item { ResultMessage(view) }
        }
    }
}
// THE PIECES the screen is built from (the Map tab can use them too: same package)
// The top of the Utility and Map screens: the title and the user's location.
@Composable
fun ShopsHeader(title: String) {
    val shopVm: ShopViewModel = viewModel()
    val label by shopVm.locationLabel.collectAsState()

    Text(title, style = MaterialTheme.typography.headlineMedium)
    LocationBar(label)
}

// Shows the suburb the app found shops around.
@Composable
fun LocationBar(label: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        }
    }
}

// What the Utility and Map tabs should show right now: a loading spinner, a message, or the shops.
sealed interface ResultView {
    data class Loading(val text: String) : ResultView
    data class Message(val text: String, val button: String? = null, val onClick: () -> Unit = {}) : ResultView
    data class Shops(val shown: List<Shop>) : ResultView
}

// Works out the ResultView from the ViewModel state and the Settings.
// "shown" is the list after the open-now filter is applied, closest shop first.
@Composable
fun shopResultView(onAskPermission: () -> Unit): ResultView {
    val shopVm: ShopViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val state by shopVm.state.collectAsState()
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
            val shown = ShopFilter.apply(s.shops, settings) { OpeningHours.statusNow(it.openingHours) }
            when {
                s.shops.isEmpty() -> ResultView.Message(
                    "No op shops found within ${settings.radiusKm} km. Try a bigger distance in Settings.",
                    button = "Try again", onClick = { shopVm.refresh() }
                )
                shown.isEmpty() -> ResultView.Message(
                    "None of the ${s.shops.size} shops nearby are open right now. " +
                            "Turn off \"Open now only\" in Settings to see them all."
                )
                else -> ResultView.Shops(shown)
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

@Composable
fun ShopCard(shop: Shop, status: OpenStatus) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(shop.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(Distance.label(shop.distanceKm), style = MaterialTheme.typography.bodySmall)
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
// PREVIEWS (Android Studio only: click "Split" at the top right to see them)

@Preview(showBackground = true)
@Composable
fun LocationBarPreview() {
    MobileTech_UtilityAppTheme {
        LocationBar(label = "Brisbane, QLD")
    }
}

@Preview(showBackground = true)
@Composable
fun ShopCardPreview() {
    MobileTech_UtilityAppTheme {
        ShopCard(
            shop = Shop(
                id = "node/1", name = "Example Charity Shop", latitude = 0.0, longitude = 0.0,
                type = "charity", openingHours = "Mo-Fr 09:00-16:00", distanceKm = 0.8
            ),
            status = OpenStatus.OPEN
        )
    }
}