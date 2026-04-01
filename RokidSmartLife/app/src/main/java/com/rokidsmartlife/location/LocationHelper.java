package com.rokidsmartlife.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.List;

public class LocationHelper {
    private static final String TAG = "LocationHelper";

    private final Context context;
    private final LocationManager locationManager;
    private final Handler mainHandler;
    private LocationCallback callback;
    private LocationListener networkListener;
    private LocationListener gpsListener;
    private boolean isListening = false;

    public LocationHelper(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocation(LocationCallback callback) {
        this.callback = callback;

        if (!hasPermission()) {
            Log.w(TAG, "No location permission");
            mainHandler.post(() -> callback.onError("缺少定位权限"));
            return;
        }

        Location lastKnown = getLastKnownLocation();
        Log.d(TAG, "lastKnown: " + (lastKnown != null ? lastKnown.getLatitude() + "," + lastKnown.getLongitude() + " age=" + (System.currentTimeMillis() - lastKnown.getTime()) + "ms" : "null"));

        if (lastKnown != null) {
            long age = System.currentTimeMillis() - lastKnown.getTime();
            if (age < 5 * 60 * 1000) {
                Log.d(TAG, "Using recent lastKnown location");
                mainHandler.post(() -> callback.onLocationReceived(lastKnown.getLatitude(), lastKnown.getLongitude()));
                return;
            }
        }

        startListening();

        mainHandler.postDelayed(() -> {
            if (isListening) {
                stopListening();
                Log.w(TAG, "Location timeout after 5s, lastKnown=" + (lastKnown != null));
                if (lastKnown != null) {
                    callback.onLocationReceived(lastKnown.getLatitude(), lastKnown.getLongitude());
                } else {
                    callback.onError("定位超时");
                }
            }
        }, 5000);
    }

    @SuppressWarnings("MissingPermission")
    private void startListening() {
        if (isListening) return;
        isListening = true;

        try {
            List<String> providers = locationManager.getProviders(true);
            Log.d(TAG, "Available providers: " + providers);

            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                networkListener = createListener("Network");
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 1000, 1, networkListener);
            }

            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                gpsListener = createListener("GPS");
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
            }

            if (providers.contains("fused")) {
                LocationListener fusedListener = createListener("Fused");
                locationManager.requestLocationUpdates("fused", 1000, 1, fusedListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("启动定位失败: " + e.getMessage()));
            }
        }
    }

    private LocationListener createListener(String providerName) {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, providerName + " location: " + location.getLatitude() + "," + location.getLongitude());
                if (callback != null && isListening) {
                    stopListening();
                    mainHandler.post(() -> callback.onLocationReceived(
                            location.getLatitude(), location.getLongitude()));
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    @SuppressWarnings("MissingPermission")
    private Location getLastKnownLocation() {
        if (!hasPermission()) return null;
        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location loc = locationManager.getLastKnownLocation(provider);
                if (loc != null && (best == null || loc.getTime() > best.getTime())) {
                    best = loc;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location", e);
        }
        return best;
    }

    public void stopListening() {
        isListening = false;
        try {
            if (networkListener != null) {
                locationManager.removeUpdates(networkListener);
                networkListener = null;
            }
            if (gpsListener != null) {
                locationManager.removeUpdates(gpsListener);
                gpsListener = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location updates", e);
        }
    }

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onError(String error);
    }
}
