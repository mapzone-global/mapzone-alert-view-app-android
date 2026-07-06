package com.mapzone.mapzonealertview.presentation.common

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import vn.vietmap.vietmapsdk.maps.MapView
import vn.vietmap.vietmapsdk.maps.VietMapGL
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun MapViewContainer(
    modifier: Modifier = Modifier,
    onMapReady: (MapView, VietMapGL) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    val destroyed = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY ->
                    if (destroyed.compareAndSet(false, true)) mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (destroyed.compareAndSet(false, true)) mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { _ ->
            mapView.getMapAsync { map -> onMapReady(mapView, map) }
            mapView
        },
    )
}
