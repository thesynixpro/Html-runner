package com.aprax.htmlrun.data

import android.content.Context

val FONT_SIZE_RANGE = 10..24
val INDENT_RANGE = 2..8

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class EditorFont { MONOSPACE, SANS, SERIF }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val fontSizeSp: Int = 14,
    val editorFont: EditorFont = EditorFont.MONOSPACE,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val indentSize: Int = 2,
    val autosave: Boolean = true,
    val autoReloadPreview: Boolean = true,
    val javaScriptEnabled: Boolean = true,
) {
    val fontSizeRange: IntRange get() = FONT_SIZE_RANGE
    val indentRange: IntRange get() = INDENT_RANGE
}

class SettingsStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("html_runner_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = readEnum(KEY_THEME, defaults.themeMode),
            fontSizeSp = preferences.getInt(KEY_FONT_SIZE, defaults.fontSizeSp),
            editorFont = readEnum(KEY_FONT, defaults.editorFont),
            showLineNumbers = preferences.getBoolean(KEY_LINE_NUMBERS, defaults.showLineNumbers),
            wordWrap = preferences.getBoolean(KEY_WORD_WRAP, defaults.wordWrap),
            indentSize = preferences.getInt(KEY_INDENT, defaults.indentSize),
            autosave = preferences.getBoolean(KEY_AUTOSAVE, defaults.autosave),
            autoReloadPreview = preferences.getBoolean(KEY_AUTO_RELOAD, defaults.autoReloadPreview),
            javaScriptEnabled = preferences.getBoolean(KEY_JAVASCRIPT, defaults.javaScriptEnabled),
        )
    }

    fun save(settings: AppSettings) {
        preferences.edit()
            .putString(KEY_THEME, settings.themeMode.name)
            .putInt(KEY_FONT_SIZE, settings.fontSizeSp)
            .putString(KEY_FONT, settings.editorFont.name)
            .putBoolean(KEY_LINE_NUMBERS, settings.showLineNumbers)
            .putBoolean(KEY_WORD_WRAP, settings.wordWrap)
            .putInt(KEY_INDENT, settings.indentSize)
            .putBoolean(KEY_AUTOSAVE, settings.autosave)
            .putBoolean(KEY_AUTO_RELOAD, settings.autoReloadPreview)
            .putBoolean(KEY_JAVASCRIPT, settings.javaScriptEnabled)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T {
        val name = preferences.getString(key, null) ?: return fallback
        return enumValues<T>().firstOrNull { it.name == name } ?: fallback
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_FONT = "font"
        const val KEY_LINE_NUMBERS = "line_numbers"
        const val KEY_WORD_WRAP = "word_wrap"
        const val KEY_INDENT = "indent"
        const val KEY_AUTOSAVE = "autosave"
        const val KEY_AUTO_RELOAD = "auto_reload"
        const val KEY_JAVASCRIPT = "javascript"
    }
}
