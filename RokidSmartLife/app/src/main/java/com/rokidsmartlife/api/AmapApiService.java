package com.rokidsmartlife.api;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import android.util.Log;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AmapApiService {

    private static final String BASE_URL = "https://restapi.amap.com/v3";
    private static final String BASE_URL_V5 = "https://restapi.amap.com/v5";

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private String apiKey;

    public AmapApiService(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void setApiKey(String key) {
        this.apiKey = key;
    }

    public String getApiKey() {
        return apiKey;
    }

    /**
     * POI 周边搜索 (扫街榜核心)
     * @param longitude 经度
     * @param latitude 纬度
     * @param type POI类型编码 (如 "050000" 餐饮, "060000" 购物)
     * @param radius 搜索半径(米)
     * @param page 页码
     * @param callback 回调
     */
    public void searchNearby(double longitude, double latitude, String type,
                             int radius, int page, ApiCallback<List<PoiResult>> callback) {
        String url = BASE_URL + "/place/around?" +
                "key=" + apiKey +
                "&location=" + longitude + "," + latitude +
                "&types=" + type +
                "&radius=" + radius +
                "&offset=15" +
                "&page=" + page +
                "&sortrule=distance" +
                "&extensions=all";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handlePoiResponse(response, callback);
            }
        });
    }

    /**
     * 关键词搜索 POI
     */
    public void searchByKeyword(String keyword, String city, double longitude, double latitude,
                                int page, ApiCallback<List<PoiResult>> callback) {
        String url = BASE_URL + "/place/text?" +
                "key=" + apiKey +
                "&keywords=" + keyword +
                "&city=" + (city != null ? city : "") +
                "&location=" + longitude + "," + latitude +
                "&offset=15" +
                "&page=" + page +
                "&sortrule=distance" +
                "&extensions=all";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handlePoiResponse(response, callback);
            }
        });
    }

    /**
     * 步行路线规划
     */
    public void walkingRoute(double originLng, double originLat,
                             double destLng, double destLat,
                             ApiCallback<RouteResult> callback) {
        String url = BASE_URL + "/direction/walking?" +
                "key=" + apiKey +
                "&origin=" + originLng + "," + originLat +
                "&destination=" + destLng + "," + destLat;

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (!"1".equals(json.get("status").getAsString())) {
                        postError(callback, getErrorInfo(json));
                        return;
                    }
                    JsonObject route = json.getAsJsonObject("route");
                    RouteResult result = parseRoute(route);
                    postSuccess(callback, result);
                } catch (Exception e) {
                    postError(callback, "解析路线数据失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 驾车路线规划
     */
    public void drivingRoute(double originLng, double originLat,
                             double destLng, double destLat,
                             ApiCallback<RouteResult> callback) {
        String url = BASE_URL + "/direction/driving?" +
                "key=" + apiKey +
                "&origin=" + originLng + "," + originLat +
                "&destination=" + destLng + "," + destLat +
                "&strategy=10";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (!"1".equals(json.get("status").getAsString())) {
                        postError(callback, getErrorInfo(json));
                        return;
                    }
                    JsonObject route = json.getAsJsonObject("route");
                    RouteResult result = parseRoute(route);
                    postSuccess(callback, result);
                } catch (Exception e) {
                    postError(callback, "解析路线数据失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 逆地理编码 (坐标 → 地址)
     */
    public void reverseGeocode(double longitude, double latitude, ApiCallback<String> callback) {
        String url = BASE_URL + "/geocode/regeo?" +
                "key=" + apiKey +
                "&location=" + longitude + "," + latitude +
                "&extensions=base";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if ("1".equals(json.get("status").getAsString())) {
                        String address = json.getAsJsonObject("regeocode")
                                .get("formatted_address").getAsString();
                        postSuccess(callback, address);
                    } else {
                        postError(callback, getErrorInfo(json));
                    }
                } catch (Exception e) {
                    postError(callback, "解析地址失败");
                }
            }
        });
    }

    private void handlePoiResponse(Response response, ApiCallback<List<PoiResult>> callback) {
        try {
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);

            if (!"1".equals(json.get("status").getAsString())) {
                postError(callback, getErrorInfo(json));
                return;
            }

            List<PoiResult> results = new ArrayList<>();
            JsonArray pois = json.getAsJsonArray("pois");
            if (pois != null) {
                for (JsonElement element : pois) {
                    JsonObject poi = element.getAsJsonObject();
                    PoiResult result = new PoiResult();
                    result.id = getStr(poi, "id");
                    result.name = getStr(poi, "name");
                    result.type = getStr(poi, "type");
                    result.typecode = getStr(poi, "typecode");
                    result.address = getStr(poi, "address");
                    result.tel = getStr(poi, "tel");
                    result.distance = getStr(poi, "distance");

                    String location = getStr(poi, "location");
                    if (location != null && location.contains(",")) {
                        String[] parts = location.split(",");
                        result.longitude = Double.parseDouble(parts[0]);
                        result.latitude = Double.parseDouble(parts[1]);
                    }

                    JsonObject biz = poi.has("biz_ext") && !poi.get("biz_ext").isJsonNull()
                            ? poi.getAsJsonObject("biz_ext") : null;
                    if (biz != null) {
                        result.rating = getStr(biz, "rating");
                        result.cost = getStr(biz, "cost");
                        result.openTime = getStr(biz, "open_time");
                    }

                    if (poi.has("photos") && poi.get("photos").isJsonArray()) {
                        JsonArray photos = poi.getAsJsonArray("photos");
                        if (photos.size() > 0) {
                            result.photoUrl = getStr(photos.get(0).getAsJsonObject(), "url");
                        }
                    }

                    result.cityname = getStr(poi, "cityname");
                    result.adname = getStr(poi, "adname");

                    results.add(result);
                }
            }
            postSuccess(callback, results);
        } catch (Exception e) {
            postError(callback, "解析数据失败: " + e.getMessage());
        }
    }

    private RouteResult parseRoute(JsonObject route) {
        RouteResult result = new RouteResult();
        result.origin = getStr(route, "origin");
        result.destination = getStr(route, "destination");

        JsonArray paths = route.getAsJsonArray("paths");
        if (paths != null && paths.size() > 0) {
            JsonObject path = paths.get(0).getAsJsonObject();
            result.totalDistance = getStr(path, "distance");
            result.totalDuration = getStr(path, "duration");

            JsonArray stepsArr = path.getAsJsonArray("steps");
            if (stepsArr != null) {
                result.steps = new ArrayList<>();
                for (JsonElement stepEl : stepsArr) {
                    JsonObject stepObj = stepEl.getAsJsonObject();
                    RouteResult.Step step = new RouteResult.Step();
                    step.instruction = getStr(stepObj, "instruction");
                    step.road = getStr(stepObj, "road");
                    step.distance = getStr(stepObj, "distance");
                    step.duration = getStr(stepObj, "duration");
                    step.action = getStr(stepObj, "action");
                    step.assistantAction = getStr(stepObj, "assistant_action");
                    result.steps.add(step);
                }
            }
        }
        return result;
    }

    private String getStr(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            JsonElement el = obj.get(key);
            if (el.isJsonArray()) return null;
            String val = el.getAsString();
            return "[]".equals(val) || val.isEmpty() ? null : val;
        }
        return null;
    }

    private String getErrorInfo(JsonObject json) {
        String info = json.has("info") ? json.get("info").getAsString() : "未知错误";
        String infocode = json.has("infocode") ? json.get("infocode").getAsString() : "";
        if ("10001".equals(infocode)) return "API Key 无效，请在设置中配置正确的高德 Web 服务 Key";
        if ("10003".equals(infocode)) return "API 调用超出限制";
        if ("10004".equals(infocode)) return "缺少必要参数";
        if ("10009".equals(infocode)) return "当前 Key 不支持 Web 服务\n请到 lbs.amap.com 申请「Web服务」类型的 Key";
        if ("10044".equals(infocode)) return "API 调用频率超限，请稍后重试";
        return info + " (" + infocode + ")";
    }

    private <T> void postSuccess(ApiCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    /**
     * IP 定位 (无需 GPS/WiFi 定位权限，通过网络 IP 获取大致位置)
     */
    private static final String TAG = "AmapApi";

    public void ipLocate(ApiCallback<double[]> callback) {
        String url = BASE_URL + "/ip?key=" + apiKey;
        Log.d(TAG, "ipLocate request: " + url);

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Amap ipLocate failed, trying ipinfo.io", e);
                ipLocateFallback(callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    Log.d(TAG, "ipLocate response: " + body);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (!"1".equals(json.get("status").getAsString())) {
                        Log.e(TAG, "ipLocate API error: " + getErrorInfo(json));
                        ipLocateFallback(callback);
                        return;
                    }
                    String rectangle = getStr(json, "rectangle");
                    if (rectangle != null && rectangle.contains(";")) {
                        String[] bounds = rectangle.split(";");
                        String[] sw = bounds[0].split(",");
                        String[] ne = bounds[1].split(",");
                        double lng = (Double.parseDouble(sw[0]) + Double.parseDouble(ne[0])) / 2;
                        double lat = (Double.parseDouble(sw[1]) + Double.parseDouble(ne[1])) / 2;
                        Log.d(TAG, "ipLocate success: lat=" + lat + " lng=" + lng);
                        postSuccess(callback, new double[]{lat, lng});
                    } else {
                        Log.w(TAG, "Amap ipLocate empty result, trying ipinfo.io fallback");
                        ipLocateFallback(callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ipLocate parse error, trying fallback", e);
                    ipLocateFallback(callback);
                }
            }
        });
    }

    private void ipLocateFallback(ApiCallback<double[]> callback) {
        String url = "https://ipinfo.io/json";
        Log.d(TAG, "ipLocateFallback: trying ipinfo.io");
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "ipinfo.io also failed", e);
                postError(callback, "IP 定位失败（网络不可用或使用了VPN）");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    Log.d(TAG, "ipinfo.io response: " + body);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String loc = json.has("loc") ? json.get("loc").getAsString() : null;
                    if (loc != null && loc.contains(",")) {
                        String[] parts = loc.split(",");
                        double lat = Double.parseDouble(parts[0]);
                        double lng = Double.parseDouble(parts[1]);
                        Log.d(TAG, "ipinfo.io success: lat=" + lat + " lng=" + lng);
                        postSuccess(callback, new double[]{lat, lng});
                    } else {
                        String city = json.has("city") ? json.get("city").getAsString() : null;
                        if (city != null && !city.isEmpty()) {
                            Log.d(TAG, "ipinfo.io got city: " + city + ", geocoding...");
                            geocodeCity(city, callback);
                        } else {
                            postError(callback, "无法获取位置信息");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ipinfo.io parse error", e);
                    postError(callback, "IP 定位解析失败");
                }
            }
        });
    }

    /**
     * 公交/地铁路线规划（综合换乘方案）
     */
    public void transitRoute(double originLng, double originLat,
                             double destLng, double destLat,
                             String city,
                             ApiCallback<TransitResult> callback) {
        String url = BASE_URL + "/direction/transit/integrated?" +
                "key=" + apiKey +
                "&origin=" + originLng + "," + originLat +
                "&destination=" + destLng + "," + destLat +
                "&city=" + urlEncode(city != null ? city : "全国") +
                "&strategy=0" +
                "&nightflag=0" +
                "&output=json";

        Log.d(TAG, "transitRoute request: " + url);
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    Log.d(TAG, "transitRoute response length=" + body.length());
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (!"1".equals(json.get("status").getAsString())) {
                        postError(callback, getErrorInfo(json));
                        return;
                    }
                    JsonObject route = json.getAsJsonObject("route");
                    TransitResult result = parseTransitRoute(route);
                    postSuccess(callback, result);
                } catch (Exception e) {
                    Log.e(TAG, "transitRoute parse error", e);
                    postError(callback, "解析公交路线失败: " + e.getMessage());
                }
            }
        });
    }

    private TransitResult parseTransitRoute(JsonObject route) {
        TransitResult result = new TransitResult();
        result.origin = getStr(route, "origin");
        result.destination = getStr(route, "destination");
        result.totalDistance = getStr(route, "distance");

        JsonArray transits = route.getAsJsonArray("transits");
        if (transits != null && transits.size() > 0) {
            result.plans = new ArrayList<>();
            for (int i = 0; i < Math.min(transits.size(), 5); i++) {
                JsonObject transit = transits.get(i).getAsJsonObject();
                TransitResult.TransitPlan plan = new TransitResult.TransitPlan();
                plan.cost = getStr(transit, "cost");
                plan.duration = getStr(transit, "duration");
                plan.walkingDistance = getStr(transit, "walking_distance");
                plan.nightflag = "1".equals(getStr(transit, "nightflag"));

                plan.segments = new ArrayList<>();
                JsonArray segmentsArr = transit.getAsJsonArray("segments");
                if (segmentsArr != null) {
                    for (JsonElement segEl : segmentsArr) {
                        JsonObject segObj = segEl.getAsJsonObject();
                        parseTransitSegment(segObj, plan.segments);
                    }
                }
                result.plans.add(plan);
            }

            if (!result.plans.isEmpty()) {
                result.totalDuration = result.plans.get(0).duration;
                result.totalPrice = result.plans.get(0).cost;
                result.totalWalkingDistance = result.plans.get(0).walkingDistance;
            }
        }
        return result;
    }

    private void parseTransitSegment(JsonObject segObj, List<TransitResult.Segment> segments) {
        if (segObj.has("walking") && !segObj.get("walking").isJsonNull()) {
            JsonObject walking = segObj.getAsJsonObject("walking");
            TransitResult.Segment seg = new TransitResult.Segment();
            seg.type = TransitResult.Segment.TYPE_WALKING;
            seg.walkingDistance = getStr(walking, "distance");
            seg.walkingDuration = getStr(walking, "duration");

            JsonArray steps = walking.getAsJsonArray("steps");
            if (steps != null && steps.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (JsonElement stepEl : steps) {
                    JsonObject step = stepEl.getAsJsonObject();
                    String inst = getStr(step, "instruction");
                    if (inst != null) {
                        if (sb.length() > 0) sb.append("，");
                        sb.append(inst);
                    }
                }
                seg.instruction = sb.toString();
            }
            if (seg.walkingDistance != null) {
                try {
                    int d = Integer.parseInt(seg.walkingDistance);
                    if (d > 0) segments.add(seg);
                } catch (NumberFormatException e) {
                    segments.add(seg);
                }
            }
        }

        if (segObj.has("bus") && !segObj.get("bus").isJsonNull()) {
            JsonObject bus = segObj.getAsJsonObject("bus");
            JsonArray buslines = bus.getAsJsonArray("buslines");
            if (buslines != null) {
                for (JsonElement blEl : buslines) {
                    JsonObject bl = blEl.getAsJsonObject();
                    TransitResult.Segment seg = new TransitResult.Segment();

                    String lineName = getStr(bl, "name");
                    if (lineName != null && (lineName.contains("地铁") || lineName.contains("号线")
                            || lineName.contains("轨道") || lineName.contains("城轨"))) {
                        seg.type = TransitResult.Segment.TYPE_SUBWAY;
                    } else {
                        seg.type = TransitResult.Segment.TYPE_BUS;
                    }

                    seg.lineName = lineName;
                    seg.busDuration = getStr(bl, "duration");

                    JsonObject depStop = bl.has("departure_stop") && !bl.get("departure_stop").isJsonNull()
                            ? bl.getAsJsonObject("departure_stop") : null;
                    if (depStop != null) {
                        seg.departureStop = getStr(depStop, "name");
                    }

                    JsonObject arrStop = bl.has("arrival_stop") && !bl.get("arrival_stop").isJsonNull()
                            ? bl.getAsJsonObject("arrival_stop") : null;
                    if (arrStop != null) {
                        seg.arrivalStop = getStr(arrStop, "name");
                    }

                    JsonArray viaStops = bl.getAsJsonArray("via_stops");
                    if (viaStops != null) {
                        seg.passStops = viaStops.size() + 1;
                    }

                    segments.add(seg);
                    break;
                }
            }
        }
    }

    public void geocodeCity(String city, ApiCallback<double[]> callback) {
        String url = BASE_URL + "/geocode/geo?key=" + apiKey + "&address=" + city;
        Log.d(TAG, "geocodeCity: " + url);
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postError(callback, "地理编码失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if ("1".equals(json.get("status").getAsString())) {
                        JsonArray geocodes = json.getAsJsonArray("geocodes");
                        if (geocodes != null && geocodes.size() > 0) {
                            String location = geocodes.get(0).getAsJsonObject().get("location").getAsString();
                            String[] parts = location.split(",");
                            double lng = Double.parseDouble(parts[0]);
                            double lat = Double.parseDouble(parts[1]);
                            postSuccess(callback, new double[]{lat, lng});
                            return;
                        }
                    }
                    postError(callback, "地理编码未找到结果");
                } catch (Exception e) {
                    postError(callback, "地理编码解析失败");
                }
            }
        });
    }

    /**
     * WiFi 定位：利用高德智能硬件定位 API，通过周边 WiFi 的 BSSID/信号强度来精确定位
     * @param connectedMac 已连接 WiFi 的 MAC，格式 "mac,signal,ssid"
     * @param wifiList 周边 WiFi 列表，每个元素格式 "mac,signal,ssid"，用 "|" 连接
     */
    public void wifiLocate(String connectedMac, String wifiList, ApiCallback<double[]> callback) {
        StringBuilder sb = new StringBuilder("https://apilocate.amap.com/position?");
        sb.append("key=").append(apiKey);
        sb.append("&accesstype=1");
        if (connectedMac != null && !connectedMac.isEmpty()) {
            sb.append("&mmac=").append(urlEncode(connectedMac));
        }
        sb.append("&macs=").append(urlEncode(wifiList));
        sb.append("&output=json");

        String url = sb.toString();
        Log.d(TAG, "wifiLocate request URL length=" + url.length());

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "wifiLocate network failure", e);
                postError(callback, "WiFi 定位网络失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    Log.d(TAG, "wifiLocate response: " + body);
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    String status = json.has("status") ? json.get("status").getAsString() : "0";
                    if ("1".equals(status)) {
                        JsonObject result = json.has("result") ? json.getAsJsonObject("result") : null;
                        if (result != null) {
                            String location = result.has("location") ? result.get("location").getAsString() : null;
                            String type = result.has("type") ? result.get("type").getAsString() : "0";
                            if (location != null && location.contains(",") && !"0".equals(type)) {
                                String[] parts = location.split(",");
                                double lng = Double.parseDouble(parts[0]);
                                double lat = Double.parseDouble(parts[1]);
                                String radius = result.has("radius") ? result.get("radius").getAsString() : "?";
                                Log.d(TAG, "wifiLocate success: lat=" + lat + " lng=" + lng + " radius=" + radius + "m");
                                postSuccess(callback, new double[]{lat, lng});
                                return;
                            }
                        }
                        postError(callback, "WiFi 定位无结果");
                    } else {
                        String info = json.has("info") ? json.get("info").getAsString() : "未知错误";
                        Log.e(TAG, "wifiLocate API error: " + info);
                        postError(callback, "WiFi 定位失败: " + info);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "wifiLocate parse error", e);
                    postError(callback, "WiFi 定位解析失败");
                }
            }
        });
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public interface ApiCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
