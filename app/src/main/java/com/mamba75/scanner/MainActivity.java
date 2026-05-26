package com.mamba75.scanner;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge dark chrome
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#050c14"));
            getWindow().setNavigationBarColor(Color.parseColor("#050c14"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Dark background = light icons off (white icons)
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        setupWebView();
        webView.loadUrl("file:///android_asset/index.html");

        // Start background service immediately — no permissions needed
        startScannerService();

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
        }
    }

    private void setupWebView() {
        WebSettings ws = webView.getSettings();

        // Core
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);

        // File access for local assets
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);

        // Network
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Viewport — fills screen properly, no forced zoom
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);

        // Disable zoom controls (scanner is designed for mobile already)
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);

        // Smooth scrolling
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);

        // Keep JS running when app goes to background
        webView.setKeepScreenOn(false); // let screen turn off, but JS keeps running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.getSettings().setOffscreenPreRaster(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // handle all urls inside webview
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(Color.parseColor("#050c14"));
    }

    private void startScannerService() {
        Intent intent = new Intent(this, ScannerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        // Resume JS execution when app comes back to foreground
        if (webView != null) webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // DO NOT pause WebView timers — this keeps JS/WebSocket alive in background
        // webView.pauseTimers(); <-- intentionally NOT called
        if (webView != null) webView.onPause();
    }

    @Override
    public void onBackPressed() {
        // Back button goes to home, keeps scanner running — does NOT close app
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Only destroy WebView if app is actually finishing, not just backgrounded
        if (isFinishing() && webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
