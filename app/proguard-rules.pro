# Keep the JavaScript bridge used by the preview WebView.
-keepclassmembers class com.openprojects.htmlrunner.runner.JsBridge {
    public *;
}
-keepattributes JavascriptInterface
