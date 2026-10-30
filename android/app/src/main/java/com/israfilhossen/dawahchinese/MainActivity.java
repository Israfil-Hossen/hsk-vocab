package com.israfilhossen.dawahchinese;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedDispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Shows the bundled Dawah Chinese pages offline and gives them native speech, reminders and sharing. */
public class MainActivity extends Activity {
    static final String HOST = "appassets.androidplatform.net";
    static final String START = "https://" + HOST + "/www/index.html";
    static final String SITE = "https://israfil-hossen.github.io/hsk-vocab/";

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(0xFFEDF5FD);
        setContentView(web);

        // Draw below the status bar and above the navigation bar (edge-to-edge is the default on new Android).
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(b.left, b.top, b.right, b.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new Client());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { notifyDone(); }
                    @Override public void onError(String id) { notifyDone(); }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
        }

        if (state != null) web.restoreState(state);
        else web.loadUrl(START);
    }

    private void goBack() {
        if (web.canGoBack()) web.goBack();
        else finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        web.destroy();
        super.onDestroy();
    }

    private void notifyDone() {
        runOnUiThread(() -> web.evaluateJavascript("window.__ttsDone&&window.__ttsDone()", null));
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    /** Serves the bundled pages from assets/www on a secure origin, so storage and fetch work as on the website. */
    private class Client extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (!HOST.equals(u.getHost())) return null;
            String path = u.getPath();
            if (path == null || !path.startsWith("/www/")) return null;
            String asset = path.substring(1);
            if (asset.endsWith("/")) asset += "index.html";
            try {
                InputStream in = getAssets().open(asset);
                WebResourceResponse r = new WebResourceResponse(mime(asset), "utf-8", in);
                Map<String, String> h = new HashMap<>();
                h.put("Access-Control-Allow-Origin", "*");
                r.setResponseHeaders(h);
                return r;
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            String url = u.toString();
            if (HOST.equals(u.getHost())) {
                String path = u.getPath() == null ? "" : u.getPath();
                // The PDF and Word files are not bundled: open them from the website.
                if (path.contains("/dawah/books/")) {
                    openExternal(SITE + path.substring("/www/".length()));
                    return true;
                }
                return false;
            }
            openExternal(url);
            return true;
        }
    }

    private static String mime(String p) {
        String f = p.toLowerCase(Locale.ROOT);
        if (f.endsWith(".html")) return "text/html";
        if (f.endsWith(".js")) return "application/javascript";
        if (f.endsWith(".css")) return "text/css";
        if (f.endsWith(".json")) return "application/json";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".svg")) return "image/svg+xml";
        if (f.endsWith(".mp3")) return "audio/mpeg";
        if (f.endsWith(".xml")) return "text/xml";
        return "application/octet-stream";
    }

    /** Methods the pages can call as window.AndroidApp. */
    private class Bridge {
        @JavascriptInterface
        public void speak(String text, float rate) {
            if (tts == null || !ttsReady) { notifyDone(); return; }
            tts.setSpeechRate(Math.max(0.4f, rate / 0.9f));
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u" + System.nanoTime());
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public boolean ttsReady() {
            return ttsReady;
        }

        @JavascriptInterface
        public void openTtsSettings() {
            try {
                startActivity(new Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }

        @JavascriptInterface
        public void setReminder(int hour, int minute) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                }
                ReminderReceiver.save(MainActivity.this, hour, minute);
                ReminderReceiver.schedule(MainActivity.this);
            });
        }

        @JavascriptInterface
        public void cancelReminder() {
            ReminderReceiver.cancel(MainActivity.this);
        }

        @JavascriptInterface
        public void share(String title, String url) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, title);
            i.putExtra(Intent.EXTRA_TEXT, title + "\n" + url);
            startActivity(Intent.createChooser(i, "Share"));
        }

        @JavascriptInterface
        public void openUrl(String url) {
            openExternal(url);
        }

        @JavascriptInterface
        public String version() {
            return BuildConfigVersion.NAME;
        }
    }
}
