package com.aprax.htmlrun.runner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/** HTML, CSS and JS read from the project folder chosen by the user. */
data class ProjectFolder(
    val name: String,
    val html: String,
    val css: String,
    val js: String,
)

/** Reads a project folder through the system folder picker. */
object ProjectFolders {

    private const val MAX_BYTES = 4 * 1024 * 1024

    private val HTML_EXTENSIONS = setOf("html", "htm")
    private val CSS_EXTENSIONS = setOf("css")
    private val JS_EXTENSIONS = setOf("js", "mjs")

    fun read(context: Context, treeUri: Uri): Result<ProjectFolder> = runCatching {
        val resolver = context.contentResolver
        val children = listChildren(context, treeUri)
            .filterNot { it.isDirectory }
            .ifEmpty { error("The selected folder has no files") }

        val html = pick(children, HTML_EXTENSIONS)
            ?: error("No HTML file found in the selected folder")
        val folderName = html.name.substringBeforeLast('.', html.name)

        ProjectFolder(
            name = folderName,
            html = readFile(resolver, treeUri, html.documentId, html.name),
            css = pick(children, CSS_EXTENSIONS)?.let { readFile(resolver, treeUri, it.documentId, it.name) } ?: "",
            js = pick(children, JS_EXTENSIONS)?.let { readFile(resolver, treeUri, it.documentId, it.name) } ?: "",
        )
    }

    /** Name and document id of every file and sub folder inside the tree. */
    private fun listChildren(context: Context, treeUri: Uri): List<Child> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        return context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            buildList {
                while (cursor.moveToNext()) {
                    if (idColumn < 0 || nameColumn < 0) continue
                    val name = cursor.getString(nameColumn) ?: continue
                    val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                    add(
                        Child(
                            documentId = cursor.getString(idColumn),
                            name = name,
                            isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        )
                    )
                }
            }
        } ?: error("The selected folder could not be read")
    }

    /** Prefers index.html over any other HTML file. */
    private fun pick(children: List<Child>, extensions: Set<String>): Child? {
        val matches = children.filter { extensionOf(it.name) in extensions }
        if (matches.isEmpty()) return null
        return matches.firstOrNull { it.name.equals("index.html", ignoreCase = true) } ?: matches.first()
    }

    private fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

    private fun readFile(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        documentId: String,
        name: String,
    ): String {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("$name could not be opened")
        if (bytes.isEmpty()) error("$name is empty")
        if (bytes.size > MAX_BYTES) error("$name is larger than 4 MB")
        return String(bytes, Charsets.UTF_8)
    }

    private data class Child(
        val documentId: String,
        val name: String,
        val isDirectory: Boolean,
    )
}
