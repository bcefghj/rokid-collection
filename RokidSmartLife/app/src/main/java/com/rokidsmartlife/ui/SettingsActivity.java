package com.rokidsmartlife.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rokidsmartlife.R;
import com.rokidsmartlife.utils.PrefsManager;
import com.rokidsmartlife.utils.RokidKeyHelper;

public class SettingsActivity extends AppCompatActivity {

    private PrefsManager prefsManager;
    private EditText apiKeyInput;
    private EditText cityInput;
    private TextView radiusText;
    private int[] radiusOptions = {500, 1000, 2000, 3000, 5000};
    private int currentRadiusIndex = 1;

    private View[] focusableViews;
    private int focusIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefsManager = new PrefsManager(this);

        apiKeyInput = findViewById(R.id.input_api_key);
        cityInput = findViewById(R.id.input_default_city);
        radiusText = findViewById(R.id.radius_value);
        View saveBtn = findViewById(R.id.btn_save);
        View radiusDecrease = findViewById(R.id.btn_radius_decrease);
        View radiusIncrease = findViewById(R.id.btn_radius_increase);

        String currentKey = prefsManager.getApiKey();
        if (currentKey != null && !currentKey.isEmpty()) {
            apiKeyInput.setText(currentKey);
        }

        String currentCity = prefsManager.getDefaultCity();
        if (currentCity != null && !currentCity.isEmpty()) {
            cityInput.setText(currentCity);
        }

        int currentRadius = prefsManager.getSearchRadius();
        for (int i = 0; i < radiusOptions.length; i++) {
            if (radiusOptions[i] == currentRadius) {
                currentRadiusIndex = i;
                break;
            }
        }
        updateRadiusDisplay();

        radiusDecrease.setOnClickListener(v -> {
            if (currentRadiusIndex > 0) {
                currentRadiusIndex--;
                updateRadiusDisplay();
            }
        });

        radiusIncrease.setOnClickListener(v -> {
            if (currentRadiusIndex < radiusOptions.length - 1) {
                currentRadiusIndex++;
                updateRadiusDisplay();
            }
        });

        saveBtn.setOnClickListener(v -> {
            String key = apiKeyInput.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show();
                return;
            }
            prefsManager.setApiKey(key);
            prefsManager.setDefaultCity(cityInput.getText().toString().trim());
            prefsManager.setSearchRadius(radiusOptions[currentRadiusIndex]);
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
            finish();
        });

        setupFocusEffect(saveBtn);
        setupFocusEffect(radiusDecrease);
        setupFocusEffect(radiusIncrease);

        focusableViews = new View[]{apiKeyInput, cityInput, radiusDecrease, radiusIncrease, saveBtn};
        apiKeyInput.requestFocus();
    }

    private void updateRadiusDisplay() {
        int r = radiusOptions[currentRadiusIndex];
        if (r >= 1000) {
            radiusText.setText(String.format("%.1fkm", r / 1000.0));
        } else {
            radiusText.setText(r + "m");
        }
    }

    private void setupFocusEffect(View view) {
        view.setFocusable(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(8);
            bg.setColor(hasFocus ? 0xFF1A73E8 : 0xFF333333);
            v.setBackground(bg);
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

            boolean editFocused = apiKeyInput.hasFocus() || cityInput.hasFocus();
            if (editFocused && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                    && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
                return super.dispatchKeyEvent(event);
            }

            // 前滑/下 = 下一个焦点元素
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_FORWARD) {
                if (focusIndex < focusableViews.length - 1) {
                    focusIndex++;
                    focusableViews[focusIndex].requestFocus();
                }
                return true;
            }

            // 后滑/上 = 上一个焦点元素
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
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
