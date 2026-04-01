package com.rokidsmartlife.ui;

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
import com.rokidsmartlife.adapter.StepAdapter;
import com.rokidsmartlife.api.AmapApiService;
import com.rokidsmartlife.api.RouteResult;
import com.rokidsmartlife.utils.PrefsManager;
import com.rokidsmartlife.utils.RokidKeyHelper;

public class NavigationActivity extends AppCompatActivity {

    private TextView destNameText;
    private TextView summaryText;
    private TextView currentStepText;
    private RecyclerView stepList;
    private ProgressBar progressBar;
    private View errorView;
    private TextView errorText;

    private RouteResult routeResult;
    private int currentStepIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        destNameText = findViewById(R.id.navi_dest_name);
        summaryText = findViewById(R.id.navi_summary);
        currentStepText = findViewById(R.id.navi_current_step);
        stepList = findViewById(R.id.navi_step_list);
        progressBar = findViewById(R.id.progress_bar);
        errorView = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);

        String destName = getIntent().getStringExtra("dest_name");
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);
        double originLat = getIntent().getDoubleExtra("origin_lat", 0);
        double originLng = getIntent().getDoubleExtra("origin_lng", 0);
        String mode = getIntent().getStringExtra("mode");

        destNameText.setText(destName != null ? destName : "目的地");

        PrefsManager prefs = new PrefsManager(this);
        AmapApiService api = new AmapApiService(prefs.getApiKey());

        progressBar.setVisibility(View.VISIBLE);

        AmapApiService.ApiCallback<RouteResult> callback = new AmapApiService.ApiCallback<RouteResult>() {
            @Override
            public void onSuccess(RouteResult data) {
                progressBar.setVisibility(View.GONE);
                routeResult = data;
                showRoute();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);
                errorText.setText(error);
                Toast.makeText(NavigationActivity.this, error, Toast.LENGTH_LONG).show();
            }
        };

        if ("driving".equals(mode)) {
            api.drivingRoute(originLng, originLat, destLng, destLat, callback);
        } else {
            api.walkingRoute(originLng, originLat, destLng, destLat, callback);
        }
    }

    private void showRoute() {
        if (routeResult == null) return;

        String summary = routeResult.getFormattedDistance() +
                "    预计 " + routeResult.getFormattedDuration();
        summaryText.setText(summary);

        if (routeResult.steps != null && !routeResult.steps.isEmpty()) {
            updateCurrentStep(0);

            StepAdapter adapter = new StepAdapter(routeResult.steps);
            stepList.setLayoutManager(new LinearLayoutManager(this));
            stepList.setAdapter(adapter);
            stepList.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

            stepList.post(() -> {
                View first = stepList.getChildAt(0);
                if (first != null) first.requestFocus();
            });
        }
    }

    private void updateCurrentStep(int index) {
        if (routeResult == null || routeResult.steps == null) return;
        if (index < 0 || index >= routeResult.steps.size()) return;
        currentStepIndex = index;
        RouteResult.Step step = routeResult.steps.get(index);
        currentStepText.setText(step.getActionIcon() + "  " + step.instruction);

        stepList.scrollToPosition(index);
        stepList.post(() -> {
            RecyclerView.ViewHolder vh = stepList.findViewHolderForAdapterPosition(index);
            if (vh != null) vh.itemView.requestFocus();
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

            // 前滑/下滑 = 下一步
            if (RokidKeyHelper.isScrollDown(keyCode)) {
                if (routeResult != null && routeResult.steps != null
                        && currentStepIndex < routeResult.steps.size() - 1) {
                    updateCurrentStep(currentStepIndex + 1);
                }
                return true;
            }

            // 后滑/上滑 = 上一步
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (currentStepIndex > 0) {
                    updateCurrentStep(currentStepIndex - 1);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
