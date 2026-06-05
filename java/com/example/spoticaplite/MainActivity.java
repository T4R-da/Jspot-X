package com.example.spoticaplite;

import android.annotation.SuppressLint;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.activity.OnBackPressedCallback;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static MainActivity instance;

    private WebView webView;
    private TextView lyricsOverlay;
    private String lastInjectedUrl = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        // ── Modern Root Layout ────────────────────────────────────
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);

        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(webView, webParams);

        // Enhanced Lyrics Overlay
        lyricsOverlay = new TextView(this);
        lyricsOverlay.setTextColor(Color.WHITE);
        lyricsOverlay.setBackgroundColor(0xCC121212);
        lyricsOverlay.setPadding(40, 40, 40, 40);
        lyricsOverlay.setTextSize(16f);
        lyricsOverlay.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        lyricsOverlay.setGravity(Gravity.CENTER);
        lyricsOverlay.setVisibility(View.GONE);
        
        FrameLayout.LayoutParams lyricsParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        lyricsParams.setMargins(0, 50, 0, 0);
        lyricsOverlay.setLayoutParams(lyricsParams);

        root.addView(lyricsOverlay);

        setContentView(root);

        setupWebView();
        setupBridge();

        WebView.setWebContentsDebuggingEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Start Media3 Service
        Intent serviceIntent = new Intent(this, MediaService.class);
        startService(serviceIntent);

        webView.loadUrl("https://open.spotify.com");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        WebSettings s = webView.getSettings();

        // ── Maximum Speed Optimizations ────────────────────────────
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setOffscreenPreRaster(true);
        
        // Disable unnecessary features for speed
        s.setGeolocationEnabled(false);
        s.setAllowFileAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setSaveFormData(false);
        
        // ── Media & Display ──────────────────────────────────────
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);
        s.setDomStorageEnabled(true);

        // This UA is carefully crafted to:
        // 1. Trigger the Tablet/Desktop-like logic (No Shuffle/Previous Lock)
        // Using a Chromebook User-Agent: 
        // 1. Seen as a "Laptop/Desktop" by Spotify -> No forced shuffle, Previous button unlocked.
        // 2. Based on Linux/Chrome -> Maximum compatibility, bypasses "Unsupported Browser" screen.
        // 3. We use CSS to force the mobile-friendly layout inside the app.
        s.setUserAgentString(
            "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        );

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && !Objects.equals(url, lastInjectedUrl)) {
                    lastInjectedUrl = url;
                    injectJS(view);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Keep navigation within Spotify
                if (url.contains("spotify.com")) {
                    return false;
                }
                // Open external links in browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    startActivity(intent);
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // Temporary removal of adblocker to test "Unavailable Browser" fix
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Silently handle errors for stability
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // If the WebView renderer crashes, reload it to recover automatically
                String currentUrl = webView.getUrl();
                if (currentUrl != null) {
                    webView.loadUrl(currentUrl);
                } else {
                    webView.loadUrl("https://open.spotify.com");
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                for (String resource : request.getResources()) {
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                        request.grant(new String[]{resource});
                        return;
                    }
                }
            }
        });
    }

    private void setupBridge() {
        webView.addJavascriptInterface(new SpotifyBridge(), "Android");
    }

    private class SpotifyBridge {
        @JavascriptInterface
        public void update(String title, String artist, String lyrics, boolean isPlaying, String artUrl) {
            runOnUiThread(() -> {
                // Update system notification via Media3 Service
                if (MediaService.instance != null) {
                    MediaService.instance.updateMetadata(title, artist, isPlaying, artUrl);
                }
                
                if (lyrics != null && !lyrics.isEmpty() && !Objects.equals(lyrics, "Lyrics")) {
                    lyricsOverlay.setText(lyrics);
                    lyricsOverlay.setVisibility(View.VISIBLE);
                } else {
                    lyricsOverlay.setVisibility(View.GONE);
                }
            });
        }
    }

    private void injectJS(WebView v) {
        String js =
            "(function() {" +
            "  if (window.__spoticapInjected) return;" +
            "  window.__spoticapInjected = true;" +
            
            // Comprehensive browser masking to bypass Spotify's "Unsupported Browser" checks
            "  Object.defineProperty(navigator, 'webdriver', { get: () => undefined });" +
            "  Object.defineProperty(navigator, 'platform', { get: () => 'Linux armv8l' });" +
            "  if (!navigator.languages || navigator.languages.length === 0) {" +
            "    Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });" +
            "  }" +
            
            // Masking userAgentData to look like a real mobile browser
            "  if (navigator.userAgentData) {" +
            "    Object.defineProperty(navigator.userAgentData, 'mobile', { get: () => true });" +
            "    Object.defineProperty(navigator.userAgentData, 'platform', { get: () => 'Android' });" +
            "  }" +

            "  var style = document.createElement('style');" +
            "  style.innerHTML = 'body { background: #000 !important; } " +
            "    [data-testid=\"ad-visible-container\"], [aria-label=\"Upgrade to Premium\"], " +
            "    .view-port-section-ads, .main-view-container-ads, " +
            "    [data-testid=\"upsell-banner\"], #fl-v-bottom-ads, " +
            "    [aria-label=\"Premium\"], a[href*=\"/premium/\"], " +
            "    nav li:has(a[href*=\"premium\"]), nav li:has(a[href*=\"upgrade\"]), " +
            "    div:has(> a[href*=\"premium\"]), " +
            "    [data-testid=\"side-navigation-bar\"] { display: none !important; } " +
            "    [data-testid=\"now-playing-bar\"], .Root__now-playing-bar { display: flex !important; visibility: visible !important; } " +
            "    [data-testid=\"footer-bar\"], .Root__nav-bar { display: flex !important; visibility: visible !important; } " +
            "    .Root__main-view { padding-bottom: 90px !important; }';" +
            "  document.head.appendChild(style);" +

            // Optimization: Use state-diffing for high responsiveness with low CPU
            "  var lastData = {};" +
            "  function update() {" +
            "    var titleEl  = document.querySelector('[data-testid=\"context-item-info-title\"] a') || document.querySelector('[data-testid=\"context-item-info-title\"]');" +
            "    var artistEl = document.querySelector('[data-testid=\"context-item-info-artist\"] a') || document.querySelector('[data-testid=\"context-item-info-artist\"]');" +
            "    var lyricsEl = document.querySelector('[data-testid=\"lyrics-content\"]');" +
            "    var artEl = document.querySelector('[data-testid=\"cover-art-image\"]') || document.querySelector('.cover-art img') || document.querySelector('[data-testid=\"now-playing-widget\"] img');" +
            "    var playBtn = document.querySelector('[data-testid=\"control-button-playpause\"]') || document.querySelector('[data-testid=\"play-pause-button\"]');" +
            "    " +
            "    var isPlaying = playBtn ? (playBtn.getAttribute('aria-label') === 'Pause' || playBtn.getAttribute('data-play-button-tile') === 'true' || (playBtn.innerHTML && playBtn.innerHTML.toLowerCase().includes('pause'))) : false;" +
            "    var t = titleEl ? titleEl.innerText : null;" +
            "    var a = artistEl ? artistEl.innerText : null;" +
            "    var l = lyricsEl ? lyricsEl.innerText : null;" +
            "    var art = artEl ? artEl.src : null;" +
            "    " +
            "    if (t !== lastData.t || a !== lastData.a || isPlaying !== lastData.p || art !== lastData.art) {" +
            "      lastData = {t:t, a:a, p:isPlaying, art:art};" +
            "      if (t && a) { Android.update(t, a, l, isPlaying, art); }" +
            "    }" +
            "  }" +
            "  " +
            "  setInterval(update, 500);" +
            "  update();" +
            "})();";
        v.evaluateJavascript(js, null);
    }

    public void sendCommand(String cmd) {
        String selector = switch (cmd) {
            case "playpause" ->
                    "[data-testid=\"control-button-playpause\"], [data-testid=\"play-pause-button\"], .player-controls__buttons button[aria-label=\"Pause\"], .player-controls__buttons button[aria-label=\"Play\"]";
            case "next" -> "[data-testid=\"control-button-skip-forward\"], [data-testid=\"skip-forward-button\"]";
            case "prev" -> "[data-testid=\"control-button-skip-back\"], [data-testid=\"skip-back-button\"]";
            case "repeat" -> "[data-testid=\"control-button-repeat\"]";
            default -> "";
        };
        if (!selector.isEmpty()) {
            webView.evaluateJavascript("(function() { " +
                "var el = document.querySelector('" + selector + "'); " +
                "if (el) el.click(); " +
                "})();", null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep WebView running in background for continuous playback
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
