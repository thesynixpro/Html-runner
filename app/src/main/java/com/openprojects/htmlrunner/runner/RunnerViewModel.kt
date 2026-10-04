package com.openprojects.htmlrunner.runner

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import com.openprojects.htmlrunner.data.ProjectStore
import com.openprojects.htmlrunner.editor.Language

data class ConsoleEntry(val level: String, val text: String)

class RunnerViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ProjectStore(application)
    private val mainHandler = Handler(Looper.getMainLooper())

    var html: String by mutableStateOf(store.html)
        private set
    var css: String by mutableStateOf(store.css)
        private set
    var js: String by mutableStateOf(store.js)
        private set

    var activeTab: FileTab by mutableStateOf(store.tab)
        private set

    var autoRun: Boolean by mutableStateOf(store.autoRun)
        private set

    var consoleEntries: List<ConsoleEntry> by mutableStateOf(emptyList())
        private set

    var previewDocument: String by mutableStateOf(HtmlBuilder.build(store.html, store.css, store.js))
        private set

    var errorCount: Int by mutableStateOf(0)
        private set

    var editorValues: Map<FileTab, TextFieldValue> by mutableStateOf(
        mapOf(
            FileTab.HTML to TextFieldValue(store.html),
            FileTab.CSS to TextFieldValue(store.css),
            FileTab.JS to TextFieldValue(store.js),
        )
    )
        private set

    fun languageOf(tab: FileTab): Language = when (tab) {
        FileTab.HTML -> Language.HTML
        FileTab.CSS -> Language.CSS
        FileTab.JS -> Language.JS
    }

    fun textOf(tab: FileTab): String = when (tab) {
        FileTab.HTML -> html
        FileTab.CSS -> css
        FileTab.JS -> js
    }

    fun selectTab(tab: FileTab) {
        activeTab = tab
        persist()
    }

    fun toggleAutoRun() {
        autoRun = !autoRun
        persist()
        if (autoRun) run()
    }

    fun onEditorValueChange(value: TextFieldValue) {
        val tab = activeTab
        editorValues = editorValues + (tab to value)
        when (tab) {
            FileTab.HTML -> html = value.text
            FileTab.CSS -> css = value.text
            FileTab.JS -> js = value.text
        }
    }

    fun run() {
        consoleEntries = emptyList()
        errorCount = 0
        previewDocument = HtmlBuilder.build(html, css, js)
        persist()
    }

    fun clearConsole() {
        consoleEntries = emptyList()
        errorCount = 0
    }

    /** Called from the WebView thread, so the snapshot state update is moved to the main thread. */
    fun appendFromBridge(level: String, text: String) {
        mainHandler.post { appendConsoleEntry(level, text) }
    }

    fun appendConsoleEntry(level: String, text: String) {
        val entry = ConsoleEntry(level, text)
        val updated = consoleEntries + entry
        consoleEntries = if (updated.size > MAX_CONSOLE_ENTRIES) updated.takeLast(MAX_CONSOLE_ENTRIES) else updated
        if (level == "error") errorCount++
    }

    fun resetToTemplate() {
        html = Templates.html
        css = Templates.css
        js = Templates.js
        editorValues = mapOf(
            FileTab.HTML to TextFieldValue(Templates.html),
            FileTab.CSS to TextFieldValue(Templates.css),
            FileTab.JS to TextFieldValue(Templates.js),
        )
        run()
    }

    fun persist() {
        store.save(html, css, js, activeTab, autoRun)
    }

    fun clearEditorTab() {
        val tab = activeTab
        editorValues = editorValues + (tab to TextFieldValue("", TextRange.Zero))
        when (tab) {
            FileTab.HTML -> html = ""
            FileTab.CSS -> css = ""
            FileTab.JS -> js = ""
        }
    }

    private companion object {
        const val MAX_CONSOLE_ENTRIES = 300
    }
}