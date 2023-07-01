package com.example.navegacao

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

fun requestPermission (context: Context, launcher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>) {
    val permissionCheckCoarseLocation =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionCheckFineLocation =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (!(permissionCheckCoarseLocation && permissionCheckFineLocation)) {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }
}

@Composable
fun MapViewCompose (arcGISMap: ArcGISMap) {

    lateinit var mapView: MapView
    val lifecycleOwner = LocalLifecycleOwner.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val isGranted = permissionsMap.values.reduce {acc, next -> acc && next }
            if (isGranted)
                lifecycleOwner.lifecycleScope.launch {
                    mapView.locationDisplay.dataSource.start()
                }
        }
    )

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).also { mapViewArg: MapView ->
                mapView = mapViewArg
                lifecycleOwner.lifecycle.addObserver(mapView)
                mapView.map = arcGISMap
                mapView.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)

                lifecycleOwner.lifecycleScope.launch {
                    mapView.locationDisplay.dataSource.start()
                        .onFailure { requestPermission(context, launcher) }
                }
            }
        }
    )
}