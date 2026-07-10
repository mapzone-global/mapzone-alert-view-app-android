package com.mapzone.mapzonealertview.presentation.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import vn.vietmap.vietmapsdk.location.engine.LocationEngine
import vn.vietmap.services.android.navigation.v5.location.engine.LocationEngineProvider
import vn.vietmap.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import vn.vietmap.services.android.navigation.v5.navigation.NavigationRoute
import vn.vietmap.services.android.navigation.v5.navigation.VietmapNavigation
import vn.vietmap.services.android.navigation.v5.navigation.VietmapNavigationOptions
import vn.vietmap.services.android.navigation.v5.offroute.OffRouteListener
import vn.vietmap.services.android.navigation.v5.routeprogress.ProgressChangeListener
import vn.vietmap.services.android.navigation.v5.routeprogress.RouteProgress
import vn.vietmap.services.android.navigation.v5.snap.SnapToRoute

data class NavigationUiState(
    val isNavigating: Boolean = false,
    val isBuildingRoute: Boolean = false,
    val isSimulator: Boolean = false,
    val simulatorSpeed: Int = 70,
    val origin: Point? = null,
    val destination: Point? = null,
    val currentRoute: DirectionsRoute? = null,
    val routeSummary: String? = null,
    val status: String = "Chọn điểm đến để bắt đầu",
    val lastLocation: Location? = null,
    val snappedLocation: Location? = null,
    val isFollowingUser: Boolean = true,
)

