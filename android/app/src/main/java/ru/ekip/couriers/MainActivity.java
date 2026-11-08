package ru.ekip.couriers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO_REQUEST = 4102;
    private static final long MAX_ARCHIVE_BYTES = 40L * 1024L * 1024L;
    private static final long MAX_EXTRACTED_BYTES = 100L * 1024L * 1024L;
    private WebView webView;
    private ValueCallback<Uri[]> pendingFilePicker;
    private final ExecutorService otaExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFFFFD21F);
        getWindow().setNavigationBarColor(0xFF252629);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF2F0E7);
        webView.addJavascriptInterface(new OtaBridge(), "EkipNative");
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!"localhost".equals(uri.getHost())) return super.shouldInterceptRequest(view, request);
                String path = uri.getPath();
                if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
                String mime = contentType(path);
                File updatedFile = new File(new File(getFilesDir(), "ota"), path.substring(1));
                try {
                    InputStream stream;
                    if (updatedFile.isFile() && updatedFile.getCanonicalPath().startsWith(new File(getFilesDir(), "ota").getCanonicalPath() + File.separator)) {
                        stream = new BufferedInputStream(new java.io.FileInputStream(updatedFile));
                    } else {
                        stream = getAssets().open("public" + path);
                    }
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Access-Control-Allow-Origin", "*");
                    headers.put("Cache-Control", "no-cache");
                    return new WebResourceResponse(mime, "UTF-8", 200, "OK", headers, stream);
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
                Intent intent;
                if (Build.VERSION.SDK_INT >= 33) {
                    intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                    intent.setType("video/*");
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("video/*");
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    if (Build.VERSION.SDK_INT >= 33) startActivityForResult(intent, PICK_VIDEO_REQUEST);
                    else startActivityForResult(Intent.createChooser(intent, "Выберите видео"), PICK_VIDEO_REQUEST);
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

    private static String contentType(String path) {
        if (path.endsWith(".js")) return "text/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".webmanifest")) return "application/manifest+json";
        if (path.endsWith(".svg")) return "image/svg+xml";
        String guessed = URLConnection.guessContentTypeFromName(path);
        return guessed == null ? "application/octet-stream" : guessed;
    }

    private class OtaBridge {
        @JavascriptInterface
        public int getWebVersion() {
            int installedVersion = getSharedPreferences("ota", MODE_PRIVATE).getInt("web_version", -1);
            if (installedVersion >= 0) return installedVersion;
            try (InputStream input = getAssets().open("public/ota-version.txt")) {
                byte[] bytes = new byte[32];
                int count = input.read(bytes);
                return Integer.parseInt(new String(bytes, 0, count, "UTF-8").trim());
            } catch (Exception ignored) {
                return 2;
            }
        }

        @JavascriptInterface
        public void installWebUpdate(String updateUrl, String expectedSha256, String version) {
            if (updateUrl == null || expectedSha256 == null || version == null || !expectedSha256.matches("(?i)[a-f0-9]{64}") || !version.matches("[0-9]{1,9}")) {
                postOtaResult(false);
                return;
            }
            otaExecutor.execute(() -> {
                boolean success = false;
                File staging = new File(getFilesDir(), "ota-staging");
                try {
                    URL manifestUrl = new URL(updateUrl);
                    if (!"https".equals(manifestUrl.getProtocol()) || !"raw.githubusercontent.com".equals(manifestUrl.getHost()) || !manifestUrl.getPath().equals("/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/release/web-update.zip")) {
                        throw new IOException("Update URL is not trusted");
                    }
                    URL url = new URL(updateUrl + "?t=" + System.currentTimeMillis());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(30000);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("User-Agent", "Ekip-Android-Updater");
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Update download failed");
                    if (connection.getContentLengthLong() > MAX_ARCHIVE_BYTES) throw new IOException("Update is too large");
                    File archive = new File(getCacheDir(), "web-update.zip");
                    try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(archive)) {
                        byte[] buffer = new byte[8192];
                        long total = 0;
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            if (total > MAX_ARCHIVE_BYTES) throw new IOException("Update is too large");
                            output.write(buffer, 0, count);
                        }
                    } finally {
                        connection.disconnect();
                    }
                    if (!expectedSha256.equalsIgnoreCase(sha256(archive))) throw new IOException("Update checksum mismatch");
                    deleteTree(staging);
                    if (!staging.mkdirs()) throw new IOException("Could not prepare update directory");
                    unzipSafely(archive, staging);
                    File index = new File(staging, "index.html");
                    if (!index.isFile()) throw new IOException("Update package is incomplete");
                    File current = new File(getFilesDir(), "ota");
                    File backup = new File(getFilesDir(), "ota-previous");
                    deleteTree(backup);
                    if (current.exists() && !current.renameTo(backup)) throw new IOException("Could not replace current update");
                    if (!staging.renameTo(current)) {
                        if (backup.exists()) backup.renameTo(current);
                        throw new IOException("Could not activate update");
                    }
                    getSharedPreferences("ota", MODE_PRIVATE).edit().putInt("web_version", Integer.parseInt(version)).apply();
                    deleteTree(backup);
                    success = true;
                } catch (Exception ignored) {
                    deleteTree(staging);
                }
                postOtaResult(success);
            });
        }
    }

    private void postOtaResult(boolean success) {
        if (webView == null) return;
        webView.post(() -> {
            if (webView == null) return;
            webView.evaluateJavascript("if(window.__ekipOtaResult)window.__ekipOtaResult(" + success + ")", null);
            if (success) webView.loadUrl("https://localhost/");
        });
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new BufferedInputStream(new java.io.FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    private static void unzipSafely(File archive, File destination) throws IOException {
        String root = destination.getCanonicalPath() + File.separator;
        long extracted = 0;
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = input.getNextEntry()) != null) {
                File outputFile = new File(destination, entry.getName());
                if (!outputFile.getCanonicalPath().startsWith(root)) throw new IOException("Invalid archive path");
                if (entry.isDirectory()) {
                    if (!outputFile.isDirectory() && !outputFile.mkdirs()) throw new IOException("Could not create update directory");
                } else {
                    File parent = outputFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create update directory");
                    try (FileOutputStream output = new FileOutputStream(outputFile)) {
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            extracted += count;
                            if (extracted > MAX_EXTRACTED_BYTES) throw new IOException("Extracted update is too large");
                            output.write(buffer, 0, count);
                        }
                    }
                }
                input.closeEntry();
            }
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
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
        otaExecutor.shutdownNow();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
