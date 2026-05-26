package com.mamba75.scanner;

import android.app.Activity;
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

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#050c14"));
            getWindow().setNavigationBarColor(Color.parseColor("#050c14"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        setupWebView();
        webView.loadUrl("file:///android_asset/index.html");

        startScannerService();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
        }
    }

    private void setupWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(false);
        ws.setBuiltInZoomControls(false);
        ws.setDisplayZoomControls(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.setOffscreenPreRaster(true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
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
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Do NOT pause the WebView — this would freeze JavaScript and stop
        // the scanner, ticks and signals when the app goes to background.
        // The ScannerService WakeLock keeps the WebSocket alive instead.
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
