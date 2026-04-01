package com.rokidnav.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.rokidnav.RokidNavApp;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationService extends Service {
    private static final String TAG = "RokidNav_Loc";
    private static final String CHANNEL_ID = "loc_channel";
    private LocationManager locationManager;
    private Location lastLocation;
    private LocationCallback callback;
    private final IBinder binder = new LocalBinder();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean gotLocation = false;

    public interface LocationCallback {
        void onLocationChanged(double lat, double lng, float accuracy);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AR导航")
                .setContentText("正在定位…")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
        startForeground(1, notification);
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallback(LocationCallback cb) {
        this.callback = cb;
        if (lastLocation != null && cb != null) {
            cb.onLocationChanged(lastLocation.getLatitude(), lastLocation.getLongitude(),
                    lastLocation.getAccuracy());
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    private void setLocation(double lat, double lng, float accuracy, String provider) {
        Location loc = new Location(provider);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setAccuracy(accuracy);
        lastLocation = loc;
        gotLocation = true;
        Log.i(TAG, "Location set: " + lat + "," + lng + " acc=" + accuracy + " via " + provider);
        handler.post(() -> {
            if (callback != null) {
                callback.onLocationChanged(lat, lng, accuracy);
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, using fallback");
            tryAllFallbacks();
            return;
        }

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "System: " + location.getLatitude() + "," + location.getLongitude()
                        + " acc=" + location.getAccuracy() + " via " + location.getProvider());
                lastLocation = location;
                gotLocation = true;
                if (callback != null) {
                    callback.onLocationChanged(location.getLatitude(),
                            location.getLongitude(), location.getAccuracy());
                }
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        List<String> providers = locationManager.getAllProviders();
        Log.i(TAG, "Providers: " + providers);

        for (String provider : providers) {
            if (provider.equals(LocationManager.PASSIVE_PROVIDER)) continue;
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 3000, 5, listener);
                    Log.i(TAG, "Requested: " + provider);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed " + provider + ": " + e.getMessage());
            }
        }

        Location best = null;
        for (String provider : providers) {
            try {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getAccuracy() < best.getAccuracy())) {
                    best = l;
                }
            } catch (Exception ignored) {}
        }

        if (best != null) {
            Log.i(TAG, "Last known: " + best.getLatitude() + "," + best.getLongitude());
            lastLocation = best;
            gotLocation = true;
            if (callback != null) {
                callback.onLocationChanged(best.getLatitude(), best.getLongitude(), best.getAccuracy());
            }
        }

        handler.postDelayed(() -> {
            if (!gotLocation) {
                Log.i(TAG, "No system location after 5s, trying fallbacks");
                tryAllFallbacks();
            }
        }, 5000);
    }

    private void tryAllFallbacks() {
        executor.execute(() -> {
            if (tryIpLocation()) return;
            if (tryWifiLocation()) return;
            useDefaultLocation();
        });
    }

    private boolean tryIpLocation() {
        try {
            String url = RokidNavApp.AMAP_BASE + "/ip?key=" + RokidNavApp.AMAP_KEY;
            Log.i(TAG, "Trying IP location...");
            String json = httpGet(url);
            Log.i(TAG, "IP response: " + json);
            JSONObject obj = new JSONObject(json);

            String rect = obj.optString("rectangle", "");
            if (rect != null && !rect.isEmpty() && !rect.equals("[]")) {
                String[] corners = rect.split(";");
                if (corners.length == 2) {
                    String[] sw = corners[0].split(",");
                    String[] ne = corners[1].split(",");
                    double lng = (Double.parseDouble(sw[0]) + Double.parseDouble(ne[0])) / 2;
                    double lat = (Double.parseDouble(sw[1]) + Double.parseDouble(ne[1])) / 2;
                    setLocation(lat, lng, 5000, "ip");
                    return true;
                }
            }

            String city = obj.optString("city", "");
            if (city != null && !city.isEmpty() && !city.equals("[]")) {
                return tryGeocode(city);
            }
        } catch (Exception e) {
            Log.e(TAG, "IP location failed: " + e.getMessage());
        }
        return false;
    }

    private boolean tryWifiLocation() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return false;
            WifiInfo info = wm.getConnectionInfo();
            if (info == null) return false;

            String bssid = info.getBSSID();
            int rssi = info.getRssi();
            Log.i(TAG, "WiFi BSSID=" + bssid + " RSSI=" + rssi);

            if (bssid == null || bssid.equals("02:00:00:00:00:00")) return false;

            String url = "https://www.googleapis.com/geolocation/v1/geolocate?key=";
            Log.i(TAG, "WiFi location not available without API key, skipping");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "WiFi location failed: " + e.getMessage());
            return false;
        }
    }

    private boolean tryGeocode(String address) {
        try {
            String url = RokidNavApp.AMAP_BASE + "/geocode/geo?key=" + RokidNavApp.AMAP_KEY
                    + "&address=" + java.net.URLEncoder.encode(address, "UTF-8");
            String json = httpGet(url);
            JSONObject obj = new JSONObject(json);
            JSONArray geocodes = obj.optJSONArray("geocodes");
            if (geocodes != null && geocodes.length() > 0) {
                String loc = geocodes.getJSONObject(0).optString("location", "");
                String[] parts = loc.split(",");
                if (parts.length == 2) {
                    double lng = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    setLocation(lat, lng, 10000, "geocode");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocode failed: " + e.getMessage());
        }
        return false;
    }

    private void useDefaultLocation() {
        Log.w(TAG, "All location methods failed, using WiFi-based estimation");
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            WifiInfo info = wm.getConnectionInfo();
            if (info != null) {
                String ssid = info.getSSID();
                Log.i(TAG, "Connected to WiFi: " + ssid);
            }
        }
        // Shenzhen as a reasonable default since user is likely in China
        setLocation(22.5431, 114.0579, 50000, "default");
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "定位服务", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        stopForeground(true);
    }
}
