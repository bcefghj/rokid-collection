package com.rokidsmartlife.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView resultList;
    private ProgressBar progressBar;
    private TextView emptyText;
    private PoiListAdapter adapter;
    private AmapApiService apiService;
    private double latitude, longitude;
    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;
    private int currentFocusPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        PrefsManager prefs = new PrefsManager(this);
        apiService = new AmapApiService(prefs.getApiKey());

        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        searchInput = findViewById(R.id.search_input);
        resultList = findViewById(R.id.search_results);
        progressBar = findViewById(R.id.progress_bar);
        emptyText = findViewById(R.id.empty_text);

        adapter = new PoiListAdapter(poi -> {
            Intent intent = new Intent(this, PoiDetailActivity.class);
            intent.putExtra("poi", poi);
            intent.putExtra("user_lat", latitude);
            intent.putExtra("user_lng", longitude);
            startActivity(intent);
        });

        resultList.setLayoutManager(new LinearLayoutManager(this));
        resultList.setAdapter(adapter);
        resultList.setDescendantFocusability(RecyclerView.FOCUS_AFTER_DESCENDANTS);

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch(searchInput.getText().toString().trim());
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchRunnable = () -> doSearch(query);
                    searchHandler.postDelayed(searchRunnable, 800);
                }
            }
        });

        searchInput.requestFocus();
    }

    private void doSearch(String keyword) {
        if (keyword.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        apiService.searchByKeyword(keyword, null, longitude, latitude, 1,
                new AmapApiService.ApiCallback<List<PoiResult>>() {
                    @Override
                    public void onSuccess(List<PoiResult> data) {
                        progressBar.setVisibility(View.GONE);
                        if (data.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("未找到 \"" + keyword + "\"");
                        } else {
                            adapter.setItems(data);
                            focusResultItem(0);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText(error);
                        Toast.makeText(SearchActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void focusResultItem(int pos) {
        if (pos < 0 || pos >= adapter.getItemCount()) return;
        currentFocusPos = pos;
        resultList.scrollToPosition(pos);
        resultList.post(() -> {
            RecyclerView.ViewHolder vh = resultList.findViewHolderForAdapterPosition(pos);
            if (vh != null) vh.itemView.requestFocus();
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish();
                return true;
            }

            if (keyCode == RokidKeyHelper.KEYCODE_DASHBOARD) {
                return true;
            }

            if (!searchInput.hasFocus() && adapter.getItemCount() > 0) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_FORWARD) {
                    focusResultItem(Math.min(currentFocusPos + 1, adapter.getItemCount() - 1));
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == RokidKeyHelper.KEYCODE_SPRITE_SWIPE_BACK) {
                    if (currentFocusPos > 0) {
                        focusResultItem(currentFocusPos - 1);
                    } else {
                        searchInput.requestFocus();
                    }
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
