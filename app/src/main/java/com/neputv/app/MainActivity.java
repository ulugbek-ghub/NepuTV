package com.neputv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final String TARGET_URL = "https://nepu.to/";

    /**
     * Chrome 120 on Android TV user-agent.
     * This makes nepu.to serve the video-player HTML rather than a mobile page.
     */
    private static final String TV_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; SHIELD Android TV Build/SQ3A.220705.002.A1) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.6099.144 Mobile Safari/537.36";

    // ─── Views ────────────────────────────────────────────────────────────────
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorView;

    // ─── JavaScript injected after every page load ─────────────────────────────
    /**
     * This script:
     *  1. Adds visible focus outlines so the D-pad cursor is visible.
     *  2. Makes all interactive elements (anchors, buttons, inputs, posters)
     *     focusable and navigable with keyboard/D-pad.
     *  3. Shows the soft keyboard when an <input> or <textarea> gets focus
     *     (TV on-screen keyboard workaround via Android bridge).
     */
    private static final String FOCUS_INJECTION_JS =
        "(function() {\n" +
        "  // ── 1. Global focus ring ─────────────────────────────────────────\n" +
        "  var style = document.createElement('style');\n" +
        "  style.textContent = [\n" +
        "    '*:focus { outline: 3px solid #E50914 !important; outline-offset: 2px !important; }',\n" +
        "    '*:focus-visible { outline: 3px solid #E50914 !important; }',\n" +
        "    '.tv-focused { box-shadow: 0 0 0 3px #E50914, 0 0 20px rgba(229,9,20,0.5) !important; transform: scale(1.05); transition: transform 0.15s ease; }'\n" +
        "  ].join('\\n');\n" +
        "  document.head.appendChild(style);\n" +
        "\n" +
        "  // ── 2. Make all interactive elements focusable ───────────────────\n" +
        "  var selectors = [\n" +
        "    'a', 'button', 'input', 'textarea', 'select',\n" +
        "    '[role=\"button\"]', '[role=\"link\"]', '[role=\"menuitem\"]',\n" +
        "    '.film-item', '.movie-item', '.item', '.poster',\n" +
        "    '.btn', '.play-btn', '.watch-btn', '.episode-item',\n" +
        "    '[class*=\"card\"]', '[class*=\"poster\"]', '[class*=\"thumb\"]'\n" +
        "  ];\n" +
        "  document.querySelectorAll(selectors.join(',')).forEach(function(el) {\n" +
        "    if (!el.getAttribute('tabindex')) {\n" +
        "      el.setAttribute('tabindex', '0');\n" +
        "    }\n" +
        "  });\n" +
        "\n" +
        "  // ── 3. Keyboard bridge for TV on-screen keyboard ─────────────────\n" +
        "  document.addEventListener('focus', function(e) {\n" +
        "    var tag = e.target.tagName ? e.target.tagName.toLowerCase() : '';\n" +
        "    if (tag === 'input' || tag === 'textarea' || tag === 'select') {\n" +
        "      if (window.AndroidBridge) {\n" +
        "        window.AndroidBridge.showKeyboard();\n" +
        "      }\n" +
        "    }\n" +
        "  }, true);\n" +
        "  document.addEventListener('blur', function(e) {\n" +
        "    var tag = e.target.tagName ? e.target.tagName.toLowerCase() : '';\n" +
        "    if (tag === 'input' || tag === 'textarea') {\n" +
        "      if (window.AndroidBridge) {\n" +
        "        window.AndroidBridge.hideKeyboard();\n" +
        "      }\n" +
        "    }\n" +
        "  }, true);\n" +
        "\n" +
        "  // ── 4. MutationObserver: re-inject on dynamic content load ────────\n" +
        "  var observer = new MutationObserver(function(mutations) {\n" +
        "    mutations.forEach(function(m) {\n" +
        "      m.addedNodes.forEach(function(node) {\n" +
        "        if (node.nodeType === 1) {\n" +
        "          node.querySelectorAll && node.querySelectorAll(selectors.join(',')).forEach(function(el) {\n" +
        "            if (!el.getAttribute('tabindex')) el.setAttribute('tabindex', '0');\n" +
        "          });\n" +
        "          if (!node.getAttribute('tabindex') &&\n" +
        "              selectors.some(function(s) { return node.matches && node.matches(s); })) {\n" +
        "            node.setAttribute('tabindex', '0');\n" +
        "          }\n" +
        "        }\n" +
        "      });\n" +
        "    });\n" +
        "  });\n" +
        "  observer.observe(document.body, { childList: true, subtree: true });\n" +
        "\n" +
        "  // ── 5. Auto-focus first focusable element ────────────────────────\n" +
        "  setTimeout(function() {\n" +
        "    var first = document.querySelector(selectors.join(','));\n" +
        "    if (first) first.focus();\n" +
        "  }, 500);\n" +
        "\n" +
        "  console.log('[NepuTV] Focus injection complete');\n" +
        "})();";

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on and go full-screen (TV best practice)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        buildLayout();
        setupWebView();

        if (isNetworkAvailable()) {
            webView.loadUrl(TARGET_URL);
        } else {
            showError("No internet connection.\nPlease check your network and restart the app.");
        }
    }

    // ─── Layout (programmatic – no XML needed) ────────────────────────────────
    private void buildLayout() {
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(0xFF0D0D0D);

        // WebView
        webView = new WebView(this);
        RelativeLayout.LayoutParams webParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(webParams);
        root.addView(webView);

        // Progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(0xFFE50914));
        RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 6);
        pbParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        progressBar.setLayoutParams(pbParams);
        root.addView(progressBar);

        // Error view
        errorView = new TextView(this);
        errorView.setTextColor(0xFFCCCCCC);
        errorView.setTextSize(20);
        errorView.setGravity(android.view.Gravity.CENTER);
        errorView.setVisibility(View.GONE);
        RelativeLayout.LayoutParams errParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        errorView.setLayoutParams(errParams);
        root.addView(errorView);

        setContentView(root);
    }

    // ─── WebView Setup ────────────────────────────────────────────────────────
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        WebSettings s = webView.getSettings();

        // Performance & compatibility
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        // Hardware acceleration for 4K/1080p video
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        s.setAllowContentAccess(true);

        // TV user-agent → makes nepu.to serve TV-compatible player
        s.setUserAgentString(TV_USER_AGENT);

        // Android bridge for keyboard control
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");

        // Chrome client (handles video permissions, progress)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Grant DRM / video permissions automatically
                request.grant(request.getResources());
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                // Suppress console noise in production
                return true;
            }
        });

        // WebView client (handles navigation, JS injection)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                // Inject focus + keyboard JS on every page load
                view.evaluateJavascript(FOCUS_INJECTION_JS, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep all navigation inside the WebView
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                showError("Failed to load page.\n\nError: " + description +
                          "\n\nPress Back and try again.");
            }
        });
    }

    // ─── Remote / D-pad key handling ─────────────────────────────────────────
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {

            // ── Back button → go back in WebView history ──────────────────
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                return super.onKeyDown(keyCode, event);

            // ── Center/OK → simulate mouse click on focused element ───────
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                webView.evaluateJavascript(
                    "(function(){" +
                    "  var el = document.activeElement;" +
                    "  if(el && el !== document.body){" +
                    "    el.click();" +
                    "  } else {" +
                    "    var first = document.querySelector('a,button,[tabindex]');" +
                    "    if(first){ first.focus(); first.click(); }" +
                    "  }" +
                    "})();", null);
                return true;

            // ── D-pad navigation → native Tab/Shift-Tab key events ────────
            case KeyEvent.KEYCODE_DPAD_UP:
                webView.evaluateJavascript(
                    "document.activeElement && document.activeElement.dispatchEvent(" +
                    "new KeyboardEvent('keydown',{bubbles:true,key:'ArrowUp',keyCode:38}));", null);
                return super.onKeyDown(keyCode, event); // also let WebView handle

            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.evaluateJavascript(
                    "document.activeElement && document.activeElement.dispatchEvent(" +
                    "new KeyboardEvent('keydown',{bubbles:true,key:'ArrowDown',keyCode:40}));", null);
                return super.onKeyDown(keyCode, event);

            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.evaluateJavascript(
                    "document.activeElement && document.activeElement.dispatchEvent(" +
                    "new KeyboardEvent('keydown',{bubbles:true,key:'ArrowLeft',keyCode:37}));", null);
                return super.onKeyDown(keyCode, event);

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.evaluateJavascript(
                    "document.activeElement && document.activeElement.dispatchEvent(" +
                    "new KeyboardEvent('keydown',{bubbles:true,key:'ArrowRight',keyCode:39}));", null);
                return super.onKeyDown(keyCode, event);

            // ── Media keys → forward to page ──────────────────────────────
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                webView.evaluateJavascript(
                    "(function(){" +
                    "  var v = document.querySelector('video');" +
                    "  if(v){ v.paused ? v.play() : v.pause(); }" +
                    "})();", null);
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    // ─── JavaScript → Android Bridge ─────────────────────────────────────────
    public static class AndroidBridge {
        private final Activity activity;

        public AndroidBridge(Activity activity) {
            this.activity = activity;
        }

        /**
         * Called by JS when an <input>/<textarea> gains focus.
         * Shows the system soft keyboard so the TV on-screen keyboard appears.
         */
        @JavascriptInterface
        public void showKeyboard() {
            activity.runOnUiThread(() -> {
                View view = activity.getCurrentFocus();
                if (view != null) {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(view,
                                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

        @JavascriptInterface
        public void hideKeyboard() {
            activity.runOnUiThread(() -> {
                View view = activity.getCurrentFocus();
                if (view != null) {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                            activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            });
        }
    }
}
