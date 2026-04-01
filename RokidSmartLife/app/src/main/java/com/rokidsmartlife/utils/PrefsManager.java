package com.rokidsmartlife.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREFS_NAME = "rokid_smartlife_prefs";
    private static final String KEY_API_KEY = "amap_api_key";
    private static final String KEY_SEARCH_RADIUS = "search_radius";
    private static final String KEY_LAST_LAT = "last_latitude";
    private static final String KEY_LAST_LNG = "last_longitude";
    private static final String KEY_DEFAULT_CITY = "default_city";

    private static final String DEFAULT_API_KEY = "ec54bc7f445760ae1dd4bb7b06a1b451";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, DEFAULT_API_KEY);
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }

    public int getSearchRadius() {
        return prefs.getInt(KEY_SEARCH_RADIUS, 1000);
    }

    public void setSearchRadius(int radius) {
        prefs.edit().putInt(KEY_SEARCH_RADIUS, radius).apply();
    }

    public void saveLastLocation(double lat, double lng) {
        prefs.edit()
                .putFloat(KEY_LAST_LAT, (float) lat)
                .putFloat(KEY_LAST_LNG, (float) lng)
                .apply();
    }

    public double getLastLatitude() {
        return prefs.getFloat(KEY_LAST_LAT, 0f);
    }

    public double getLastLongitude() {
        return prefs.getFloat(KEY_LAST_LNG, 0f);
    }

    public boolean hasLastLocation() {
        return getLastLatitude() != 0 && getLastLongitude() != 0;
    }

    public String getDefaultCity() {
        return prefs.getString(KEY_DEFAULT_CITY, "武汉大学");
    }

    public void setDefaultCity(String city) {
        prefs.edit().putString(KEY_DEFAULT_CITY, city).apply();
    }

    public boolean hasDefaultCity() {
        String city = getDefaultCity();
        return city != null && !city.isEmpty();
    }
}
