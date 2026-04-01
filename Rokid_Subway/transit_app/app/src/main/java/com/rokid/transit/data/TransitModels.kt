package com.rokid.transit.data

data class TransitPlan(
    val cost: TransitCost,
    val segments: List<TransitSegment>
)

data class TransitCost(
    val duration: Int,       // 秒
    val transitFee: String,  // 票价
    val walkDistance: Int     // 步行距离(米)
)

data class TransitSegment(
    val type: SegmentType,
    val walkInfo: WalkInfo? = null,
    val transitInfo: TransitLineInfo? = null
)

enum class SegmentType {
    WALK,
    SUBWAY,
    BUS
}

data class WalkInfo(
    val distance: Int,    // 米
    val duration: Int,    // 秒
    val origin: String,
    val destination: String
)

data class TransitLineInfo(
    val lineName: String,
    val lineColor: String,   // 线路颜色 hex
    val departureStop: String,
    val arrivalStop: String,
    val stationCount: Int,
    val duration: Int,        // 秒
    val viaStops: List<String>,
    val direction: String,    // 方向
    val isSubway: Boolean
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)
