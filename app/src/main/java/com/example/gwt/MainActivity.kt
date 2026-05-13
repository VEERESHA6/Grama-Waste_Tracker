package com.example.gwt

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.gwt.screens.AdminScreen
import com.example.gwt.screens.CameraScreen
import com.example.gwt.screens.MapScreen
import com.example.gwt.screens.ReportDetailScreen
import com.example.gwt.screens.WasteGuideScreen
import com.example.gwt.screens.auth.LoginScreen
import com.example.gwt.screens.auth.RegisterScreen
import com.example.gwt.ui.theme.GwtTheme
import com.example.gwt.viewmodel.AuthViewModel
import com.example.gwt.viewmodel.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Map : Screen("map", R.string.nav_tracker, Icons.Default.Home)
    object Guide : Screen("guide", R.string.nav_guide, Icons.Default.Info)
    object Admin : Screen("admin", R.string.nav_admin, Icons.Default.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val mapViewModel: MapViewModel = viewModel()
            val navController = rememberNavController()
            val currentUser by authViewModel.currentUser.collectAsState()
            
            var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

            GwtTheme {
                if (currentUser == null) {
                    Box {
                        NavHost(navController, startDestination = "login") {
                            composable("login") { 
                                LoginScreen(
                                    onNavigateToRegister = { navController.navigate("register") },
                                    viewModel = authViewModel
                                ) 
                            }
                            composable("register") { 
                                RegisterScreen(
                                    onNavigateToLogin = { navController.navigate("login") },
                                    viewModel = authViewModel
                                ) 
                            }
                        }
                        LanguageFab(modifier = Modifier.padding(16.dp).statusBarsPadding())
                    }
                } else {
                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.CAMERA
                        )
                    )

                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (permissionsState.allPermissionsGranted) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let { mapViewModel.updateUserLocation(it) }
                            }
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    }

                    if (permissionsState.allPermissionsGranted) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route
                                
                                // Only show bottom bar on main screens
                                if (currentRoute in listOf(Screen.Map.route, Screen.Guide.route, Screen.Admin.route)) {
                                    NavigationBar {
                                        val items = mutableListOf(Screen.Map, Screen.Guide)
                                        if (currentUser?.role == "Admin") {
                                            items.add(Screen.Admin)
                                        }
                                        
                                        items.forEach { screen ->
                                            NavigationBarItem(
                                                icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                                                label = { Text(stringResource(screen.labelRes)) },
                                                selected = currentRoute == screen.route,
                                                onClick = {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(navController, startDestination = Screen.Map.route, modifier = Modifier.padding(innerPadding)) {
                                composable(Screen.Map.route) {
                                    MapScreen(
                                        viewModel = mapViewModel,
                                        onNavigateToCamera = { navController.navigate("camera") },
                                        user = currentUser
                                    )
                                }
                                composable(Screen.Guide.route) {
                                    WasteGuideScreen(
                                        onBack = { navController.popBackStack() },
                                        authViewModel = authViewModel
                                    )
                                }
                                composable(Screen.Admin.route) {
                                    AdminScreen()
                                }
                                composable("camera") {
                                    CameraScreen(
                                        onImageCaptured = { uri ->
                                            capturedImageUri = uri
                                            navController.navigate("report_detail")
                                        },
                                        onError = {
                                            Toast.makeText(this@MainActivity, "Camera Error!", Toast.LENGTH_SHORT).show()
                                        },
                                        onClose = { navController.popBackStack() }
                                    )
                                }
                                composable("report_detail") {
                                    capturedImageUri?.let { uri ->
                                        ReportDetailScreen(
                                            imageUri = uri,
                                            onConfirm = { desc ->
                                                mapViewModel.reportBlackspot(
                                                    imageUri = uri,
                                                    description = desc,
                                                    onSuccess = {
                                                        navController.popBackStack("map", inclusive = false)
                                                        Toast.makeText(this@MainActivity, "Reported Successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = {
                                                        Toast.makeText(this@MainActivity, "Upload Failed!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            },
                                            onCancel = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Column(Modifier.padding(innerPadding).padding(32.dp)) {
                                Greeting(name = stringResource(R.string.perm_msg))
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                                    Text(stringResource(R.string.grant_perm))
                                }
                                Spacer(Modifier.height(16.dp))
                                LanguageFab()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageFab(modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.change_language))
        }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("English") },
                onClick = {
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                    AppCompatDelegate.setApplicationLocales(appLocale)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("ಕನ್ನಡ (Kannada)") },
                onClick = {
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("kn")
                    AppCompatDelegate.setApplicationLocales(appLocale)
                    showMenu = false
                }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = name, modifier = modifier, style = MaterialTheme.typography.bodyLarge)
}