class NavigationController(
    private val context: Context,
    private val speedAlert: SpeedAlertController,
) : ProgressChangeListener, OffRouteListener {
    private val tag = "NavigationCtrl"
    private val _state = MutableStateFlow(NavigationUiState())
    val state: StateFlow<NavigationUiState> = _state.asStateFlow()

    private val snapEngine = SnapToRoute()
    private var navigation: VietmapNavigation? = null
    private var locationEngine: LocationEngine? = null

    private var routeBuildGen: Int = 0

    private var lastApikey: String? = null
    private var lastProfile: String = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC

    fun changeProfile(profile: String) {
        lastProfile = profile;
    }

    private var isRerouting: Boolean = false

    private val options: VietmapNavigationOptions = VietmapNavigationOptions.builder()
        .maxTurnCompletionOffset(30.0)
        .maneuverZoneRadius(40.0)
        .maximumDistanceOffRoute(50.0)
        .deadReckoningTimeInterval(5.0)
        .maxManipulatedCourseAngle(25.0)
        .userLocationSnapDistance(20.0)
        .secondsBeforeReroute(3)
        .enableOffRouteDetection(true)
        .enableFasterRouteDetection(false)
        .snapToRoute(true)
        .manuallyEndNavigationUponCompletion(false)
        .defaultMilestonesEnabled(true)
        .minimumDistanceBeforeRerouting(10.0)
        .metersRemainingTillArrival(20.0)
        .isFromNavigationUi(false)
        .isDebugLoggingEnabled(false)
        .locationAcceptableAccuracyInMetersThreshold(100)
        .build()

    fun setOrigin(p: Point?) {
        _state.value = _state.value.copy(origin = p)
    }

    fun setDestination(p: Point?) {
        _state.value = _state.value.copy(destination = p)
    }

    fun setFollowing(value: Boolean) {
        _state.value = _state.value.copy(isFollowingUser = value)
    }

    fun setSimulator(enabled: Boolean) {
        _state.value = _state.value.copy(isSimulator = enabled)
    }

    fun setSimulatorSpeed(speedKmh: Int) {
        _state.value = _state.value.copy(simulatorSpeed = speedKmh)
        (locationEngine as? ReplayRouteLocationEngine)?.updateSpeed(speedKmh)
    }

    fun buildRoute(
        apikey: String,
        origin: Point,
        destination: Point,
        profile: String,
        bearing: Double? = null,
        onResult: (DirectionsRoute?) -> Unit,
    ) {
        val myGen = ++routeBuildGen
        lastApikey = apikey
        lastProfile = profile
        _state.value = _state.value.copy(status = "Đang tính tuyến…", isBuildingRoute = true)
        val builder = NavigationRoute.builder(context)
            .apikey(apikey)
            .destination(destination)
            .alternatives(true)
            .profile(profile)
        if (bearing != null) {
            builder.origin(origin, bearing, 60.0)
        } else {
            builder.origin(origin)
        }
        builder
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (myGen != routeBuildGen) return
                    val route = response.body()?.routes()?.firstOrNull()
                    if (route == null) {
                        _state.value = _state.value.copy(
                            status = "Không tìm thấy tuyến",
                            isBuildingRoute = false,
                        )
                        onResult(null)
                        return
                    }
                    val km = (route.distance() ?: 0.0) / 1000.0
                    val min = (route.duration() ?: 0.0) / 60.0
                    _state.value = _state.value.copy(
                        currentRoute = route,
                        routeSummary = "Tuyến %.1f km · %.1f phút".format(km, min),
                        status = "Tuyến đã sẵn sàng",
                        isBuildingRoute = false,
                    )
                    onResult(route)
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    if (myGen != routeBuildGen) return
                    Log.e(tag, "Route failed", t)
                    _state.value = _state.value.copy(
                        status = "Tính tuyến thất bại: ${t.message}",
                        isBuildingRoute = false,
                    )
                    onResult(null)
                }
            })
    }

    @SuppressLint("MissingPermission")
    fun startNavigationWithRoute(route: DirectionsRoute) {
        runCatching { navigation?.stopNavigation() }
        runCatching { navigation?.onDestroy() }
        navigation = null

        val isSim = _state.value.isSimulator
        val simSpeed = _state.value.simulatorSpeed
        locationEngine =
            if (isSim) ReplayRouteLocationEngine() else LocationEngineProvider.getBestLocationEngine(context)

        val nav = VietmapNavigation(context, options, locationEngine!!)
        nav.addProgressChangeListener(this)
        nav.addOffRouteListener(this)

        if (isSim) {
            (locationEngine as? ReplayRouteLocationEngine)?.apply {
                updateSpeed(simSpeed)
                assign(route)
            }
        }

        navigation = nav
        nav.startNavigation(route)

        route.geometry()?.let { speedAlert.startRoute(it) }
            ?: Log.w(tag, "Route không có geometry — không thể start Alert View SDK")

        _state.value = _state.value.copy(
            isNavigating = true,
            currentRoute = route,
            isFollowingUser = true,
            status = if (isSim) "Đang điều hướng (mô phỏng)" else "Đang điều hướng",
        )
    }

    fun stop() {
        routeBuildGen++
        isRerouting = false
        runCatching { navigation?.stopNavigation() }
        runCatching { navigation?.onDestroy() }
        navigation = null
        locationEngine = null
        _state.value = NavigationUiState(
            status = "Đã kết thúc điều hướng",
        )
    }

    fun destroy() {
        routeBuildGen++
        runCatching { navigation?.onDestroy() }
        navigation = null
        locationEngine = null
    }

    override fun onProgressChange(p0: Location?, p1: RouteProgress?) {
        val loc = p0 ?: return
        val progress = p1 ?: return
        val snapped = snapEngine.getSnappedLocation(loc, progress)
        val cur = _state.value
        _state.value = cur.copy(lastLocation = loc, snappedLocation = snapped)
        val speedKmh = when {
            cur.isSimulator -> cur.simulatorSpeed.toDouble()
            loc.hasSpeed() -> loc.speed * 3.6
            else -> 0.0
        }
        speedAlert.feedLocation(
            lat = snapped.latitude,
            lng = snapped.longitude,
            speedKmh = speedKmh,
            heading = snapped.bearing.toDouble(),
            accuracy = loc.accuracy.toDouble(),
        )
    }

    override fun userOffRoute(p0: Location?) {
        val loc = p0 ?: return
        if (isRerouting) return
        if (!_state.value.isNavigating) return
        val destination = _state.value.destination ?: run {
            return
        }
        val apikey = lastApikey ?: run {
            return
        }

        isRerouting = true
        val origin = Point.fromLngLat(loc.longitude, loc.latitude)
        val bearing = if (loc.hasBearing()) loc.bearing.toDouble() else null
        _state.value = _state.value.copy(origin = origin, status = "Đi lệch tuyến — đang tính lại…")

        buildRoute(
            apikey = apikey,
            origin = origin,
            destination = destination,
            profile = lastProfile,
            bearing = bearing,
        ) { route ->
            isRerouting = false
            if (route != null) {
                startNavigationWithRoute(route)
            } else {
                Log.w(tag, "Reroute thất bại — giữ nguyên tuyến cũ")
            }
        }
    }
}
