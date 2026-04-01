#!/bin/bash
# Rokid Glasses APK 构建脚本
# 用法: ./build_apk.sh <项目目录> <包名> <应用名> <html文件>
# 例如: ./build_apk.sh lc-hot100 com.rokid.lchot100 "LC Hot100" index.html

set -e

PROJECT_DIR="$(cd "$1" && pwd)"
PACKAGE="$2"
APP_NAME="$3"
HTML_FILE="$4"

if [ -z "$PROJECT_DIR" ] || [ -z "$PACKAGE" ] || [ -z "$APP_NAME" ] || [ -z "$HTML_FILE" ]; then
    echo "用法: $0 <项目目录> <包名> <应用名> <html文件>"
    exit 1
fi

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
BT=$ANDROID_HOME/build-tools/33.0.2
PLATFORM=$ANDROID_HOME/platforms/android-33/android.jar

BUILD="$PROJECT_DIR/build"
rm -rf "$BUILD"
mkdir -p "$BUILD"/{src,obj,bin,res/raw}

PACKAGE_DIR=$(echo "$PACKAGE" | tr '.' '/')
mkdir -p "$BUILD/src/$PACKAGE_DIR"

cp "$PROJECT_DIR/$HTML_FILE" "$BUILD/res/raw/index.html"

cat > "$BUILD/AndroidManifest.xml" << MANIFEST
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$PACKAGE" android:versionCode="1" android:versionName="1.0">
    <uses-sdk android:minSdkVersion="28" android:targetSdkVersion="33" />
    <application android:label="$APP_NAME" android:usesCleartextTraffic="true"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
        <activity android:name="$PACKAGE.MainActivity" android:exported="true"
            android:configChanges="orientation|screenSize|keyboard|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
MANIFEST

cat > "$BUILD/src/$PACKAGE_DIR/MainActivity.java" << 'JAVA'
package PACKAGE_PLACEHOLDER;

import android.app.Activity;
import android.content.SharedPreferences;
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

public class MainActivity extends Activity {
    private WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wv = new WebView(this);
        wv.setBackgroundColor(Color.BLACK);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setTextZoom(100);
        wv.setWebViewClient(new WebViewClient());
        wv.addJavascriptInterface(new Bridge(), "NativeBridge");
        try {
            InputStream is = getResources().openRawResource(
                getResources().getIdentifier("index", "raw", getPackageName()));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            wv.loadDataWithBaseURL("file:///android_res/raw/", sb.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            wv.loadData("<h1 style='color:green;background:black'>Load Error: " + e.getMessage() + "</h1>", "text/html", "UTF-8");
        }
        setContentView(wv);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int kc = event.getKeyCode();
            if (kc == 21) { wv.evaluateJavascript("onKey('left')", null); return true; }
            if (kc == 22) { wv.evaluateJavascript("onKey('right')", null); return true; }
            if (kc == 23) { wv.evaluateJavascript("onKey('center')", null); return true; }
            if (kc == 19) { wv.evaluateJavascript("onKey('up')", null); return true; }
            if (kc == 20) { wv.evaluateJavascript("onKey('down')", null); return true; }
        }
        return super.dispatchKeyEvent(event);
    }

    class Bridge {
        @JavascriptInterface
        public void save(String key, String val) {
            getSharedPreferences("app", 0).edit().putString(key, val).apply();
        }
        @JavascriptInterface
        public String load(String key) {
            return getSharedPreferences("app", 0).getString(key, "");
        }
    }
}
JAVA

sed -i '' "s/PACKAGE_PLACEHOLDER/$PACKAGE/g" "$BUILD/src/$PACKAGE_DIR/MainActivity.java"

echo "编译资源..."
$BT/aapt2 compile --dir "$BUILD/res" -o "$BUILD/compiled.zip" 2>/dev/null || true
$BT/aapt2 link -o "$BUILD/bin/app.unsigned.apk" --manifest "$BUILD/AndroidManifest.xml" \
    -I "$PLATFORM" --auto-add-overlay -R "$BUILD/compiled.zip" 2>/dev/null || \
$BT/aapt2 link -o "$BUILD/bin/app.unsigned.apk" --manifest "$BUILD/AndroidManifest.xml" \
    -I "$PLATFORM" --auto-add-overlay 2>/dev/null

echo "编译Java..."
javac -source 1.8 -target 1.8 -classpath "$PLATFORM" -d "$BUILD/obj" \
    "$BUILD/src/$PACKAGE_DIR/MainActivity.java" 2>&1 | grep -v "警告\|注:" || true

echo "生成DEX..."
$BT/d8 --lib "$PLATFORM" --output "$BUILD/bin/" $(find "$BUILD/obj" -name "*.class") 2>/dev/null

echo "打包APK..."
cd "$BUILD/bin"
cp app.unsigned.apk app.apk
zip -j app.apk classes.dex 2>/dev/null

# 签名
KEYSTORE="$HOME/.rokid-debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" -alias debug -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass android -keypass android -dname "CN=bcefghj,O=RokidDev,C=CN" 2>/dev/null
fi

$BT/zipalign -f 4 app.apk app.aligned.apk
OUT_NAME=$(echo "$PACKAGE" | tr '.' '-')
$BT/apksigner sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
    --out "$PROJECT_DIR/${OUT_NAME}.apk" app.aligned.apk

echo "✅ APK 已生成: $PROJECT_DIR/${OUT_NAME}.apk"
ls -lh "$PROJECT_DIR/${OUT_NAME}.apk"
