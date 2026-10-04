# Keep the JavaScript bridge used by the preview WebView.
-keepclassmembers class com.aprax.htmlrun.runner.JsBridge {
    public *;
}
-keepattributes JavascriptInterface
