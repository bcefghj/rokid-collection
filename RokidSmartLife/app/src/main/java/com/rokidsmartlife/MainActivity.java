package com.rokidsmartlife;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.adapter.CategoryAdapter;
import com.rokidsmartlife.location.LocationHelper;
import com.rokidsmartlife.location.WifiLocationHelper;
import com.rokidsmartlife.ui.ExploreActivity;
import com.rokidsmartlife.ui.SearchActivity;
import com.rokidsmartlife.ui.SettingsActivity;
import com.rokidsmartlife.utils.PoiCategory;
import com.rokidsmartlife.utils.PrefsManager;
import com.rokidsmartlife.utils.RokidKeyHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private RecyclerView categoryGrid;
    private TextView locationText;
    private PrefsManager prefsManager;
    private LocationHelper locationHelper;
    private WifiLocationHelper wifiLocationHelper;
    private double currentLat = 0, currentLng = 0;
    private int focusedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsManager = new PrefsManager(this);
        locationHelper = new LocationHelper(this);
        wifiLocationHelper = new WifiLocationHelper(this);

        locationText = findViewById(R.id.location_text);
        categoryGrid = findViewById(R.id.category_grid);
        View searchBtn = findViewById(R.id.btn_search);
        View settingsBtn = findViewById(R.id.btn_settings);

        CategoryAdapter adapter = new CategoryAdapter(PoiCategory.CATEGORIES, category -> {
            if (!prefsManager.hasApiKey()) {
                Toast.makeText(this, "请先在设置中配置高德地图 API Key", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SettingsActivity.class));
                return;
            }
            Intent intent = new Intent(this, ExploreActivity.class);
            intent.putExtra("category_name", category.name);
            intent.putExtra("category_type", category.typeCode);
            intent.putExtra("latitude", currentLat);
            intent.putExtra("longitude", currentLng);
            startActivity(intent);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        categoryGrid.setLayoutManager(layoutManager);
        categoryGrid.setAdapter(adapter);
        categoryGrid.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

        searchBtn.setOnClickListener(v -> {
            if (!prefsManager.hasApiKey()) {
                Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SettingsActivity.class));
                return;
            }
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("latitude", currentLat);
            intent.putExtra("longitude", currentLng);
            startActivity(intent);
        });

        settingsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        categoryGrid.post(() -> {
            View first = categoryGrid.getChildAt(0);
            if (first != null) first.requestFocus();
        });

        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationHelper.hasPermission()) {
            updateLocation();
        }
    }

    private void requestPermissions() {
        if (!locationHelper.hasPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, REQUEST_PERMISSIONS);
        } else {
            updateLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                updateLocation();
            } else {
                locationText.setText("需要定位权限");
            }
        }
    }

    private static final String TAG = "SmartLife";

    private void updateLocation() {
        locationText.setText("定位中...");
        Log.d(TAG, "updateLocation: starting, hasApiKey=" + prefsManager.hasApiKey());

        locationHelper.requestLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                Log.d(TAG, "System location received: " + latitude + "," + longitude);
                onLocationObtained(latitude, longitude);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "System location error: " + error + ", trying WiFi locate");
                tryWifiLocate();
            }
        });
    }

    private void tryWifiLocate() {
        if (!prefsManager.hasApiKey() || !wifiLocationHelper.isWifiEnabled()) {
            Log.w(TAG, "WiFi locate skipped: apiKey=" + prefsManager.hasApiKey() + " wifiEnabled=" + wifiLocationHelper.isWifiEnabled());
            tryIpLocate();
            return;
        }
        locationText.setText("WiFi定位中...");
        String connMac = wifiLocationHelper.getConnectedWifiMac();
        String nearbyMacs = wifiLocationHelper.getNearbyWifiMacs();
        Log.d(TAG, "WiFi locate: connMac=" + connMac + " nearbyMacs=" + (nearbyMacs != null ? nearbyMacs.length() + " chars" : "null"));

        if (nearbyMacs == null) {
            Log.w(TAG, "Not enough nearby WiFi for location, falling back");
            tryIpLocate();
            return;
        }

        new com.rokidsmartlife.api.AmapApiService(prefsManager.getApiKey())
                .wifiLocate(connMac, nearbyMacs, new com.rokidsmartlife.api.AmapApiService.ApiCallback<double[]>() {
                    @Override
                    public void onSuccess(double[] latLng) {
                        Log.d(TAG, "WiFi locate success: " + latLng[0] + "," + latLng[1]);
                        onLocationObtained(latLng[0], latLng[1]);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "WiFi locate failed: " + error + ", trying IP locate");
                        tryIpLocate();
                    }
                });
    }

    private void tryIpLocate() {
        if (!prefsManager.hasApiKey()) {
            Log.e(TAG, "No API key configured");
            locationText.setText("请配置API Key");
            return;
        }
        locationText.setText("IP定位中...");
        new com.rokidsmartlife.api.AmapApiService(prefsManager.getApiKey())
                .ipLocate(new com.rokidsmartlife.api.AmapApiService.ApiCallback<double[]>() {
                    @Override
                    public void onSuccess(double[] latLng) {
                        Log.d(TAG, "IP locate success: " + latLng[0] + "," + latLng[1]);
                        onLocationObtained(latLng[0], latLng[1]);
                    }

                    @Override
                    public void onError(String ipError) {
                        Log.w(TAG, "IP locate failed: " + ipError + ", trying default city");
                        tryCityGeocode();
                    }
                });
    }

    private void tryCityGeocode() {
        if (prefsManager.hasDefaultCity() && prefsManager.hasApiKey()) {
            String city = prefsManager.getDefaultCity();
            locationText.setText("定位: " + city);
            Log.d(TAG, "Geocoding default city: " + city);
            new com.rokidsmartlife.api.AmapApiService(prefsManager.getApiKey())
                    .geocodeCity(city, new com.rokidsmartlife.api.AmapApiService.ApiCallback<double[]>() {
                        @Override
                        public void onSuccess(double[] latLng) {
                            Log.d(TAG, "City geocode success: " + latLng[0] + "," + latLng[1]);
                            onLocationObtained(latLng[0], latLng[1]);
                        }

                        @Override
                        public void onError(String geoError) {
                            Log.e(TAG, "City geocode failed: " + geoError);
                            useLastLocationOrFail();
                        }
                    });
        } else {
            useLastLocationOrFail();
        }
    }

    private void useLastLocationOrFail() {
        if (prefsManager.hasLastLocation()) {
            currentLat = prefsManager.getLastLatitude();
            currentLng = prefsManager.getLastLongitude();
            locationText.setText("使用上次位置");
            Log.d(TAG, "Using last location: " + currentLat + "," + currentLng);
        } else {
            locationText.setText("请在设置中配置默认城市");
        }
    }

    private void onLocationObtained(double latitude, double longitude) {
        currentLat = latitude;
        currentLng = longitude;
        prefsManager.saveLastLocation(latitude, longitude);
        locationText.setText(String.format("%.4f, %.4f", latitude, longitude));

        if (prefsManager.hasApiKey()) {
            new com.rokidsmartlife.api.AmapApiService(prefsManager.getApiKey())
                    .reverseGeocode(longitude, latitude,
                            new com.rokidsmartlife.api.AmapApiService.ApiCallback<String>() {
                                @Override
                                public void onSuccess(String address) {
                                    if (address != null && !address.isEmpty()) {
                                        String shortAddr = address.length() > 20
                                                ? address.substring(address.length() - 20)
                                                : address;
                                        locationText.setText(shortAddr);
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                }
                            });
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (RokidKeyHelper.isForwardSwipe(keyCode) || keyCode == RokidKeyHelper.KEYCODE_DASHBOARD) {
                return moveFocus(1);
            }

            if (RokidKeyHelper.isBackSwipe(keyCode) && keyCode != KeyEvent.KEYCODE_BACK) {
                return moveFocus(-1);
            }
        }

        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
                return true;
            }

            if (RokidKeyHelper.isForwardSwipe(keyCode)
                    || keyCode == RokidKeyHelper.KEYCODE_DASHBOARD
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_BACK) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean moveFocus(int direction) {
        int totalItems = categoryGrid.getAdapter() != null ? categoryGrid.getAdapter().getItemCount() : 0;
        if (totalItems == 0) return false;

        int newIndex = focusedIndex + direction;
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= totalItems) newIndex = totalItems - 1;

        if (newIndex != focusedIndex) {
            focusedIndex = newIndex;
            categoryGrid.smoothScrollToPosition(focusedIndex);
            categoryGrid.post(() -> {
                RecyclerView.ViewHolder vh = categoryGrid.findViewHolderForAdapterPosition(focusedIndex);
                if (vh != null && vh.itemView != null) {
                    vh.itemView.requestFocus();
                }
            });
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopListening();
        }
    }
}
