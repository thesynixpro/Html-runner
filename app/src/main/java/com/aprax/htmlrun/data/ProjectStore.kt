package com.aprax.htmlrun.data

import android.content.Context
import com.aprax.htmlrun.runner.FileTab

class ProjectStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("html_runner_project", Context.MODE_PRIVATE)

    var html: String
        get() = preferences.getString(KEY_HTML, "") ?: ""
        set(value) = preferences.edit().putString(KEY_HTML, value).apply()

    var css: String
        get() = preferences.getString(KEY_CSS, "") ?: ""
        set(value) = preferences.edit().putString(KEY_CSS, value).apply()

    var js: String
        get() = preferences.getString(KEY_JS, "") ?: ""
        set(value) = preferences.edit().putString(KEY_JS, value).apply()

    var tab: FileTab
        get() = runCatching { FileTab.valueOf(preferences.getString(KEY_TAB, null) ?: FileTab.HTML.name) }
            .getOrDefault(FileTab.HTML)
        set(value) = preferences.edit().putString(KEY_TAB, value.name).apply()

    var autoRun: Boolean
        get() = preferences.getBoolean(KEY_AUTO_RUN, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_RUN, value).apply()

    fun save(html: String, css: String, js: String, tab: FileTab, autoRun: Boolean) {
        preferences.edit()
            .putString(KEY_HTML, html)
            .putString(KEY_CSS, css)
            .putString(KEY_JS, js)
            .putString(KEY_TAB, tab.name)
            .putBoolean(KEY_AUTO_RUN, autoRun)
            .apply()
    }

    private companion object {
        const val KEY_HTML = "html"
        const val KEY_CSS = "css"
        const val KEY_JS = "js"
        const val KEY_TAB = "tab"
        const val KEY_AUTO_RUN = "auto_run"
    }
}