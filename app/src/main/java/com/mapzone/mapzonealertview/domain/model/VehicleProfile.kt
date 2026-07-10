package com.mapzone.mapzonealertview.domain.model

enum class VehicleType(val label: String, val routingProfile: String) {
    CAR("Ô tô", "driving-traffic"),
    MOTORCYCLE("Xe máy", "motorcycle"),
    TRUCK("Xe tải", "driving-traffic"),
    BIKE("Xe đạp", "cycling");
}

data class VehicleProfile(
    val type: VehicleType = VehicleType.CAR,
    val seats: Int = 5,
    val weightKg: Int = 1500,
) {
    fun resolveVehicleType(): Int = when (type) {
        VehicleType.CAR -> 1
        VehicleType.MOTORCYCLE -> 2
        VehicleType.TRUCK -> 3
        VehicleType.BIKE -> 7
    }

    fun resolveVehicleClass(): Int = when (type) {
        VehicleType.MOTORCYCLE, VehicleType.BIKE -> 1
        VehicleType.CAR -> when {
            seats <= 12 -> 1
            seats <= 30 -> 2
            else -> 3
        }
        VehicleType.TRUCK -> when {
            weightKg < 2_000 -> 1
            weightKg < 4_000 -> 2
            weightKg < 10_000 ->3
            weightKg < 18_000 ->4
            else -> 5
        }
    }
}
