package com.mapzone.mapzonealertview.presentation.feature.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapzone.mapzonealertview.R
import com.mapzone.mapzonealertview.config.AppConfig
import com.mapzone.mapzonealertview.core.util.ReportUtils
import com.mapzone.mapzonealertview.core.util.drawableToBitmap
import com.mapzone.mapzonealertview.domain.model.VehicleProfile
import com.mapzone.mapzonealertview.permission.rememberLocationPermissionState
import com.mapzone.mapzonealertview.presentation.common.MapViewContainer
import com.mapzone.mapzonealertview.presentation.common.SpeedSignOverlay
import com.mapzone.mapzonealertview.presentation.controllers.NavigationController
import com.mapzone.mapzonealertview.presentation.controllers.SpeedAlertController
import com.mapzone.mapzonealertview.presentation.feature.map.components.BottomActionBar
import com.mapzone.mapzonealertview.presentation.feature.map.components.MapControlCluster
import com.mapzone.mapzonealertview.presentation.feature.search.SearchBar
import com.mapzone.mapzonealertview.presentation.feature.map.components.SettingsDialog
import com.mapzone.mapzonealertview.presentation.feature.map.components.VoiceSettingsDialog
import com.vietmap.alert_view_sdk.VoiceAlertType
import com.mapzone.mapzonealertview.presentation.feature.search.SearchViewModel
import kotlinx.coroutines.launch
import vn.vietmap.services.android.navigation.ui.v5.route.NavigationMapRoute
import vn.vietmap.vietmapsdk.camera.CameraPosition
import vn.vietmap.vietmapsdk.camera.CameraUpdateFactory
import vn.vietmap.vietmapsdk.geometry.LatLng
import vn.vietmap.vietmapsdk.geometry.LatLngBounds
import vn.vietmap.vietmapsdk.location.LocationComponentActivationOptions
import vn.vietmap.vietmapsdk.location.LocationComponentOptions
import vn.vietmap.vietmapsdk.location.modes.CameraMode
import vn.vietmap.vietmapsdk.location.modes.RenderMode
import vn.vietmap.vietmapsdk.maps.MapView
import vn.vietmap.vietmapsdk.maps.Style
import vn.vietmap.vietmapsdk.maps.VietMapGL
import vn.vietmap.vietmapsdk.plugins.annotation.SymbolManager
import vn.vietmap.vietmapsdk.plugins.annotation.SymbolOptions

