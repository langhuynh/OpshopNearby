package au.edu.jcu.assessment.utilityapp.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.model.OpeningHours
import au.edu.jcu.assessment.utilityapp.model.Shop
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// =====================================================================================
// THE MAP TAB: the same shops as the Utility tab, as pins on an OpenStreetMap map.
// It uses the shared pieces in utility_screen.kt (ShopsHeader, shopResultView, OsmCredit...).
// =====================================================================================
// The Map tab: the same shops as the Utility tab, as pins on an OpenStreetMap map.
// Upright: header on top, map below. Sideways: header on the left (scrollable), map on the right,
// so the map always gets enough space.
@Composable
fun MapScreen(onAskPermission: () -> Unit) {
    val view = shopResultView(onAskPermission)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (landscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShopsHeader(title = "Shop map")
            }
            MapArea(view, Modifier.weight(0.6f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShopsHeader(title = "Shop map")
            MapArea(view, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

// The map with its hint and credit, or a loading/error message in the same space.
@Composable
private fun MapArea(view: ResultView, modifier: Modifier) {
    val shopVm: ShopViewModel = viewModel()
    val userLocation by shopVm.userLocation.collectAsState()

    if (view is ResultView.Shops) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Tap a pin to see the shop's name and whether it's open.",
                style = MaterialTheme.typography.bodySmall
            )
            ShopMap(
                shops = view.shown,
                userLocation = userLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            OsmCredit()
        }
    } else {
        Box(modifier) { ResultMessage(view) }
    }
}


// =====================================================================================
// THE MAP ITSELF (osmdroid, free OpenStreetMap tiles, no API key)
// =====================================================================================
// A free OpenStreetMap map (the osmdroid library, no API key needed).
// osmdroid is an older Android "View", so AndroidView places it inside Compose.
@Composable
fun ShopMap(
    shops: List<Shop>,
    userLocation: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)   // pinch to zoom
            controller.setZoom(14.0)
        }
    }

    // Remembers whether the map has already moved to the user,
    // so it doesn't jump back every time the list changes (for example after Refresh).
    val centred = remember { booleanArrayOf(false) }

    // osmdroid needs to know when the screen pauses and resumes.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        // clipToBounds stops the map drawing over the header and buttons above it.
        modifier = modifier.clipToBounds(),
        update = { map ->
            map.overlays.clear()

            // Move the map to the user's position (only the first time).
            userLocation?.let { (lat, lon) ->
                if (!centred[0]) {
                    map.controller.setCenter(GeoPoint(lat, lon))
                    centred[0] = true
                }
            }

            // One pin per shop. Tapping it shows its name and open status.
            shops.forEach { shop ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(shop.latitude, shop.longitude)
                    title = shop.name
                    snippet = OpeningHours.statusNow(shop.openingHours).label
                    icon = ContextCompat.getDrawable(map.context, R.drawable.ic_shop_pin)   // purple pin with a hanger
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
            map.invalidate()
        }
    )
}