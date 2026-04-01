package com.rokidnav.util;

import com.rokidnav.RokidNavApp;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class AmapApi {
    private static final String TAG = "RokidNav_API";
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class PoiResult {
        public String name;
        public String address;
        public String type;
        public String distance;
        public double lat;
        public double lng;
        public String tel;

        @Override
        public String toString() {
            return name + " (" + distance + "m)";
        }
    }

    public static class RouteStep {
        public String instruction;
        public String orientation;
        public int distance;
        public String action;
        public List<double[]> polyline = new ArrayList<>();
    }

    public static class RouteResult {
        public int totalDistance;
        public int totalDuration;
        public List<RouteStep> steps = new ArrayList<>();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public static void searchNearby(double lat, double lng, String types, int radius, Callback<List<PoiResult>> callback) {
        Log.i(TAG, "searchNearby: lat=" + lat + " lng=" + lng + " types=" + types + " r=" + radius);
        executor.execute(() -> {
            try {
                String location = lng + "," + lat;
                String url = RokidNavApp.AMAP_BASE + "/place/around?"
                        + "key=" + RokidNavApp.AMAP_KEY
                        + "&location=" + location
                        + "&types=" + URLEncoder.encode(types, "UTF-8")
                        + "&radius=" + radius
                        + "&offset=20&page=1&output=json"
                        + "&sortrule=distance";
                Log.i(TAG, "searchNearby URL: " + url.substring(0, Math.min(url.length(), 120)));
                String json = httpGet(url);
                Log.i(TAG, "searchNearby response len=" + json.length());
                JSONObject obj = new JSONObject(json);
                List<PoiResult> results = parsePoiList(obj);
                Log.i(TAG, "searchNearby found " + results.size() + " POIs");
                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                Log.e(TAG, "searchNearby error: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void searchKeyword(String keyword, String city, Callback<List<PoiResult>> callback) {
        executor.execute(() -> {
            try {
                String url = RokidNavApp.AMAP_BASE + "/place/text?"
                        + "key=" + RokidNavApp.AMAP_KEY
                        + "&keywords=" + URLEncoder.encode(keyword, "UTF-8")
                        + "&city=" + URLEncoder.encode(city, "UTF-8")
                        + "&offset=20&page=1&output=json";
                String json = httpGet(url);
                JSONObject obj = new JSONObject(json);
                List<PoiResult> results = parsePoiList(obj);
                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void searchKeywordNearby(String keyword, double lat, double lng, Callback<List<PoiResult>> callback) {
        executor.execute(() -> {
            try {
                String location = lng + "," + lat;
                String url = RokidNavApp.AMAP_BASE + "/place/around?"
                        + "key=" + RokidNavApp.AMAP_KEY
                        + "&keywords=" + URLEncoder.encode(keyword, "UTF-8")
                        + "&location=" + location
                        + "&radius=5000"
                        + "&offset=20&page=1&output=json"
                        + "&sortrule=distance";
                String json = httpGet(url);
                JSONObject obj = new JSONObject(json);
                List<PoiResult> results = parsePoiList(obj);
                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void walkRoute(double fromLat, double fromLng, double toLat, double toLng, Callback<RouteResult> callback) {
        executor.execute(() -> {
            try {
                String origin = fromLng + "," + fromLat;
                String dest = toLng + "," + toLat;
                String url = RokidNavApp.AMAP_BASE + "/direction/walking?"
                        + "key=" + RokidNavApp.AMAP_KEY
                        + "&origin=" + origin
                        + "&destination=" + dest
                        + "&output=json";
                String json = httpGet(url);
                JSONObject obj = new JSONObject(json);
                RouteResult route = parseRoute(obj);
                mainHandler.post(() -> callback.onSuccess(route));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private static List<PoiResult> parsePoiList(JSONObject obj) throws Exception {
        List<PoiResult> results = new ArrayList<>();
        JSONArray pois = obj.optJSONArray("pois");
        if (pois == null) return results;
        for (int i = 0; i < pois.length(); i++) {
            JSONObject poi = pois.getJSONObject(i);
            PoiResult r = new PoiResult();
            r.name = poi.optString("name", "");
            r.address = poi.optString("address", "");
            r.type = poi.optString("type", "");
            r.distance = poi.optString("distance", "");
            r.tel = poi.optString("tel", "");
            String loc = poi.optString("location", "0,0");
            String[] parts = loc.split(",");
            if (parts.length == 2) {
                r.lng = Double.parseDouble(parts[0]);
                r.lat = Double.parseDouble(parts[1]);
            }
            results.add(r);
        }
        return results;
    }

    private static RouteResult parseRoute(JSONObject obj) throws Exception {
        RouteResult result = new RouteResult();
        JSONObject route = obj.getJSONObject("route");
        JSONArray paths = route.getJSONArray("paths");
        if (paths.length() == 0) return result;
        JSONObject path = paths.getJSONObject(0);
        result.totalDistance = path.optInt("distance", 0);
        result.totalDuration = path.optInt("duration", 0);
        JSONArray steps = path.getJSONArray("steps");
        for (int i = 0; i < steps.length(); i++) {
            JSONObject s = steps.getJSONObject(i);
            RouteStep step = new RouteStep();
            step.instruction = s.optString("instruction", "");
            step.orientation = s.optString("orientation", "");
            step.distance = s.optInt("distance", 0);
            step.action = s.optString("action", "");
            String polyStr = s.optString("polyline", "");
            if (!polyStr.isEmpty()) {
                for (String pt : polyStr.split(";")) {
                    String[] coords = pt.split(",");
                    if (coords.length == 2) {
                        step.polyline.add(new double[]{
                                Double.parseDouble(coords[1]),
                                Double.parseDouble(coords[0])
                        });
                    }
                }
            }
            result.steps.add(step);
        }
        return result;
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
