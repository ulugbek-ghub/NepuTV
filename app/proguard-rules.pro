# Keep WebView JavaScript interface
-keepclassmembers class com.neputv.app.MainActivity$AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Activity
-keep class com.neputv.app.MainActivity { *; }
