package com.example.gwt.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gwt.R
import com.example.gwt.model.User
import com.example.gwt.viewmodel.MapViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
    onNavigateToCamera: () -> Unit = {},
    user: User? = null
) {
    val tractorLocation by viewModel.tractorLocation.collectAsState()
    val distance by viewModel.distanceToTractor.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val blackspots by viewModel.blackspots.collectAsState()
    
    val villageLocation = LatLng(12.9716, 77.5946)
    var showReportDialog by remember { mutableStateOf(false) }
    var isSharingLocation by remember { mutableStateOf(false) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(villageLocation, 15f)
    }

    LaunchedEffect(isSharingLocation) {
        if (isSharingLocation && user?.role == "Driver" && user.tractorId != null) {
            while (true) {
                viewModel.startSharingLocation(
                    user.tractorId,
                    cameraPositionState.position.target.latitude,
                    cameraPositionState.position.target.longitude
                )
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (user?.role == "Driver") {
                ExtendedFloatingActionButton(
                    onClick = { isSharingLocation = !isSharingLocation },
                    containerColor = if (isSharingLocation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    icon = { Icon(if (isSharingLocation) Icons.Default.Stop else Icons.Default.PlayArrow, null) },
                    text = { Text(if (isSharingLocation) stringResource(R.string.stop_tracking) else stringResource(R.string.start_collection)) },
                    shape = CircleShape
                )
            } else {
                FloatingActionButton(
                    onClick = { showReportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.report_trash), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                // Tractor Marker
                tractorLocation?.let { location ->
                    val tractorLatLng = LatLng(location.latitude, location.longitude)
                    Marker(
                        state = rememberMarkerState(position = tractorLatLng),
                        title = stringResource(R.string.kachara_gaadi),
                        snippet = stringResource(R.string.live_location),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                // Blackspot Markers
                blackspots.forEach { spot ->
                    val spotLatLng = LatLng(spot.latitude, spot.longitude)
                    val hue = when (spot.status) {
                        "Cleaned" -> BitmapDescriptorFactory.HUE_AZURE
                        "In Progress" -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    Marker(
                        state = rememberMarkerState(key = spot.id, position = spotLatLng),
                        title = spot.status,
                        snippet = spot.description,
                        icon = BitmapDescriptorFactory.defaultMarker(hue)
                    )
                }
            }

            // Enhanced Top Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                AnimatedVisibility(
                    visible = distance != null && user?.role != "Driver",
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                ) {
                    distance?.let { d ->
                        val isNear = d < 500
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = if (isNear) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, if (isNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isNear) stringResource(R.string.tractor_nearby) else stringResource(R.string.tractor_status),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = if (d < 1000) stringResource(R.string.meters_away, d.toInt()) else stringResource(R.string.km_away, d/1000),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isNear) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (isUploading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(strokeWidth = 4.dp)
                            Spacer(Modifier.height(20.dp))
                            Text(stringResource(R.string.submitting_report), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.wait_moment), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                icon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(stringResource(R.string.confirm_report)) },
                text = { Text(stringResource(R.string.confirm_report_text)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showReportDialog = false
                            onNavigateToCamera()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.open_camera))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}
