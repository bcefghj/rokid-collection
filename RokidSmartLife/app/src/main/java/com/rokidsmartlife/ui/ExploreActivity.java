package com.rokidsmartlife.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.adapter.PoiListAdapter;
import com.rokidsmartlife.api.AmapApiService;
import com.rokidsmartlife.api.PoiResult;
import com.rokidsmartlife.utils.PrefsManager;
import com.rokidsmartlife.utils.RokidKeyHelper;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private RecyclerView poiList;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView titleText;
    private PoiListAdapter adapter;
    private AmapApiService apiService;
    private PrefsManager prefsManager;

    private String categoryType;
    private String categoryName;
    private double latitude, longitude;
    private int currentPage = 1;
    private boolean isLoading = false;
    private int currentFocusPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        prefsManager = new PrefsManager(this);
        apiService = new AmapApiService(prefsManager.getApiKey());

        categoryName = getIntent().getStringExtra("category_name");
        categoryType = getIntent().getStringExtra("category_type");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        titleText = findViewById(R.id.title_text);
        poiList = findViewById(R.id.poi_list);
        progressBar = findViewById(R.id.progress_bar);
        emptyText = findViewById(R.id.empty_text);

        titleText.setText("附近" + (categoryName != null ? categoryName : ""));

        adapter = new PoiListAdapter(poi -> {
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra("poi", poi);
            intent.putExtra("user_lat", latitude);
            intent.putExtra("user_lng", longitude);
            startActivity(intent);
        });

        LinearLayoutManager lm = new LinearLayoutManager(this);
        poiList.setLayoutManager(lm);
        poiList.setAdapter(adapter);
        poiList.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

        loadData();
    }

    private void loadData() {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        int radius = prefsManager.getSearchRadius();
        apiService.searchNearby(longitude, latitude, categoryType, radius, currentPage,
                new AmapApiService.ApiCallback<List<PoiResult>>() {
                    @Override
                    public void onSuccess(List<PoiResult> data) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        if (data.isEmpty() && currentPage == 1) {
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("附近没有找到" + categoryName);
                        } else {
                            if (currentPage == 1) {
                                adapter.setItems(data);
                            } else {
                                adapter.addItems(data);
                            }
                            poiList.post(() -> focusItem(0));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText(error);
                        Toast.makeText(ExploreActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void focusItem(int position) {
        if (position < 0 || position >= adapter.getItemCount()) return;
        currentFocusPos = position;
        poiList.scrollToPosition(position);
        poiList.post(() -> {
            RecyclerView.ViewHolder vh = poiList.findViewHolderForAdapterPosition(position);
            if (vh != null && vh.itemView != null) {
                vh.itemView.requestFocus();
            }
        });
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

            // 前滑 = 下一项
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_FORWARD) {
                int next = currentFocusPos + 1;
                if (next < adapter.getItemCount()) {
                    focusItem(next);
                } else if (!isLoading) {
                    currentPage++;
                    loadData();
                }
                return true;
            }

            // 后滑 = 上一项
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                int prev = currentFocusPos - 1;
                if (prev >= 0) {
                    focusItem(prev);
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
