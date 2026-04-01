package com.rokidsmartlife.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rokidsmartlife.R;
import com.rokidsmartlife.api.PoiResult;
import com.rokidsmartlife.utils.RokidKeyHelper;
import com.rokidsmartlife.utils.RokidNavBridge;

public class PoiDetailActivity extends AppCompatActivity {

    private PoiResult poi;
    private View[] focusableViews;
    private int focusIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_detail);

        poi = (PoiResult) getIntent().getSerializableExtra("poi");

        if (poi == null) {
            finish();
            return;
        }

        TextView nameText = findViewById(R.id.detail_name);
        TextView iconText = findViewById(R.id.detail_icon);
        TextView ratingText = findViewById(R.id.detail_rating);
        TextView costText = findViewById(R.id.detail_cost);
        TextView distText = findViewById(R.id.detail_distance);
        TextView typeText = findViewById(R.id.detail_type);
        TextView addressText = findViewById(R.id.detail_address);
        TextView telText = findViewById(R.id.detail_tel);
        TextView openTimeText = findViewById(R.id.detail_opentime);
        View walkBtn = findViewById(R.id.btn_walk);
        View naviBtn = findViewById(R.id.btn_navigate);

        nameText.setText(poi.name);
        iconText.setText(poi.getCategoryIcon());

        String rating = poi.getFormattedRating();
        ratingText.setText(rating.isEmpty() ? "暂无评分" : "★ " + rating);
        ratingText.setTextColor(rating.isEmpty() ? 0xFF888888 : 0xFFFFD700);

        String cost = poi.getFormattedCost();
        costText.setText(cost.isEmpty() ? "" : cost);
        costText.setVisibility(cost.isEmpty() ? View.GONE : View.VISIBLE);

        distText.setText(poi.getFormattedDistance());

        String type = poi.type != null ? poi.type.replace(";", " > ") : "";
        typeText.setText(type);

        addressText.setText(poi.address != null ? poi.address : "地址未知");

        if (poi.tel != null && !poi.tel.isEmpty()) {
            telText.setText("电话: " + poi.tel);
            telText.setVisibility(View.VISIBLE);
        } else {
            telText.setVisibility(View.GONE);
        }

        if (poi.openTime != null && !poi.openTime.isEmpty()) {
            openTimeText.setText("营业: " + poi.openTime);
            openTimeText.setVisibility(View.VISIBLE);
        } else {
            openTimeText.setVisibility(View.GONE);
        }

        // 步行导航 → 调用 Rokid 原生导航
        walkBtn.setFocusable(true);
        walkBtn.setOnClickListener(v -> {
            String dest = poi.name;
            if (poi.address != null && !poi.address.isEmpty()) {
                dest = poi.address + poi.name;
            }
            boolean ok = RokidNavBridge.startNavigation(this, dest, RokidNavBridge.NAVI_TYPE_WALK);
            if (!ok) {
                Toast.makeText(this, "启动导航失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 驾车导航 → 调用 Rokid 原生导航
        naviBtn.setFocusable(true);
        naviBtn.setOnClickListener(v -> {
            String dest = poi.name;
            if (poi.address != null && !poi.address.isEmpty()) {
                dest = poi.address + poi.name;
            }
            boolean ok = RokidNavBridge.startNavigation(this, dest, RokidNavBridge.NAVI_TYPE_DRIVE);
            if (!ok) {
                Toast.makeText(this, "启动导航失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 公交/地铁 → 打开换乘方案页面
        View transitBtn = findViewById(R.id.btn_transit);
        transitBtn.setFocusable(true);
        transitBtn.setOnClickListener(v -> {
            double userLat = getIntent().getDoubleExtra("user_lat", 0);
            double userLng = getIntent().getDoubleExtra("user_lng", 0);
            if (userLat == 0 && userLng == 0) {
                Toast.makeText(this, "当前位置未知，无法规划公交路线", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, TransitRouteActivity.class);
            intent.putExtra("dest_name", poi.name);
            intent.putExtra("origin_lat", userLat);
            intent.putExtra("origin_lng", userLng);
            intent.putExtra("dest_lat", poi.latitude);
            intent.putExtra("dest_lng", poi.longitude);
            intent.putExtra("city", poi.cityname);
            startActivity(intent);
        });

        focusableViews = new View[]{walkBtn, naviBtn, transitBtn};
        walkBtn.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_BACK
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_BACK) {
                finish();
                return true;
            }

            if (keyCode == RokidKeyHelper.KEYCODE_DASHBOARD) {
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_FORWARD) {
                if (focusIndex < focusableViews.length - 1) {
                    focusIndex++;
                    focusableViews[focusIndex].requestFocus();
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (focusIndex > 0) {
                    focusIndex--;
                    focusableViews[focusIndex].requestFocus();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
