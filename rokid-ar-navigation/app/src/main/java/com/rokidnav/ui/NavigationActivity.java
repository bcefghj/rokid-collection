package com.rokidnav.ui;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rokidnav.R;
import com.rokidnav.util.AmapApi;
import com.rokidnav.util.KeyHelper;
import java.util.Locale;

public class NavigationActivity extends AppCompatActivity {
    private TextView tvDestName, tvArrow, tvInstruction, tvStepDist;
    private TextView tvTotalDist, tvTotalTime, tvStepProgress;
    private TextToSpeech tts;
    private AmapApi.RouteResult route;
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        tvDestName = findViewById(R.id.tvDestName);
        tvArrow = findViewById(R.id.tvArrow);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvStepDist = findViewById(R.id.tvStepDist);
        tvTotalDist = findViewById(R.id.tvTotalDist);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvStepProgress = findViewById(R.id.tvStepProgress);

        String destName = getIntent().getStringExtra("dest_name");
        double destLat = getIntent().getDoubleExtra("dest_lat", 0);
        double destLng = getIntent().getDoubleExtra("dest_lng", 0);
        double fromLat = getIntent().getDoubleExtra("from_lat", 0);
        double fromLng = getIntent().getDoubleExtra("from_lng", 0);

        tvDestName.setText("→ " + destName);
        tvInstruction.setText("路线规划中…");
        tvArrow.setText("⏳");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.CHINESE);
            }
        });

        AmapApi.walkRoute(fromLat, fromLng, destLat, destLng,
                new AmapApi.Callback<AmapApi.RouteResult>() {
                    @Override
                    public void onSuccess(AmapApi.RouteResult result) {
                        route = result;
                        tvTotalDist.setText("总距离: " + formatDist(result.totalDistance));
                        tvTotalTime.setText("约" + formatTime(result.totalDuration));
                        showStep(0);
                        speak("开始步行导航，" + formatDist(result.totalDistance) + "，预计"
                                + formatTime(result.totalDuration));
                    }

                    @Override
                    public void onError(String error) {
                        tvInstruction.setText("路线规划失败");
                        tvArrow.setText("✕");
                        Toast.makeText(NavigationActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showStep(int index) {
        if (route == null || route.steps.isEmpty()) return;
        if (index >= route.steps.size()) {
            tvArrow.setText("✓");
            tvArrow.setTextColor(0xFF00E676);
            tvInstruction.setText("您已到达目的地！");
            tvStepDist.setText("");
            tvStepProgress.setText("到达");
            speak("您已到达目的地");
            return;
        }

        currentStep = index;
        AmapApi.RouteStep step = route.steps.get(index);
        tvInstruction.setText(step.instruction);
        tvStepDist.setText(formatDist(step.distance));
        tvStepProgress.setText("步骤 " + (index + 1) + "/" + route.steps.size());
        tvArrow.setText(actionToArrow(step.action));
        tvArrow.setTextColor(0xFF00E5FF);

        speak(step.instruction);
    }

    private String actionToArrow(String action) {
        if (action == null) return "↑";
        switch (action) {
            case "左转": return "←";
            case "右转": return "→";
            case "向左前方": return "↖";
            case "向右前方": return "↗";
            case "向左后方": return "↙";
            case "向右后方": return "↘";
            case "掉头": return "↓";
            case "到达": return "✓";
            default: return "↑";
        }
    }

    private String formatDist(int meters) {
        if (meters >= 1000) {
            return String.format("%.1f公里", meters / 1000.0);
        }
        return meters + "米";
    }

    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "小时" + ((seconds % 3600) / 60) + "分钟";
        }
        return (seconds / 60) + "分钟";
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyHelper.isConfirm(keyCode) || KeyHelper.isDown(keyCode)) {
            if (route != null && currentStep < route.steps.size() - 1) {
                showStep(currentStep + 1);
            } else if (route != null) {
                showStep(route.steps.size());
            }
            return true;
        }
        if (KeyHelper.isUp(keyCode)) {
            if (currentStep > 0) {
                showStep(currentStep - 1);
            }
            return true;
        }
        if (KeyHelper.isBack(keyCode)) {
            if (tts != null) tts.stop();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
