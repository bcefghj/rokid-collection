package com.rokidnav.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidnav.R;
import com.rokidnav.service.LocationService;
import com.rokidnav.util.AmapApi;
import com.rokidnav.util.KeyHelper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RokidNav_Main";
    private static final int REQ_PERM = 100;
    private static final int REQ_VOICE = 200;

    private RecyclerView rvPoi;
    private PoiAdapter adapter;
    private TextView tvTitle, tvLocation, tvStatus;
    private TextView tabNearby, tabHotel, tabShop, tabLife;
    private TextView[] tabs;

    private LocationService locationService;
    private boolean bound = false;
    private double curLat = 0, curLng = 0;
    private int currentTab = 0;

    private static final String[] TAB_TYPES = {
            "050000",   // 餐饮
            "100000",   // 酒店
            "060000",   // 购物
            "070000"    // 生活服务
    };
    private static final String[] TAB_NAMES = {"餐饮", "酒店", "购物", "生活"};

    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            bound = true;
            locationService.setCallback((lat, lng, accuracy) -> {
                Log.i(TAG, "Location callback: " + lat + "," + lng + " ±" + accuracy);
                curLat = lat;
                curLng = lng;
                tvLocation.setText(String.format("%.4f, %.4f (±%.0fm)", lat, lng, accuracy));
                loadNearby();
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tabNearby = findViewById(R.id.tabNearby);
        tabHotel = findViewById(R.id.tabHotel);
        tabShop = findViewById(R.id.tabShop);
        tabLife = findViewById(R.id.tabLife);
        tabs = new TextView[]{tabNearby, tabHotel, tabShop, tabLife};

        rvPoi = findViewById(R.id.rvPoi);
        rvPoi.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PoiAdapter();
        adapter.setListener(new PoiAdapter.OnItemActionListener() {
            @Override
            public void onItemSelected(AmapApi.PoiResult poi, int position) {
                rvPoi.smoothScrollToPosition(position);
            }

            @Override
            public void onItemConfirmed(AmapApi.PoiResult poi, int position) {
                startNavigation(poi);
            }
        });
        rvPoi.setAdapter(adapter);

        checkPermissions();
    }

    private void checkPermissions() {
        String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        List<String> needed = new ArrayList<>();
        for (String p : perms) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (needed.isEmpty()) {
            startLocService();
        } else {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            startLocService();
        }
    }

    private void startLocService() {
        Intent intent = new Intent(this, LocationService.class);
        startForegroundService(intent);
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    private void loadNearby() {
        if (curLat == 0 && curLng == 0) return;
        tvTitle.setText("附近" + TAB_NAMES[currentTab]);
        tvStatus.setText("搜索中…");

        AmapApi.searchNearby(curLat, curLng, TAB_TYPES[currentTab], 3000,
                new AmapApi.Callback<List<AmapApi.PoiResult>>() {
                    @Override
                    public void onSuccess(List<AmapApi.PoiResult> result) {
                        adapter.setData(result);
                        tvStatus.setText(result.size() + "个结果 · ↑↓选择 · 确认导航 · PROG1语音");
                    }

                    @Override
                    public void onError(String error) {
                        tvStatus.setText("搜索失败: " + error);
                    }
                });
    }

    private void switchTab(int direction) {
        currentTab = (currentTab + direction + tabs.length) % tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == currentTab ? 0xFF00E5FF : 0xFFB0B0B0);
            tabs[i].setBackgroundColor(i == currentTab ? 0xCC1A1A2E : 0x00000000);
        }
        loadNearby();
    }

    private void startNavigation(AmapApi.PoiResult poi) {
        if (curLat == 0 && curLng == 0) {
            Toast.makeText(this, "等待定位…", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("dest_name", poi.name);
        intent.putExtra("dest_lat", poi.lat);
        intent.putExtra("dest_lng", poi.lng);
        intent.putExtra("from_lat", curLat);
        intent.putExtra("from_lng", curLng);
        startActivity(intent);
    }

    private void startVoiceSearch() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "说出目的地…");
            startActivityForResult(intent, REQ_VOICE);
        } catch (Exception e) {
            Intent searchIntent = new Intent(this, SearchActivity.class);
            searchIntent.putExtra("lat", curLat);
            searchIntent.putExtra("lng", curLng);
            startActivity(searchIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String keyword = results.get(0);
                searchByKeyword(keyword);
            }
        }
    }

    private void searchByKeyword(String keyword) {
        tvTitle.setText("搜索: " + keyword);
        tvStatus.setText("搜索中…");
        if (curLat != 0 || curLng != 0) {
            AmapApi.searchKeywordNearby(keyword, curLat, curLng,
                    new AmapApi.Callback<List<AmapApi.PoiResult>>() {
                        @Override
                        public void onSuccess(List<AmapApi.PoiResult> result) {
                            adapter.setData(result);
                            tvStatus.setText(result.size() + "个结果 · ↑↓选择 · 确认导航");
                        }

                        @Override
                        public void onError(String error) {
                            tvStatus.setText("搜索失败: " + error);
                        }
                    });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyHelper.isUp(keyCode)) {
            adapter.moveUp();
            return true;
        }
        if (KeyHelper.isDown(keyCode)) {
            adapter.moveDown();
            return true;
        }
        if (KeyHelper.isLeft(keyCode)) {
            switchTab(-1);
            return true;
        }
        if (KeyHelper.isRight(keyCode)) {
            switchTab(1);
            return true;
        }
        if (KeyHelper.isConfirm(keyCode)) {
            adapter.confirm();
            return true;
        }
        if (KeyHelper.isVoice(keyCode)) {
            startVoiceSearch();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(serviceConn);
            bound = false;
        }
    }
}
