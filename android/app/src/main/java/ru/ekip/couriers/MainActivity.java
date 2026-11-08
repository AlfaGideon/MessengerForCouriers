package ru.ekip.couriers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO_REQUEST = 4102;
    private WebView webView;
    private ValueCallback<Uri[]> pendingFilePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFFFFD21F);
        getWindow().setNavigationBarColor(0xFF252629);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF2F0E7);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"localhost".equals(uri.getHost())) return super.shouldInterceptRequest(view, request);
                String path = uri.getPath();
                if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
                String assetPath = "public" + path;
                try {
                    String mime = URLConnection.guessContentTypeFromName(assetPath);
                    if (assetPath.endsWith(".js")) mime = "text/javascript";
                    else if (assetPath.endsWith(".css")) mime = "text/css";
                    else if (assetPath.endsWith(".webmanifest")) mime = "application/manifest+json";
                    else if (assetPath.endsWith(".svg")) mime = "image/svg+xml";
                    else if (mime == null) mime = "application/octet-stream";
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Access-Control-Allow-Origin", "*");
                    headers.put("Cache-Control", "no-cache");
                    return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, getAssets().open(assetPath));
                } catch (IOException ignored) {
                    return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", Collections.emptyMap(), null);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams params) {
                if (pendingFilePicker != null) pendingFilePicker.onReceiveValue(null);
                pendingFilePicker = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try {
                    startActivityForResult(intent, PICK_VIDEO_REQUEST);
                    return true;
                } catch (Exception error) {
                    pendingFilePicker = null;
                    return false;
                }
            }
        });

        setContentView(webView);
        webView.loadUrl("https://localhost/");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEO_REQUEST || pendingFilePicker == null) return;
        Uri selected = resultCode == RESULT_OK && data != null ? data.getData() : null;
        if (selected != null) {
            try {
                getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
        }
        pendingFilePicker.onReceiveValue(selected == null ? null : new Uri[]{selected});
        pendingFilePicker = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (pendingFilePicker != null) pendingFilePicker.onReceiveValue(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
