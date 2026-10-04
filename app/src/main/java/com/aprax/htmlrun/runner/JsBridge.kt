package com.aprax.htmlrun.runner

import android.webkit.JavascriptInterface

/**
 * Bridge exposed to the preview document as "AndroidRunner".
 * The JavaScript part of the bridge formats values and calls post(level, text).
 */
class JsBridge(private val onMessage: (level: String, text: String) -> Unit) {

    @JavascriptInterface
    fun post(level: String, text: String) {
        onMessage(level, text)
    }
}