private const val DESTINATION_ICON = "destination-pin"

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavigationController,
    speedAlert: SpeedAlertController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val searchVm: SearchViewModel = viewModel()

    val permission = rememberLocationPermissionState()
    val navState by navController.state.collectAsStateWithLifecycle()
    val alertState by speedAlert.state.collectAsStateWithLifecycle()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var vietMap by remember { mutableStateOf<VietMapGL?>(null) }
    var routeRenderer by remember { mutableStateOf<NavigationMapRoute?>(null) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }

    var vehicleProfile by remember { mutableStateOf(VehicleProfile()) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var mutedVoiceTypes by remember { mutableStateOf(emptySet<VoiceAlertType>()) }

    DisposableEffect(Unit) {
        onDispose {
            vietMap = null
            mapView = null
            routeRenderer = null
            symbolManager = null
        }
    }

    LaunchedEffect(navState.lastLocation) {
        navState.lastLocation?.let { searchVm.focus = it.latitude to it.longitude }
    }

    LaunchedEffect(navState.snappedLocation) {
        val snapped = navState.snappedLocation ?: return@LaunchedEffect
        val map = vietMap ?: return@LaunchedEffect
        runCatching {
            if (!map.locationComponent.isLocationComponentActivated) return@runCatching
            map.locationComponent.forceLocationUpdate(snapped)
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val isNavigating = navState.isNavigating
    val hasRoute = navState.currentRoute != null

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { mv, map ->
                mapView = mv
                vietMap = map
                val density = mv.resources.displayMetrics.density
                map.uiSettings.compassGravity = Gravity.CENTER_VERTICAL or Gravity.END
                map.uiSettings.setCompassMargins(
                    0,
                    (statusBarTop.value * density).toInt() + (16 * density).toInt(),
                    (16 * density).toInt(),
                    0,
                )
                map.uiSettings.setCompassFadeFacingNorth(true)
                map.setStyle(Style.Builder().fromUri(AppConfig.styleUrl)) { style ->
                    drawableToBitmap(context, R.drawable.ic_destination_pin)?.let { bmp ->
                        style.addImage(DESTINATION_ICON, bmp)
                    }
                    symbolManager = SymbolManager(mv, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                    }
                    routeRenderer = NavigationMapRoute(null, mv, map)
                    if (permission.granted) {
                        enableLocationComponent(context, map, style)
                    }
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == VietMapGL.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            focusManager.clearFocus()
                            navController.setFollowing(false)
                            if (map.locationComponent.isLocationComponentActivated) {
                                map.locationComponent.cameraMode = CameraMode.NONE
                            }
                        }
                    }
                    map.addOnMapLongClickListener {
                        navController.setDestination(Point.fromLngLat(it.longitude, it.latitude))
                        true
                    }
                }
            },
        )

        LaunchedEffect(permission.granted, vietMap) {
            val map = vietMap ?: return@LaunchedEffect
            if (!permission.granted) return@LaunchedEffect
            map.getStyle { style -> enableLocationComponent(context, map, style) }
        }

        LaunchedEffect(
            navState.isNavigating,
            vietMap,
            mapView,
            permission.granted,
        ) {
            val map = vietMap ?: return@LaunchedEffect
            val mv = mapView ?: return@LaunchedEffect
            if (!permission.granted) return@LaunchedEffect
            runCatching {
                if (!map.locationComponent.isLocationComponentActivated) return@runCatching
                if (navState.isNavigating) {
                    val topPad = (mv.height * 0.4).toInt()
                    map.setPadding(0, topPad, 0, 0)
                    map.locationComponent.renderMode = RenderMode.GPS
                } else {
                    map.setPadding(0, 0, 0, 0)
                    map.locationComponent.renderMode = RenderMode.COMPASS
                }
            }
        }

        LaunchedEffect(navState.currentRoute) {
            val r = navState.currentRoute ?: run {
                routeRenderer?.removeRoute(); return@LaunchedEffect
            }
            routeRenderer?.removeRoute()
            routeRenderer?.addRoute(r)
        }

        LaunchedEffect(navState.destination, symbolManager) {
            val mgr = symbolManager ?: return@LaunchedEffect
            mgr.deleteAll()
            val dest = navState.destination ?: return@LaunchedEffect
            mgr.create(
                SymbolOptions()
                    .withLatLng(LatLng(dest.latitude(), dest.longitude()))
                    .withIconImage(DESTINATION_ICON)
                    .withIconSize(0.9f)
                    .withIconAnchor("bottom"),
            )
        }

        AnimatedVisibility(
            visible = !isNavigating,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = statusBarTop + 12.dp,
                    bottom = 12.dp,
                ),
        ) {
            SearchBar(
                viewModel = searchVm,
                onPickSuggestion = { item ->
                    focusManager.clearFocus()
                    scope.launch {
                        val entry = searchVm.resolveAndRemember(item)
                        if (entry != null) {
                            navController.setDestination(Point.fromLngLat(entry.lng, entry.lat))
                            searchVm.clearQuery()
                            vietMap?.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(entry.lat, entry.lng))
                                        .zoom(15.5).build()
                                ),
                                600,
                            )
                        }
                    }
                },
                onPickHistory = { entry ->
                    focusManager.clearFocus()
                    searchVm.pickFromHistory(entry)
                    searchVm.clearQuery()
                    navController.setDestination(Point.fromLngLat(entry.lng, entry.lat))
                    vietMap?.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(entry.lat, entry.lng))
                                .zoom(15.5).build()
                        ),
                        600,
                    )
                },
            )
        }

        SpeedSignOverlay(
            current = alertState.current,
            next = alertState.next,
            camera = alertState.camera,
            toll = alertState.toll,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusBarTop + 16.dp, start = 12.dp),
        )

        MapControlCluster(
            voiceOn = voiceEnabled && mutedVoiceTypes.isEmpty(),
            showSettings = !isNavigating,
            showOverview = hasRoute,
            showReport = isNavigating,
            onVoice = { showVoiceDialog = true },
            onReport = {
                val coord = speedAlert.lastCoordinate()
                val activity = ReportUtils.findActivity(context)
                if (coord == null || activity == null) {
                    Toast.makeText(context, "Chưa có vị trí GPS để chụp", Toast.LENGTH_SHORT).show()
                    return@MapControlCluster
                }
                val name = ReportUtils.coordFileName(coord.first, coord.second)
                ReportUtils.captureAndSave(activity, vietMap, mapView, name) { path ->
                    val msg = if (path != null) "Đã lưu report: $path" else "Chụp report thất bại"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onSettings = { showSettingsDialog = true },
            onOverview = {
                val route = navState.currentRoute
                val map = vietMap
                if (route == null || map == null) {
                    Toast.makeText(context, "Chưa có tuyến để xem tổng quan", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val cords = route.routeOptions()?.coordinates().orEmpty()
                    if (cords.size > 1) {
                        val bounds = LatLngBounds.Builder()
                            .includes(cords.map { LatLng(it.latitude(), it.longitude()) })
                            .build()
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, 120, 200, 120, 320),
                        )
                    }
                }
            },
            onRecenter = {
                navController.setFollowing(true)
                val map = vietMap ?: return@MapControlCluster
                if (!map.locationComponent.isLocationComponentActivated) return@MapControlCluster
                if (isNavigating) {
                    map.locationComponent.setCameraMode(
                        CameraMode.TRACKING_GPS, 750L, 16.5, null, 60.0, null,
                    )
                } else {
                    map.locationComponent.setCameraMode(
                        CameraMode.TRACKING_GPS_NORTH, 750L, 16.5, 0.0, 0.0, null,
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = navBarBottom + 96.dp,
                ),
        )

        BottomActionBar(
            isNavigating = isNavigating,
            statusLine = navState.status,
            routeSummary = navState.routeSummary,
            isReady = alertState.ready,
            statusText = alertState.statusText,
            onStart = {
                if (navState.isNavigating || navState.isBuildingRoute) {
                    return@BottomActionBar
                }
                val map = vietMap
                if (map == null || !permission.granted ||
                    !map.locationComponent.isLocationComponentActivated
                ) {
                    Toast.makeText(context, "Chưa có quyền vị trí", Toast.LENGTH_SHORT).show()
                    permission.request()
                    return@BottomActionBar
                }
                val destination = navState.destination
                if (destination == null) {
                    Toast.makeText(context, "Hãy chọn điểm đến trước", Toast.LENGTH_SHORT).show()
                    return@BottomActionBar
                }
                val origin = map.locationComponent.lastKnownLocation?.let {
                    Point.fromLngLat(it.longitude, it.latitude)
                }
                if (origin == null) {
                    Toast.makeText(context, "Chưa có vị trí GPS, thử lại sau", Toast.LENGTH_SHORT)
                        .show()
                    return@BottomActionBar
                }
                navController.setOrigin(origin)
                speedAlert.configure(vehicleProfile)
                map.locationComponent.setCameraMode(
                    CameraMode.TRACKING_GPS, 1000L, 16.5, null, 60.0, null,
                )
                navController.buildRoute(
                    apikey = AppConfig.VIETMAP_API_KEY,
                    origin = origin,
                    destination = destination,
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                ) { route ->
                    if (route != null) {
                        symbolManager?.deleteAll()
                        navController.startNavigationWithRoute(route)
                    }
                }
            },
            onStop = {
                navController.stop()
                speedAlert.release()
                routeRenderer?.removeRoute()
                navController.setDestination(null)
                vietMap?.locationComponent?.takeIf { it.isLocationComponentActivated }
                    ?.setCameraMode(
                        CameraMode.TRACKING_GPS_NORTH, 1000L, 16.5, 0.0, 0.0, null,
                    )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = navBarBottom + 12.dp,
                ),
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initialProfile = vehicleProfile,
            initialSimulatorEnabled = navState.isSimulator,
            initialSimulatorSpeed = navState.simulatorSpeed,
            onDismiss = { showSettingsDialog = false },
            onConfirm = { result ->
                val profileChanged = result.profile != vehicleProfile
                vehicleProfile = result.profile
                navController.setSimulator(result.simulatorEnabled)
                navController.setSimulatorSpeed(result.simulatorSpeed)
                showSettingsDialog = false
                if (profileChanged && isNavigating) {
                    speedAlert.configure(result.profile)
                    navState.currentRoute?.geometry()?.let { speedAlert.startRoute(it) }
                }
            },
        )
    }

    if (showVoiceDialog) {
        VoiceSettingsDialog(
            voiceEnabled = voiceEnabled,
            mutedTypes = mutedVoiceTypes,
            onVoiceEnabledChange = {
                voiceEnabled = it
                speedAlert.setVoiceEnabled(it)
            },
            onMutedTypesChange = {
                mutedVoiceTypes = it
                speedAlert.setMutedAlertTypes(it)
            },
            onDismiss = { showVoiceDialog = false },
        )
    }
}

@SuppressLint("MissingPermission")
private fun enableLocationComponent(context: Context, map: VietMapGL, style: Style) {
    val opts = LocationComponentOptions.builder(context).pulseEnabled(true).build()
    map.locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(opts)
            .useDefaultLocationEngine(true)
            .build()
    )
    map.locationComponent.isLocationComponentEnabled = true
    map.locationComponent.renderMode = RenderMode.COMPASS
    map.locationComponent.setCameraMode(
        CameraMode.TRACKING_GPS_NORTH, 750L, 16.5, 0.0, 0.0, null,
    )
}

