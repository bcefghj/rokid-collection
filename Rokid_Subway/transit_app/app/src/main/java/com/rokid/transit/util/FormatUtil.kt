package com.rokid.transit.util

object FormatUtil {

    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "1分钟"
        }
    }

    fun formatDistance(meters: Int): String {
        return when {
            meters >= 1000 -> String.format("%.1f公里", meters / 1000.0)
            else -> "${meters}米"
        }
    }

    fun shortLineName(name: String): String {
        return name
            .replace("地铁", "")
            .replace(Regex("\\(.*\\)"), "")
            .trim()
    }
}
