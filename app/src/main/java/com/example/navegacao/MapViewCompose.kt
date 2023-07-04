package com.example.navegacao

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.data.Feature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.location.LocationDisplayAutoPanMode
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewCompose () {

    val context = LocalContext.current
    val mapView = MapView(context)
    val arcGISMap = remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISTopographic)) }

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

    val (showBottomSheet, setShowBottomSheet) = remember { mutableStateOf(false) }
    val (sheetText, setSheetText) = remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val (serviceFeatureTable) = remember {
        mutableStateOf(ServiceFeatureTable("https://services2.arcgis.com/beN7bc9ACZ1umhpG/arcgis/rest/services/bacia_do_doce/FeatureServer/0"))
    }

    val (featureLayer) = remember {
        mutableStateOf(FeatureLayer.createWithFeatureTable(serviceFeatureTable))
    }

    Scaffold  {  contentPadding ->

        AndroidView(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            factory = {

                mapView.also {

                    it.map = arcGISMap.value
                    it.map?.operationalLayers?.clear()
                    it.map?.operationalLayers?.add(featureLayer)
                    it.locationDisplay.setAutoPanMode(LocationDisplayAutoPanMode.CompassNavigation)

                    lifecycleOwner.lifecycle.addObserver(it)
                    lifecycleOwner.lifecycleScope.launch {

                        it.locationDisplay.dataSource.start()
                            .onFailure { requestPermission(context, launcher) }

                        it.onSingleTapConfirmed.collect { tapEvent ->

                            featureLayer.clearSelection()
                            val tolerance = 10.0
                            val identifyLayerResult = mapView.identifyLayer(featureLayer, tapEvent.screenCoordinate, tolerance, false)

                            identifyLayerResult.apply {
                                onSuccess { result ->
                                    val features = result.geoElements.filterIsInstance<Feature>()
                                    setSheetText(features.fold("") { acc, feature ->
                                        acc + feature.attributes.map { (key, value) -> "$key | $value \n" }.reduce {
                                            inAcc, cur -> inAcc + cur
                                        }
                                    })

                                    featureLayer.selectFeatures(features)
                                    setShowBottomSheet(true)
                                }
                                onFailure { error ->
                                    val errorMessage = "Select feature failed: " + error.message
                                    Log.e("Arcgis", errorMessage)
                                }
                            }
                        }
                    }
                }
            }
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    setShowBottomSheet(false)
                },
                sheetState = sheetState
            ) {
                Text(
                    modifier = Modifier.padding(
                        top = 0.dp,
                        bottom = 10.dp,
                        start = 13.dp,
                        end = 13.dp
                    ),
                    text = sheetText
                )
            }
        }

    }
    

}

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
