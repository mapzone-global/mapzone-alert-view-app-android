package com.mapzone.mapzonealertview.domain.model

import com.google.gson.annotations.SerializedName

data class AutocompleteItem(
    @SerializedName("ref_id") val refId: String,
    @SerializedName("address") val address: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("display") val display: String?,
    @SerializedName("distance") val distance: Double?,
)

data class PlaceResult(
    @SerializedName("display") val display: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("ward") val ward: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
)

data class HistoryEntry(
    val refId: String,
    val display: String,
    val lat: Double,
    val lng: Double,
    val savedAt: Long = System.currentTimeMillis(),
)
