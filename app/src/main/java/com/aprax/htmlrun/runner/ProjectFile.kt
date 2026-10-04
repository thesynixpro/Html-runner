package com.aprax.htmlrun.runner

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject

/** HTML, CSS and JS loaded from a single file on the device. */
data class ProjectFile(
    val name: String,
    val html: String,
    val css: String,
    val js: String,
)

/** Reads project files chosen through the system file picker. */
object ProjectFiles {

    private const val MAX_BYTES = 4 * 1024 * 1024

    /** MIME types offered to the picker: project files first, then anything else. */
    val mimeTypes = arrayOf(
        "application/json",
        "text/html",
        "text/css",
        "text/javascript",
        "text/plain",
        "application/octet-stream",
        "*/*",
    )

    fun read(context: Context, uri: Uri): Result<ProjectFile> = runCatching {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("The selected file could not be opened")
        if (bytes.isEmpty()) error("The selected file is empty")
        if (bytes.size > MAX_BYTES) error("The selected file is larger than 4 MB")
        val name = displayName(resolver, uri)
        parse(name, String(bytes, Charsets.UTF_8))
            ?: error("$name is not a supported project file")
    }

    /** Accepts a JSON project file or a plain HTML, CSS or JS source file. */
    fun parse(name: String, content: String): ProjectFile? {
        val trimmed = content.trimStart()
        return if (trimmed.startsWith("{")) fromJson(name, content) else fromSource(name, content)
    }

    private fun fromJson(name: String, content: String): ProjectFile? = runCatching {
        val json = JSONObject(content)
        ProjectFile(
            name = name,
            html = json.optString("html", ""),
            css = json.optString("css", ""),
            js = json.optString("js", ""),
        )
    }.getOrNull()

    private fun fromSource(name: String, content: String): ProjectFile? {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> ProjectFile(name, content, "", "")
            "css" -> ProjectFile(name, "", content, "")
            "js", "mjs" -> ProjectFile(name, "", "", content)
            else -> null
        }
    }

    private fun displayName(resolver: android.content.ContentResolver, uri: Uri): String {
        val queried = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
        return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "project"
    }
}
