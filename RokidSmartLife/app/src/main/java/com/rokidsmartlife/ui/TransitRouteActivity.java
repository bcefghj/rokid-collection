package com.rokidsmartlife.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rokidsmartlife.R;
import com.rokidsmartlife.adapter.TransitPlanAdapter;
import com.rokidsmartlife.adapter.TransitSegmentAdapter;
import com.rokidsmartlife.api.AmapApiService;
import com.rokidsmartlife.api.TransitResult;
import com.rokidsmartlife.utils.PrefsManager;
import com.rokidsmartlife.utils.RokidKeyHelper;

public class TransitRouteActivity extends AppCompatActivity {

    private RecyclerView planList;
    private RecyclerView segmentList;
    private LinearLayout detailContainer;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView backPlansBtn;

    private boolean showingDetail = false;
    private int currentFocusPos = 0;
    private TransitResult transitResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transit_route);

        String destName = getIntent().getStringExtra("dest_name");
        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);
        String city = getIntent().getStringExtra("city");

        TextView titleText = findViewById(R.id.transit_title);
        TextView subtitleText = findViewById(R.id.transit_subtitle);
        planList = findViewById(R.id.plan_list);
        segmentList = findViewById(R.id.segment_list);
        detailContainer = findViewById(R.id.detail_container);
        progressBar = findViewById(R.id.progress_bar);
        emptyText = findViewById(R.id.empty_text);
        backPlansBtn = findViewById(R.id.btn_back_plans);
        TextView detailPlanInfo = findViewById(R.id.detail_plan_info);

        titleText.setText("\uD83D\uDE87 公交/地铁 → " + (destName != null ? destName : "目的地"));
        subtitleText.setText("正在查询换乘方案...");

        planList.setLayoutManager(new LinearLayoutManager(this));
        planList.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

        segmentList.setLayoutManager(new LinearLayoutManager(this));
        segmentList.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

        backPlansBtn.setOnClickListener(v -> showPlanList());

        progressBar.setVisibility(View.VISIBLE);

        PrefsManager prefs = new PrefsManager(this);
        AmapApiService apiService = new AmapApiService(prefs.getApiKey());
        apiService.transitRoute(originLng, originLat, destLng, destLat,
                city != null ? city : prefs.getDefaultCity(),
                new AmapApiService.ApiCallback<TransitResult>() {
                    @Override
                    public void onSuccess(TransitResult data) {
                        progressBar.setVisibility(View.GONE);
                        transitResult = data;
                        if (data.plans == null || data.plans.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("未找到公交/地铁路线");
                            subtitleText.setText("无可用换乘方案");
                            return;
                        }
                        subtitleText.setText(data.plans.size() + " 个换乘方案");
                        showPlanResults(data);
                    }

                    @Override
                    public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText(error);
                        subtitleText.setText("查询失败");
                        Toast.makeText(TransitRouteActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showPlanResults(TransitResult data) {
        TransitPlanAdapter adapter = new TransitPlanAdapter(data.plans, (plan, position) -> {
            showPlanDetail(plan, position);
        });
        planList.setAdapter(adapter);
        planList.setVisibility(View.VISIBLE);
        detailContainer.setVisibility(View.GONE);
        showingDetail = false;
        currentFocusPos = 0;
        planList.post(() -> focusItem(planList, 0));
    }

    private void showPlanDetail(TransitResult.TransitPlan plan, int index) {
        if (plan.segments == null || plan.segments.isEmpty()) {
            Toast.makeText(this, "该方案无详细换乘信息", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView detailPlanInfo = findViewById(R.id.detail_plan_info);
        detailPlanInfo.setText("方案 " + (index + 1) + " · " + plan.getFormattedDuration() + " · " + plan.getFormattedCost());

        TransitSegmentAdapter segAdapter = new TransitSegmentAdapter(plan.segments);
        segmentList.setAdapter(segAdapter);

        planList.setVisibility(View.GONE);
        detailContainer.setVisibility(View.VISIBLE);
        showingDetail = true;
        currentFocusPos = 0;
        segmentList.post(() -> focusItem(segmentList, 0));
    }

    private void showPlanList() {
        planList.setVisibility(View.VISIBLE);
        detailContainer.setVisibility(View.GONE);
        showingDetail = false;
        currentFocusPos = 0;
        planList.post(() -> focusItem(planList, 0));
    }

    private void focusItem(RecyclerView rv, int position) {
        RecyclerView.Adapter<?> adapter = rv.getAdapter();
        if (adapter == null || position < 0 || position >= adapter.getItemCount()) return;
        currentFocusPos = position;
        rv.scrollToPosition(position);
        rv.post(() -> {
            RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
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
                if (showingDetail) {
                    showPlanList();
                    return true;
                }
                finish();
                return true;
            }

            if (keyCode == RokidKeyHelper.KEYCODE_DASHBOARD) {
                return true;
            }

            RecyclerView activeList = showingDetail ? segmentList : planList;
            RecyclerView.Adapter<?> adapter = activeList.getAdapter();
            int itemCount = adapter != null ? adapter.getItemCount() : 0;

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_FORWARD) {
                int next = currentFocusPos + 1;
                if (next < itemCount) {
                    focusItem(activeList, next);
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                int prev = currentFocusPos - 1;
                if (prev >= 0) {
                    focusItem(activeList, prev);
                } else {
                    if (showingDetail) {
                        showPlanList();
                    } else {
                        finish();
                    }
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
