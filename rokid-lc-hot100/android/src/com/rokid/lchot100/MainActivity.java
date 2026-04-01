package com.rokid.lchot100;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * LC Hot100 - Rokid AR 眼镜客户端
 *
 * 架构说明：
 * - 使用 Android WebView 加载内嵌 HTML（res/raw/index.html）
 * - 触控板按键事件 → dispatchKeyEvent → evaluateJavascript → HTML 中的 onKey()
 * - SharedPreferences 通过 JavascriptInterface 实现 JS 持久化存储（标记已会）
 * - 完全离线，数据内嵌在 HTML 中
 *
 * 屏幕方向说明：
 * - Rokid 物理屏 480×640（竖屏面板），光学系统自动将竖屏旋转投射为横屏
 * - 不设置 screenOrientation，使用系统默认 portrait（rotation=0）
 * - HTML 按 480 宽 × 640 高设计，在眼镜里看到的是横屏效果
 *
 * 触控板键码：
 * - 向前滑 → KEYCODE_DPAD_RIGHT (22)
 * - 向后滑 → KEYCODE_DPAD_LEFT (21)
 * - 单击   → KEYCODE_DPAD_CENTER (23)
 * - 上滑   → KEYCODE_DPAD_UP (19)
 *
 * 开发者：bcefghj (bcefghj@163.com)
 */
public class MainActivity extends Activity {
    private WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建 WebView
        wv = new WebView(this);
        wv.setBackgroundColor(Color.BLACK);

        // WebView 设置
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setTextZoom(100);  // 禁止系统字体缩放干扰布局
        wv.setWebViewClient(new WebViewClient());

        // 注册 Native Bridge（JS ↔ Java 通信，用于持久化"标记已会"）
        wv.addJavascriptInterface(new Bridge(), "NativeBridge");

        // 从 res/raw/index.html 读取并加载
        // 使用 file:///android_res/raw/ 作为 base URL，保证同源策略
        try {
            InputStream is = getResources().openRawResource(
                getResources().getIdentifier("index", "raw", getPackageName()));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            wv.loadDataWithBaseURL(
                "file:///android_res/raw/",
                sb.toString(),
                "text/html",
                "UTF-8",
                null
            );
        } catch (Exception e) {
            wv.loadData(
                "<h1 style='color:green;background:black'>Load Error: " + e.getMessage() + "</h1>",
                "text/html", "UTF-8"
            );
        }

        setContentView(wv);
    }

    /**
     * 拦截触控板按键，转发给 JavaScript
     * 使用 dispatchKeyEvent 而非 onKeyDown，可在系统处理前拦截
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int kc = event.getKeyCode();
            if (kc == 21) { wv.evaluateJavascript("onKey('left')", null); return true; }   // 向后滑
            if (kc == 22) { wv.evaluateJavascript("onKey('right')", null); return true; }  // 向前滑
            if (kc == 23) { wv.evaluateJavascript("onKey('center')", null); return true; } // 单击
            if (kc == 19) { wv.evaluateJavascript("onKey('up')", null); return true; }     // 上滑（标记已会）
            if (kc == 20) { wv.evaluateJavascript("onKey('down')", null); return true; }   // 下滑
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Native Bridge：让 JavaScript 调用 Android SharedPreferences
     * 用于持久化"标记已会"的题目集合
     */
    class Bridge {
        @JavascriptInterface
        public void save(String key, String val) {
            getSharedPreferences("lc_prefs", 0).edit().putString(key, val).apply();
        }

        @JavascriptInterface
        public String load(String key) {
            return getSharedPreferences("lc_prefs", 0).getString(key, "");
        }
    }
}
