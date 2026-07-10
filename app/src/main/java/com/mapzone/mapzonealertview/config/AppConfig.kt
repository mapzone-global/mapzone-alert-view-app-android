package com.mapzone.mapzonealertview.config

import com.mapzone.mapzonealertview.BuildConfig

object AppConfig {
    const val VIETMAP_API_KEY: String = BuildConfig.VIETMAP_API_KEY
    const val VIETMAP_TILEMAP_KEY: String = BuildConfig.VIETMAP_TILEMAP_KEY
    const val BASE_URL: String = BuildConfig.BASE_URL
    const val SEGMENT_URL: String = BuildConfig.SEGMENT_URL
    const val SPEED_ALERT_API_KEY_ID: String = BuildConfig.SPEED_ALERT_API_KEY_ID
    const val SPEED_ALERT_API_KEY: String = BuildConfig.SPEED_ALERT_API_KEY
    const val VEHICLE_ID: String = BuildConfig.VEHICLE_ID

    var styleUrl: String = "https://maps.vietmap.vn/maps/styles/dm/style.json?apikey=$VIETMAP_TILEMAP_KEY"

    fun assertReady() {
        require(VIETMAP_API_KEY.isNotBlank()) { "VIETMAP_API_KEY does not config in local.properties" }
        require(VIETMAP_TILEMAP_KEY.isNotBlank()) { "VIETMAP_TILEMAP_KEY does not config in local.properties" }
    }
}
