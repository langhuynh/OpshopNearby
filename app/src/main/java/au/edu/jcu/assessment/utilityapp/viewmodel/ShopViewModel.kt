package au.edu.jcu.assessment.utilityapp.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.api.OverpassQuery
import au.edu.jcu.assessment.utilityapp.api.OverpassResponse
import au.edu.jcu.assessment.utilityapp.api.RetrofitInstance
import au.edu.jcu.assessment.utilityapp.data.SettingsStore
import au.edu.jcu.assessment.utilityapp.model.PlaceName
//import au.edu.jcu.assessment.utilityapp.model.PlaceName
import au.edu.jcu.assessment.utilityapp.model.Shop
import au.edu.jcu.assessment.utilityapp.model.ShopMapper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlin.coroutines.resume

// Everything the Utility and Map screens can be showing at one moment.
sealed interface ShopUiState {
    data object NeedPermission : ShopUiState
    data object Locating : ShopUiState
    data class Loading(val message: String) : ShopUiState
    data class Success(val shops: List<Shop>) : ShopUiState
    data class Error(val message: String) : ShopUiState
}

// One ViewModel shared by the Utility and Map tabs, so both show the same shops and location.
// It keeps everything when the screen rotates or the user switches tabs.
class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore.get(application)
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    // MutableStateFlow: a value the screens watch. When it changes, they redraw.
    val state = MutableStateFlow<ShopUiState>(ShopUiState.NeedPermission)

    val userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    // The place name shown at the top of the screens, e.g. "Kirwan, QLD".
    val locationLabel = MutableStateFlow<String?>(null)

    // The search that is running now. A new search cancels it, so old answers can't pile up
    // or arrive late and replace the newer results.
    private var searchJob: Job? = null

    // The last search that worked, so a SMALLER distance can be shown straight away
    // by filtering these shops, without asking the server again.
    private var lastShops: List<Shop> = emptyList()
    private var lastRadiusKm = 0
    private var lastSearchedAt: Pair<Double, Double>? = null

    // The server that answered last time. The next search asks it first,
    // so the app doesn't keep waiting for a server that is down.
    private var lastGoodServer = 0

    init {
        if (hasPermission()) refresh()

        // When the search distance changes in Settings, search again automatically.
        // "Open now only" just filters the shops already loaded, so it needs no request.
        viewModelScope.launch {
            settingsStore.settings
                .map { it.radiusKm }
                .distinctUntilChanged()
                .drop(1)   // skip the value at start-up; refresh() above already searches
                .collect { searchAgain() }
        }
    }


    fun onPermissionResult(granted: Boolean) {
        if (granted) refresh()
        else state.value = ShopUiState.Error("Location permission is needed to find op shops near you.")
    }

    // Searches again from the location we already have (used when the distance changes).
    // Getting a new GPS fix is slow, and the user hasn't moved, so it is skipped.
    private fun searchAgain() {
        val location = userLocation.value ?: return refresh()
        val radiusKm = settingsStore.settings.value.radiusKm

        // Smaller distance, same place: the shops are already loaded, so just filter them. No waiting.
        if (location == lastSearchedAt && radiusKm <= lastRadiusKm) {
            state.value = ShopUiState.Success(lastShops.filter { (it.distanceKm ?: 0.0) <= radiusKm })
            return
        }
        search(location.first, location.second)
    }

    // Gets the user's location, then searches.
    @SuppressLint("MissingPermission")   // checked just below
    fun refresh() {
        if (!hasPermission()) {
            state.value = ShopUiState.NeedPermission
            return
        }
        state.value = ShopUiState.Locating

        // Don't wait for the GPS forever: after 10 seconds, give up and use the last known location.
        val cancelGps = CancellationTokenSource()
        val gpsTimeout = viewModelScope.launch {
            delay(10_000)
            cancelGps.cancel()
        }
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelGps.token)
            .addOnCompleteListener { task ->
                gpsTimeout.cancel()
                val location = if (task.isSuccessful) task.result else null
                if (location != null) onGpsLocation(location.latitude, location.longitude)
                else useLastLocation()
            }
    }

    // No fresh location (common on emulators, or the GPS was too slow), so try the last known one.
    @SuppressLint("MissingPermission")   // only called from refresh(), after the check
    private fun useLastLocation() {
        locationClient.lastLocation
            .addOnSuccessListener { last ->
                if (last != null) onGpsLocation(last.latitude, last.longitude)
                else state.value = ShopUiState.Error(
                    "Couldn't find your location. Turn on location, then tap Try again."
                )
            }
            .addOnFailureListener {
                state.value = ShopUiState.Error("Couldn't find your location.")
            }
    }

    private fun onGpsLocation(lat: Double, lon: Double) {
        locationLabel.value = "Finding your suburb…"
        viewModelScope.launch {
            locationLabel.value = placeName(lat, lon) ?: PlaceName.coordinates(lat, lon)
        }
        search(lat, lon)
    }

    // viewModelScope.launch starts a coroutine that is cancelled automatically when the ViewModel is destroyed.
    private fun search(lat: Double, lon: Double) {
        searchJob?.cancel()      // stop the old search, if one is still waiting for the server
        userLocation.value = lat to lon
        state.value = ShopUiState.Loading("Searching OpenStreetMap…")
        val radiusKm = settingsStore.settings.value.radiusKm
        val apiQuery = OverpassQuery.opShopsAround(lat, lon, radiusKm * 1000)

        searchJob = viewModelScope.launch {
            // If the answer is slow, tell the user the app is still working.
            val slowNotice = launch {
                delay(6_000)
                state.value = ShopUiState.Loading("Still searching… the free map server is busy, so this can take a little longer.")
            }
            val result = try {
                ShopUiState.Success(ShopMapper.toShops(fetch(apiQuery), lat, lon))
            } catch (e: CancellationException) {
                throw e   // a newer search replaced this one: stop quietly, don't show an error
            } catch (e: UnknownHostException) {
                ShopUiState.Error("No internet connection. Check your connection and try again.")
            } catch (e: SocketTimeoutException) {
                ShopUiState.Error("The map server took too long to answer. Try again in a minute.")
            } catch (e: HttpException) {
                // 429 = too many requests, 5xx (500, 502, 503, 504) = the server has a problem
                if (e.code() == 429 || e.code() >= 500) {
                    ShopUiState.Error("The free map servers are busy right now. Try again in a minute.")
                } else {
                    ShopUiState.Error("The map server returned an error (${e.code()}). Try again later.")
                }
            } catch (e: Exception) {
                ShopUiState.Error("Something went wrong: ${e.message ?: "unknown error"}")
            }
            slowNotice.cancel()
            if (result is ShopUiState.Success) {
                // Remember it, so a smaller distance later needs no new request.
                lastShops = result.shops
                lastRadiusKm = radiusKm
                lastSearchedAt = lat to lon
            }
            state.value = result
        }
    }

    // Tries each free server in turn (see RetrofitInstance.servers) until one answers,
    // starting with the one that worked last time. If they all fail, the last error is shown.
    private suspend fun fetch(query: String): OverpassResponse {
        val servers = RetrofitInstance.servers
        var lastError: Exception? = null
        for (i in servers.indices) {
            val index = (lastGoodServer + i) % servers.size   // e.g. start at 1: try 1, 2, then 0
            try {
                val answer = servers[index].search(query)
                lastGoodServer = index     // next time, ask this server first
                return answer
            } catch (e: CancellationException) {
                throw e                    // a newer search replaced this one
            } catch (e: UnknownHostException) {
                throw e                    // no internet: the other servers won't work either
            } catch (e: Exception) {
                lastError = e              // this server is busy or broken: try the next one
            }
        }
        throw lastError ?: Exception("No map server answered")
    }

    // Asks Android's built-in Geocoder (free, no key) for the suburb name. Returns null if it can't.
    private suspend fun placeName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        val address: Address? = try {
            withTimeoutOrNull(5_000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ uses a listener instead of blocking.
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
            }
        } catch (e: Exception) {
            null   // no internet, or the Geocoder service isn't available
        }
        address?.let { PlaceName.format(it.subLocality ?: it.locality, it.subAdminArea, it.adminArea) }
    }

    private fun hasPermission(): Boolean {
        val app = getApplication<Application>()
        return listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED }
    }
}