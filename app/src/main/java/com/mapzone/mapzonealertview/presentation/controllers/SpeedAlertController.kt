package com.mapzone.mapzonealertview.presentation.controllers

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.util.Log
import com.mapzone.mapzonealertview.config.AppConfig
import com.mapzone.mapzonealertview.core.util.VoiceUtils
import com.mapzone.mapzonealertview.domain.model.VehicleProfile
import com.vietmap.alert_view_sdk.AlertViewManager
import com.vietmap.alert_view_sdk.VoiceAlertType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SignSlot(
    val bitmap: Bitmap? = null,
    val distanceMeters: Int? = null,
)

data class SpeedAlertUiState(
    val ready: Boolean = false,
    val statusText: String = "Chưa khởi động",
    val current: SignSlot = SignSlot(),
    val next: SignSlot = SignSlot(),
    val camera: SignSlot = SignSlot(),
    val toll: SignSlot = SignSlot(),
    val errorMessage: String? = null,
    val speedStatus: Int = 0,
)

class SpeedAlertController(@Suppress("unused") private val context: Context) {

    private val tag = "SpeedAlertCtrl"
    private var manager: AlertViewManager? = null

    private val _state = MutableStateFlow(SpeedAlertUiState())
    val state: StateFlow<SpeedAlertUiState> = _state.asStateFlow()

    @Volatile
    private var lastLat: Double? = null
    @Volatile
    private var lastLng: Double? = null

    private var voiceEnabled: Boolean = true
    private var mutedTypes: Set<VoiceAlertType> = emptySet()

    fun lastCoordinate(): Pair<Double, Double>? {
        val la = lastLat ?: return null
        val ln = lastLng ?: return null
        return la to ln
    }

    fun configure(profile: VehicleProfile) {
        release()
        val m = AlertViewManager()
        m.configure(
            AppConfig.BASE_URL,
            AppConfig.SPEED_ALERT_API_KEY_ID,
            AppConfig.SPEED_ALERT_API_KEY,
            AppConfig.VEHICLE_ID,
            profile.resolveVehicleType(),
            profile.seats,
            profile.weightKg / 1000.0,
            0.0,
        )

        m.setBitmapCallback { currentBmp, speedStatus,
                              nextBmp, nextDist,
                              cameraBmp, cameraDist,
                              tollBmp, tollDist,
                              _ ->
            _state.value = _state.value.copy(
                current = SignSlot(currentBmp, null),
                next = SignSlot(nextBmp, nextDist),
                camera = SignSlot(cameraBmp, cameraDist),
                toll = SignSlot(tollBmp, tollDist),
                speedStatus = speedStatus,
            )
        }
        m.setResultCallback { success, errorCode, errorMessage ->
            if (success) {
                _state.value = _state.value.copy(
                    ready = true,
                    statusText = "Sẵn sàng",
                    errorMessage = null,
                )
            } else if (errorCode != 0) {
                _state.value = _state.value.copy(
                    statusText = "Tải dữ liệu tuyến thất bại",
                    errorMessage = "Lỗi $errorCode: $errorMessage",
                )
            }
        }
        manager = m
        applyVoicePrefs()
    }

    fun setVoiceEnabled(enabled: Boolean) {
        voiceEnabled = enabled
        applyVoicePrefs()
    }

    fun setMutedAlertTypes(types: Set<VoiceAlertType>) {
        mutedTypes = types
        applyVoicePrefs()
    }

    fun isVoiceEnabled(): Boolean = voiceEnabled

    fun mutedAlertTypes(): Set<VoiceAlertType> = mutedTypes

    private fun applyVoicePrefs() {
        val m = manager ?: return
        if (voiceEnabled) {
            m.setVoiceCallback(null)
        } else {
            m.setVoiceCallback { _ -> }
        }
        m.setMutedAlertTypes(mutedTypes)
    }

    fun startRoute(routePolyline: String) {
        val m = manager ?: run {
            return
        }
        _state.value = _state.value.copy(ready = false, statusText = "Đang tải dữ liệu tuyến…")
        m.start(routePolyline)
    }

    fun feedLocation(
        lat: Double,
        lng: Double,
        speedKmh: Double,
        heading: Double,
        accuracy: Double,
    ) {
        lastLat = lat
        lastLng = lng
        manager?.onLocation(lat, lng, heading, speedKmh, accuracy)
    }

    fun feedLocation(location: Location, overrideSpeedKmh: Double? = null) {
        val speed = overrideSpeedKmh ?: if (location.hasSpeed()) location.speed * 3.6 else 0.0
        feedLocation(
            lat = location.latitude,
            lng = location.longitude,
            speedKmh = speed,
            heading = location.bearing.toDouble(),
            accuracy = location.accuracy.toDouble(),
        )
    }

    fun reset() {
        runCatching { manager?.reset() }
        _state.value = _state.value.copy(
            current = SignSlot(),
            next = SignSlot(),
            camera = SignSlot(),
            toll = SignSlot(),
            speedStatus = 0,
        )
    }

    fun release() {
        runCatching { manager?.reset() }
        manager = null
        lastLat = null
        lastLng = null
        VoiceUtils.stop()
        _state.value = SpeedAlertUiState()
    }
}
