package com.rokid.transit.service

import com.google.gson.JsonParser
import com.rokid.transit.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AmapTransitService {

    companion object {
        private const val API_KEY = "ec54bc7f445760ae1dd4bb7b06a1b451"
        private const val TRANSIT_URL = "https://restapi.amap.com/v3/direction/transit/integrated"
        private const val GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo"
        private const val REGEOCODE_URL = "https://restapi.amap.com/v3/geocode/regeo"
        private const val POI_URL = "https://restapi.amap.com/v3/place/text"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchPOI(keyword: String, city: String = ""): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val cityParam = if (city.isNotEmpty()) "&city=${URLEncoder.encode(city, "UTF-8")}" else ""
        val url = "$POI_URL?key=$API_KEY&keywords=$encodedKeyword$cityParam&offset=5&page=1&extensions=base"

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JsonParser.parseString(body).asJsonObject

        if (json.get("status").asString != "1") return@withContext emptyList()

        val pois = json.getAsJsonArray("pois") ?: return@withContext emptyList()
        pois.map { poi ->
            val obj = poi.asJsonObject
            val name = obj.get("name").asString
            val location = obj.get("location").asString
            val address = obj.get("address")?.let { if (it.isJsonPrimitive) it.asString else "" } ?: ""
            Pair(name + if (address.isNotEmpty()) " ($address)" else "", location)
        }
    }

    data class GeocodeResult(val location: String, val city: String)

    suspend fun geocode(address: String, city: String = ""): String? = withContext(Dispatchers.IO) {
        geocodeFull(address, city)?.location
    }

    suspend fun geocodeFull(address: String, city: String = ""): GeocodeResult? = withContext(Dispatchers.IO) {
        val encodedAddr = URLEncoder.encode(address, "UTF-8")
        val cityParam = if (city.isNotEmpty()) "&city=${URLEncoder.encode(city, "UTF-8")}" else ""
        val url = "$GEOCODE_URL?key=$API_KEY&address=$encodedAddr$cityParam"

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null
        val json = JsonParser.parseString(body).asJsonObject

        if (json.get("status").asString != "1") return@withContext null
        val geocodes = json.getAsJsonArray("geocodes")
        if (geocodes == null || geocodes.size() == 0) return@withContext null

        val first = geocodes[0].asJsonObject
        val location = first.get("location").asString
        val geoCity = first.get("city")?.let {
            if (it.isJsonPrimitive) it.asString else ""
        } ?: ""
        GeocodeResult(location, geoCity)
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val url = "$REGEOCODE_URL?key=$API_KEY&location=$lon,$lat&radius=50&extensions=base"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext "当前位置"
        val json = JsonParser.parseString(body).asJsonObject

        if (json.get("status").asString != "1") return@withContext "当前位置"
        val regeocode = json.getAsJsonObject("regeocode") ?: return@withContext "当前位置"
        regeocode.get("formatted_address")?.asString ?: "当前位置"
    }

    suspend fun searchTransitRoute(
        originLonLat: String,
        destLonLat: String,
        city: String,
        cityD: String = ""
    ): List<TransitPlan> = withContext(Dispatchers.IO) {
        val destCity = cityD.ifEmpty { city }
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val encodedCityD = URLEncoder.encode(destCity, "UTF-8")
        val url = "$TRANSIT_URL?key=$API_KEY&origin=$originLonLat&destination=$destLonLat" +
                "&city=$encodedCity&cityd=$encodedCityD&strategy=0&nightflag=0&extensions=all"

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val json = JsonParser.parseString(body).asJsonObject

        if (json.get("status").asString != "1") return@withContext emptyList()

        val route = json.getAsJsonObject("route") ?: return@withContext emptyList()
        val transits = route.getAsJsonArray("transits") ?: return@withContext emptyList()

        transits.mapNotNull { transitEl ->
            try {
                val transit = transitEl.asJsonObject
                val cost = TransitCost(
                    duration = transit.get("duration").asString.toIntOrNull() ?: 0,
                    transitFee = transit.get("cost")?.asString ?: "0",
                    walkDistance = transit.get("walking_distance")?.asString?.toIntOrNull() ?: 0
                )

                val segmentList = mutableListOf<TransitSegment>()
                val segments = transit.getAsJsonArray("segments") ?: return@mapNotNull null

                for (segEl in segments) {
                    val seg = segEl.asJsonObject

                    val walking = seg.getAsJsonObject("walking")
                    if (walking != null) {
                        val wDist = walking.get("distance")?.asString?.toIntOrNull() ?: 0
                        val wDur = walking.get("duration")?.asString?.toIntOrNull() ?: 0
                        if (wDist > 0) {
                            val wOrigin = walking.get("origin")?.asString ?: ""
                            val wDest = walking.get("destination")?.asString ?: ""
                            segmentList.add(TransitSegment(
                                type = SegmentType.WALK,
                                walkInfo = WalkInfo(wDist, wDur, wOrigin, wDest)
                            ))
                        }
                    }

                    val busObj = seg.getAsJsonObject("bus")
                    if (busObj != null) {
                        val buslines = busObj.getAsJsonArray("buslines")
                        if (buslines != null && buslines.size() > 0) {
                            val line = buslines[0].asJsonObject
                            val lineName = line.get("name")?.asString ?: ""
                            val depStop = line.getAsJsonObject("departure_stop")
                                ?.get("name")?.asString ?: ""
                            val arrStop = line.getAsJsonObject("arrival_stop")
                                ?.get("name")?.asString ?: ""
                            val viaStops = line.getAsJsonArray("via_stops")?.map {
                                it.asJsonObject.get("name").asString
                            } ?: emptyList()
                            val dur = line.get("duration")?.asString?.toIntOrNull() ?: 0
                            val lineType = line.get("type")?.asString ?: ""
                            val isSubway = lineName.contains("地铁") || lineName.contains("号线")
                                    || lineType == "地铁线路"

                            val lineColor = getLineColor(lineName)

                            segmentList.add(TransitSegment(
                                type = if (isSubway) SegmentType.SUBWAY else SegmentType.BUS,
                                transitInfo = TransitLineInfo(
                                    lineName = lineName,
                                    lineColor = lineColor,
                                    departureStop = depStop,
                                    arrivalStop = arrStop,
                                    stationCount = viaStops.size + 1,
                                    duration = dur,
                                    viaStops = viaStops,
                                    direction = extractDirection(lineName),
                                    isSubway = isSubway
                                )
                            ))
                        }
                    }
                }

                TransitPlan(cost = cost, segments = segmentList)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun extractDirection(lineName: String): String {
        val regex = Regex("\\((.+?)\\)")
        return regex.find(lineName)?.groupValues?.get(1) ?: ""
    }

    private fun getLineColor(lineName: String): String {
        return when {
            lineName.contains("1号线") -> "#C23A2A"
            lineName.contains("2号线") -> "#006098"
            lineName.contains("3号线") -> "#EF9B0D"
            lineName.contains("4号线") -> "#008E6D"
            lineName.contains("5号线") -> "#A6217E"
            lineName.contains("6号线") -> "#D4006C"
            lineName.contains("7号线") -> "#E47300"
            lineName.contains("8号线") -> "#008BCB"
            lineName.contains("9号线") -> "#87C6E5"
            lineName.contains("10号线") -> "#009DB9"
            lineName.contains("11号线") -> "#6E3128"
            lineName.contains("12号线") -> "#007A61"
            lineName.contains("13号线") -> "#E796B3"
            lineName.contains("14号线") -> "#826A51"
            lineName.contains("15号线") -> "#6A3575"
            lineName.contains("16号线") -> "#6DB02E"
            lineName.contains("昌平线") -> "#DE82B1"
            lineName.contains("房山线") -> "#E66020"
            lineName.contains("亦庄线") -> "#E01F86"
            lineName.contains("八通线") -> "#C23A2A"
            lineName.contains("大兴机场线") -> "#015DAB"
            lineName.contains("大兴线") -> "#E01F86"
            lineName.contains("首都机场线") -> "#015DAB"
            lineName.contains("燕房线") -> "#D4006C"
            lineName.contains("S1线") || lineName.contains("磁浮线") -> "#D5A129"
            lineName.contains("西郊线") -> "#C9373D"
            else -> "#00B272"
        }
    }
}
