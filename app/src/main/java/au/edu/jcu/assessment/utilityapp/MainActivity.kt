package au.edu.jcu.assessment.utilityapp

//import au.edu.jcu.assessment.utilityapp.screen.MapScreen
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.screen.MapScreen
import au.edu.jcu.assessment.utilityapp.screen.SettingsScreen
import au.edu.jcu.assessment.utilityapp.screen.UtilityScreen
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopUiState
import au.edu.jcu.assessment.utilityapp.viewmodel.ShopViewModel
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Map setup: identify the app to the OpenStreetMap tile server (fair-use rule)
        // and keep downloaded map tiles in the app's own cache folder.
        Configuration.getInstance().apply{
            userAgentValue = packageName
            osmdroidBasePath = cacheDir // set the folder where the library stores
            osmdroidTileCache = File(cacheDir, "tiles") // specific folder for download map tiles
        }

        enableEdgeToEdge()
        setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UtilityAppPreview() {
    MobileTech_UtilityAppTheme {
        UtilityApp()
    }
}

@Composable
fun UtilityApp() {
    // rememberSaveable (instead of remember) keeps the chosen tab when the phone is rotated.
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    // The shop ViewModel: shared by all the tabs, so they show the same shops.
    val shopVm: ShopViewModel = viewModel()
    val state by shopVm.state.collectAsState()
    // Android's "Allow location?" popup, used by the Utility and Map tabs.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { answers -> shopVm.onPermissionResult(answers.values.any { it }) }

    val askPermission = {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    // Ask once, when the app first opens.
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (state == ShopUiState.NeedPermission && !askedOnce) {
            askedOnce = true
            askPermission()
        }
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Utility") },
                    label = { Text("Utility") },
                    selected = selectedTab == "Utility",
                    onClick = {
                        selectedTab = "Utility"
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = selectedTab == "Map",
                    onClick = {
                        selectedTab = "Map"
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == "Settings",
                    onClick = {
                        selectedTab = "Settings"
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                "Utility" -> UtilityScreen(onAskPermission = askPermission)
                "Map" -> MapScreen(onAskPermission = askPermission)
                "Settings" -> SettingsScreen()
            }
        }
    }
}