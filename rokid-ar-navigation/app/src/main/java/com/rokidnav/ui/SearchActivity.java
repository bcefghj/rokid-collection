package com.rokidnav.ui;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidnav.R;
import com.rokidnav.util.AmapApi;
import com.rokidnav.util.KeyHelper;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private static final int REQ_VOICE = 300;
    private RecyclerView rvResult;
    private PoiAdapter adapter;
    private TextView tvListening, tvSearchStatus;
    private double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        tvListening = findViewById(R.id.tvListening);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        rvResult = findViewById(R.id.rvSearchResult);
        rvResult.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PoiAdapter();
        adapter.setListener(new PoiAdapter.OnItemActionListener() {
            @Override
            public void onItemSelected(AmapApi.PoiResult poi, int position) {
                rvResult.smoothScrollToPosition(position);
            }

            @Override
            public void onItemConfirmed(AmapApi.PoiResult poi, int position) {
                startNavigation(poi);
            }
        });
        rvResult.setAdapter(adapter);

        lat = getIntent().getDoubleExtra("lat", 0);
        lng = getIntent().getDoubleExtra("lng", 0);

        startVoice();
    }

    private void startVoice() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "说出目的地…");
            startActivityForResult(intent, REQ_VOICE);
        } catch (Exception e) {
            tvListening.setText("语音服务不可用，请返回");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String keyword = results.get(0);
                tvListening.setText("搜索: " + keyword);
                doSearch(keyword);
            }
        } else {
            tvListening.setText("未识别到语音，按PROG1重试");
        }
    }

    private void doSearch(String keyword) {
        tvSearchStatus.setText("搜索中…");
        if (lat != 0 || lng != 0) {
            AmapApi.searchKeywordNearby(keyword, lat, lng,
                    new AmapApi.Callback<List<AmapApi.PoiResult>>() {
                        @Override
                        public void onSuccess(List<AmapApi.PoiResult> result) {
                            adapter.setData(result);
                            tvSearchStatus.setText(result.size() + "个结果 · ↑↓选择 · 确认导航");
                        }

                        @Override
                        public void onError(String error) {
                            tvSearchStatus.setText("搜索失败: " + error);
                        }
                    });
        } else {
            AmapApi.searchKeyword(keyword, "", new AmapApi.Callback<List<AmapApi.PoiResult>>() {
                @Override
                public void onSuccess(List<AmapApi.PoiResult> result) {
                    adapter.setData(result);
                    tvSearchStatus.setText(result.size() + "个结果 · ↑↓选择 · 确认导航");
                }

                @Override
                public void onError(String error) {
                    tvSearchStatus.setText("搜索失败: " + error);
                }
            });
        }
    }

    private void startNavigation(AmapApi.PoiResult poi) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("dest_name", poi.name);
        intent.putExtra("dest_lat", poi.lat);
        intent.putExtra("dest_lng", poi.lng);
        intent.putExtra("from_lat", lat);
        intent.putExtra("from_lng", lng);
        startActivity(intent);
        finish();
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
        if (KeyHelper.isConfirm(keyCode)) {
            adapter.confirm();
            return true;
        }
        if (KeyHelper.isVoice(keyCode)) {
            startVoice();
            return true;
        }
        if (KeyHelper.isBack(keyCode)) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
